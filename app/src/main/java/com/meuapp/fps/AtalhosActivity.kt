package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AtalhosActivity : Activity() {

    private var conteudo: LinearLayout? = null
    private val tf by lazy { Atalhos.typeface(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Atalhos.comEscala(newBase))
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun prefs() = getSharedPreferences("cfg", Context.MODE_PRIVATE)

    private fun existe(p: String): Boolean {
        return Atalhos.instalado(this, p) || Atalhos.sites.containsKey(p)
    }

    private fun rotulo(p: String): String {
        if (Atalhos.instalado(this, p)) return Atalhos.nomeDe(this, p)
        val s = Atalhos.sites[p]
        return if (s != null) s.first + " (site)" else p
    }

    private fun padrao(): List<String> {
        val l = ArrayList<String>()
        val o = Atalhos.pacoteOpera(this)
        if (o != null) l.add(o)
        l.add("com.android.chrome")
        l.add("com.google.android.youtube")
        l.add("com.zhiliaoapp.musically")
        return l.filter { existe(it) }
    }

    private fun naBarra(): List<String> {
        val s = prefs().getStringSet("f11", null)
        val l = if (s != null && s.isNotEmpty()) s.toList() else padrao()
        return l.filter { existe(it) }.sortedBy { rotulo(it).lowercase() }
    }

    private fun salvar(l: List<String>) {
        val s = HashSet<String>(l)
        if (s.isEmpty()) s.add("-")
        prefs().edit().putStringSet("f11", s).apply()
    }

    private fun cabecalho(t: String): TextView {
        val v = TextView(this)
        v.text = t
        v.setTextColor(Atalhos.acento(this))
        v.textSize = 13f
        v.setPadding(dp(4), dp(16), 0, dp(8))
        return v
    }

    private fun linha(nome: String, simbolo: String, cor: Int, acao: () -> Unit): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        val g = GradientDrawable()
        g.setColor(Atalhos.cartaoTema(this))
        g.cornerRadius = dp(8).toFloat()
        l.background = g
        l.setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(5)
        l.layoutParams = lp

        val n = TextView(this)
        n.text = nome
        n.setTextColor(Color.WHITE)
        n.textSize = 15f
        n.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val s = TextView(this)
        s.text = simbolo
        s.setTextColor(cor)
        s.textSize = 20f

        l.addView(n)
        l.addView(s)
        l.setOnClickListener { acao() }
        return l
    }

    private fun render() {
        val c = conteudo ?: return
        c.removeAllViews()
        val atuais = naBarra()

        c.addView(cabecalho("Na barra agora (toque para tirar)"))
        if (atuais.isEmpty()) {
            val v = TextView(this)
            v.text = "Nenhum app na barra"
            v.setTextColor(Atalhos.suaveTema(this))
            v.setPadding(dp(8), dp(4), 0, dp(4))
            c.addView(v)
        }
        for (p in atuais) {
            c.addView(linha(rotulo(p), "×", Color.parseColor("#FF6B6B")) {
                salvar(atuais - p)
                render()
            })
        }

        c.addView(cabecalho("Adicionar à barra"))
        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        val instalados = packageManager.queryIntentActivities(principal, 0)
            .map { Pair(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
        val sites = Atalhos.sites.keys
            .filter { !Atalhos.instalado(this, it) }
            .map { Pair(rotulo(it), it) }
        val outros = (instalados + sites)
            .filter { it.second != packageName && !atuais.contains(it.second) }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        for (par in outros) {
            c.addView(linha(par.first, "+", Atalhos.acento(this)) {
                salvar(atuais + par.second)
                render()
            })
        }

        c.addView(cabecalho("Padrão"))
        c.addView(linha("Restaurar padrão", "↺", Color.WHITE) {
            prefs().edit().remove("f11").apply()
            render()
        })
        Atalhos.aplicarFonte(c, tf)
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

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.setTextColor(Color.WHITE)
        voltar.textSize = 26f
        voltar.setPadding(dp(4), 0, dp(16), 0)
        voltar.setOnClickListener { finish() }

        val titulo = TextView(this)
        titulo.text = "Barra do F11"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 20f

        topo.addView(voltar)
        topo.addView(titulo)
        col.addView(topo)

        val lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        conteudo = lista
        col.addView(lista)

        setContentView(scroll)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }
}
