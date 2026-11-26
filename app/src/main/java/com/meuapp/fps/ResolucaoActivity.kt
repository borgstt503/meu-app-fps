package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ResolucaoActivity : Activity() {

    private val h = Handler(Looper.getMainLooper())
    private var status: TextView? = null
    private var campoL: EditText? = null
    private var campoA: EditText? = null
    private var campoD: EditText? = null
    private var listaView: LinearLayout? = null
    private val tf by lazy { Atalhos.typeface(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Atalhos.comEscala(newBase))
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun arred(cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(raio).toFloat()
        return g
    }

    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

    private fun presets(): MutableList<String> {
        val s = prefs().getString("res_presets", null)
        val base = if (s.isNullOrEmpty()) listOf("1080x1920") else s.split(",")
        val l = base.filter { it.matches(Regex("\\d+x\\d+")) }.toMutableList()
        if (!l.contains("1080x1920")) l.add(0, "1080x1920")
        return l
    }

    private fun salvarPresets(l: List<String>) {
        prefs().edit().putString("res_presets", l.joinToString(",")).apply()
    }

    private fun ok(): Boolean {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return false
        }
        return true
    }

    private fun atualizar() {
        val s = status ?: return
        if (!Atalhos.shizukuOk()) {
            s.text = "Ligue o Shizuku e autorize o app"
            return
        }
        Thread {
            val tam = Atalhos.rodar("wm size").trim().replace("\n", " | ")
            val den = Atalhos.rodar("wm density").trim().replace("\n", " | ")
            runOnUiThread { s.text = tam + "\n" + den }
        }.start()
    }

    private fun aplicar(w: Int, hh: Int, d: Int?, salvar: Boolean) {
        if (!ok()) return
        if (w < 200 || w > 8000 || hh < 200 || hh > 8000) {
            Atalhos.aviso(this, "Use valores entre 200 e 8000")
            return
        }
        Thread {
            val tam = Atalhos.rodar("wm size")
            val den = Atalhos.rodar("wm density")
            val antS = Regex("Override size: (\\d+x\\d+)").find(tam)?.groupValues?.get(1) ?: "reset"
            val antD = Regex("Override density: (\\d+)").find(den)?.groupValues?.get(1) ?: "reset"
            var cmd = "wm size " + w + "x" + hh
            if (d != null && d >= 72 && d <= 800) cmd += "; wm density " + d
            Atalhos.rodar(cmd)
            if (salvar) {
                val l = presets()
                val novo = w.toString() + "x" + hh
                if (!l.contains(novo)) {
                    l.add(novo)
                    salvarPresets(l)
                }
            }
            runOnUiThread {
                confirmar(antS, antD)
                preencherLista()
            }
        }.start()
    }

    private fun confirmar(antS: String, antD: String) {
        val d = AlertDialog.Builder(this)
            .setTitle("Manter esta resolução?")
            .setMessage("Se você não confirmar em 15 segundos, a resolução anterior volta sozinha.")
            .setCancelable(false)
            .setPositiveButton("Manter") { _, _ -> atualizar() }
            .setNegativeButton("Voltar") { _, _ -> reverter(antS, antD) }
            .create()
        d.show()
        h.postDelayed({
            if (d.isShowing) {
                d.dismiss()
                reverter(antS, antD)
            }
        }, 15000)
    }

    private fun reverter(s: String, d: String) {
        Thread {
            Atalhos.rodar("wm size " + s + "; wm density " + d)
            runOnUiThread { atualizar() }
        }.start()
    }

    private fun botao(texto: String, principal: Boolean, acao: () -> Unit): TextView {
        val b = TextView(this)
        b.text = texto
        b.textSize = 14f
        b.gravity = Gravity.CENTER
        b.setTextColor(if (principal) Color.BLACK else Atalhos.claroTema(this))
        b.background = arred(if (principal) Atalhos.acento(this) else Atalhos.cartaoTema(this), 10)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
        )
        lp.topMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { acao() }
        return b
    }

    private fun campo(dica: String): EditText {
        val e = EditText(this)
        e.hint = dica
        e.setHintTextColor(Atalhos.suaveTema(this))
        e.setTextColor(Color.WHITE)
        e.textSize = 14f
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.setSingleLine(true)
        e.background = arred(Atalhos.cartaoTema(this), 10)
        e.setPadding(dp(12), 0, dp(12), 0)
        val lp = LinearLayout.LayoutParams(0, dp(42), 1f)
        lp.setMargins(dp(3), 0, dp(3), 0)
        e.layoutParams = lp
        return e
    }

    private fun preencherLista() {
        val l = listaView ?: return
        l.removeAllViews()
        for (p in presets()) {
            val linha = LinearLayout(this)
            linha.orientation = LinearLayout.HORIZONTAL
            linha.gravity = Gravity.CENTER_VERTICAL
            linha.background = arred(Atalhos.cartaoTema(this), 10)
            linha.setPadding(dp(14), dp(10), dp(14), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(6)
            linha.layoutParams = lp

            val t = TextView(this)
            t.text = p.replace("x", " x ")
            t.setTextColor(Color.WHITE)
            t.textSize = 15f
            t.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            linha.addView(t)

            if (p != "1080x1920") {
                val x = TextView(this)
                x.text = "×"
                x.setTextColor(Color.parseColor("#FF6B6B"))
                x.textSize = 20f
                x.setPadding(dp(12), 0, 0, 0)
                x.setOnClickListener {
                    val novo = presets()
                    novo.remove(p)
                    salvarPresets(novo)
                    preencherLista()
                }
                linha.addView(x)
            }

            linha.setOnClickListener {
                val partes = p.split("x")
                aplicar(partes[0].toInt(), partes[1].toInt(), null, false)
            }
            l.addView(linha)
        }
        Atalhos.aplicarFonte(l, tf)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        val fundo = arred(Atalhos.fundoTema(this), 14)
        fundo.setStroke(dp(1), Atalhos.bordaTema(this))
        caixa.background = fundo
        caixa.setPadding(dp(16), dp(14), dp(16), dp(14))

        val titulo = TextView(this)
        titulo.text = "Resolução da tela"
        titulo.setTextColor(Atalhos.acento(this))
        titulo.textSize = 16f

        val st = TextView(this)
        st.setTextColor(Atalhos.claroTema(this))
        st.textSize = 12f
        st.setPadding(0, dp(6), 0, dp(6))
        status = st

        val rotuloPres = TextView(this)
        rotuloPres.text = "Salvas (toque para aplicar)"
        rotuloPres.setTextColor(Atalhos.suaveTema(this))
        rotuloPres.textSize = 12f
        rotuloPres.setPadding(0, dp(6), 0, 0)

        val lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        listaView = lista

        val rotuloOutra = TextView(this)
        rotuloOutra.text = "Outra resolução (largura, altura e DPI opcional)"
        rotuloOutra.setTextColor(Atalhos.suaveTema(this))
        rotuloOutra.textSize = 12f
        rotuloOutra.setPadding(0, dp(12), 0, dp(6))

        val linhaCampos = LinearLayout(this)
        linhaCampos.orientation = LinearLayout.HORIZONTAL
        val cl = campo("Largura")
        val ca = campo("Altura")
        val cd = campo("DPI")
        campoL = cl
        campoA = ca
        campoD = cd
        linhaCampos.addView(cl)
        linhaCampos.addView(ca)
        linhaCampos.addView(cd)

        fun lerCampos(salvar: Boolean) {
            val w = cl.text.toString().toIntOrNull()
            val a = ca.text.toString().toIntOrNull()
            if (w == null || a == null) {
                Atalhos.aviso(this, "Digite a largura e a altura")
                return
            }
            aplicar(w, a, cd.text.toString().toIntOrNull(), salvar)
        }

        val conteudo = LinearLayout(this)
        conteudo.orientation = LinearLayout.VERTICAL
        conteudo.addView(titulo)
        conteudo.addView(st)
        conteudo.addView(rotuloPres)
        conteudo.addView(lista)
        conteudo.addView(rotuloOutra)
        conteudo.addView(linhaCampos)
        conteudo.addView(botao("Aplicar", true) { lerCampos(false) })
        conteudo.addView(botao("Aplicar e salvar", false) { lerCampos(true) })
        conteudo.addView(
            botao("Resetar resolução", false) {
                if (ok()) {
                    Thread {
                        Atalhos.rodar("wm size reset; wm density reset")
                        runOnUiThread { atualizar() }
                    }.start()
                }
            }
        )
        conteudo.addView(botao("Fechar", false) { finish() })

        val scroll = ScrollView(this)
        scroll.addView(conteudo)
        caixa.addView(scroll)
        setContentView(caixa)

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(
            minOf(dp(380), resources.displayMetrics.widthPixels - dp(16)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        Atalhos.aplicarFonte(caixa, tf)
        preencherLista()
        atualizar()
    }
}
