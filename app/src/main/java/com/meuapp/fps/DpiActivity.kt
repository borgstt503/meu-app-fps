package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class DpiActivity : Activity() {

    private var disp: TextView? = null
    private var res: TextView? = null
    private var campo: EditText? = null
    private var mouses: List<Pair<String, String>> = emptyList()
    private var sel = 0
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

    private fun cartao(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = arred(Atalhos.cartaoTema(this), 10)
        c.setPadding(dp(16), dp(12), dp(16), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(8)
        c.layoutParams = lp
        return c
    }

    private fun botao(texto: String, principal: Boolean, acao: () -> Unit): TextView {
        val b = TextView(this)
        b.text = texto
        b.textSize = 14f
        b.gravity = Gravity.CENTER
        b.setTextColor(if (principal) Color.BLACK else Atalhos.claroTema(this))
        b.background = arred(if (principal) Atalhos.acento(this) else Atalhos.cartaoTema(this), 10)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        )
        lp.bottomMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { acao() }
        return b
    }

    private fun mostrarDisp() {
        val d = disp ?: return
        if (mouses.isEmpty()) {
            d.text = if (Atalhos.shizukuOk()) {
                "Nenhum mouse detectado. Conecte o mouse e toque em Procurar mouse."
            } else {
                "Ligue o Shizuku e autorize o app"
            }
            return
        }
        sel = sel.coerceIn(0, mouses.size - 1)
        d.text = "Mouse: " + mouses[sel].first + "\n" + mouses[sel].second +
            (if (mouses.size > 1) "\n(toque aqui para trocar de mouse)" else "")
    }

    private fun detectar() {
        val d = disp
        if (!Atalhos.shizukuOk()) {
            d?.text = "Ligue o Shizuku e autorize o app"
            return
        }
        d?.text = "Procurando..."
        Thread {
            val s = Atalhos.rodar("cat /proc/bus/input/devices")
            val lista = ArrayList<Pair<String, String>>()
            for (bloco in s.split("\n\n")) {
                val h = Regex("Handlers=([^\\n]*)").find(bloco)?.groupValues?.get(1) ?: continue
                if (!h.contains("mouse")) continue
                val ev = Regex("event(\\d+)").find(h)?.groupValues?.get(1) ?: continue
                val nome = Regex("Name=\"([^\"]*)\"").find(bloco)?.groupValues?.get(1) ?: "Mouse"
                lista.add(Pair(nome, "/dev/input/event" + ev))
            }
            runOnUiThread {
                mouses = lista
                sel = 0
                mostrarDisp()
            }
        }.start()
    }

    private fun medir() {
        val r = res ?: return
        val cm = campo?.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (cm == null || cm < 1.0 || cm > 100.0) {
            r.text = "Digite a distância em cm (de 1 a 100)"
            return
        }
        if (!Atalhos.shizukuOk()) {
            r.text = "Ligue o Shizuku e autorize o app"
            return
        }
        if (mouses.isEmpty()) {
            r.text = "Toque em Procurar mouse primeiro"
            return
        }
        val dev = mouses[sel].second
        r.text = "Medindo... mova o mouse agora, em linha reta para a direita (8 segundos)"
        Thread {
            val saida = Atalhos.rodar("timeout 8 getevent -l " + dev + " 2>&1")
            var sx = 0L
            var sy = 0L
            var eventos = 0
            for (m in Regex("EV_REL\\s+REL_([XY])\\s+([0-9a-fA-F]{8})").findAll(saida)) {
                val v = java.lang.Long.parseLong(m.groupValues[2], 16).toInt().toLong()
                if (m.groupValues[1] == "X") sx += v else sy += v
                eventos++
            }
            val texto: String
            if (eventos == 0) {
                texto = "Nada foi lido. O GG Mouse Pro ou outro app pode estar segurando o mouse. " +
                    "Feche-o e tente de novo.\n" + saida.take(160)
            } else {
                val pol = cm / 2.54
                val usaX = Math.abs(sx) >= Math.abs(sy)
                val contagem = if (usaX) Math.abs(sx) else Math.abs(sy)
                val dpi = (contagem / pol).toInt()
                val comuns = listOf(400, 800, 1000, 1200, 1600, 2000, 2400, 3200, 4000, 6400, 8000, 12000, 16000)
                val perto = comuns.minByOrNull { Math.abs(it - dpi) } ?: dpi
                texto = "DPI medido: aproximadamente " + dpi +
                    "\nValor mais próximo de um DPI comum: " + perto +
                    "\n\nContagens: X=" + sx + "  Y=" + sy +
                    "\nDistância informada: " + cm + " cm" +
                    "\n\nPara melhorar a precisão, mova exatamente a distância e só em uma direção."
            }
            runOnUiThread { r.text = texto }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fundo = Atalhos.fundoTema(this)
        window.statusBarColor = fundo
        window.navigationBarColor = fundo

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(fundo)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(24))
        scroll.addView(col)

        val topo = LinearLayout(this)
        topo.orientation = LinearLayout.HORIZONTAL
        topo.gravity = Gravity.CENTER_VERTICAL
        topo.setPadding(0, 0, 0, dp(10))

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.setTextColor(Color.WHITE)
        voltar.textSize = 26f
        voltar.setPadding(dp(4), 0, dp(16), 0)
        voltar.setOnClickListener { finish() }

        val titulo = TextView(this)
        titulo.text = "Teste de DPI do mouse"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 20f

        topo.addView(voltar)
        topo.addView(titulo)
        col.addView(topo)

        val como = cartao()
        val ct = TextView(this)
        ct.text = "Como funciona: coloque uma régua ao lado do mouse, escolha a distância (por exemplo " +
            "10 cm), toque em Medir e mova o mouse em linha reta para a direita exatamente essa " +
            "distância, sem voltar. O app lê as contagens brutas do mouse e calcula o DPI."
        ct.setTextColor(Atalhos.suaveTema(this))
        ct.textSize = 12f
        como.addView(ct)
        col.addView(como)

        val cd = cartao()
        val dt = TextView(this)
        dt.setTextColor(Color.WHITE)
        dt.textSize = 13f
        dt.text = "Procurando..."
        dt.setOnClickListener {
            if (mouses.size > 1) {
                sel = (sel + 1) % mouses.size
                mostrarDisp()
            }
        }
        disp = dt
        cd.addView(dt)
        col.addView(cd)

        col.addView(botao("Procurar mouse", false) { detectar() })

        val cc = cartao()
        val rotuloCampo = TextView(this)
        rotuloCampo.text = "Distância que você vai mover (cm)"
        rotuloCampo.setTextColor(Atalhos.suaveTema(this))
        rotuloCampo.textSize = 12f
        val e = EditText(this)
        e.setText("10")
        e.setTextColor(Color.WHITE)
        e.textSize = 16f
        e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        e.setSingleLine(true)
        campo = e
        cc.addView(rotuloCampo)
        cc.addView(e)
        col.addView(cc)

        col.addView(botao("Medir (8 segundos)", true) { medir() })

        val cr = cartao()
        val rt = TextView(this)
        rt.setTextColor(Color.WHITE)
        rt.textSize = 14f
        rt.setLineSpacing(0f, 1.25f)
        rt.text = "O resultado aparece aqui."
        res = rt
        cr.addView(rt)
        col.addView(cr)

        setContentView(scroll)
        Atalhos.aplicarFonte(scroll, tf)
        detectar()
    }
}
