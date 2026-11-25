package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class GerenciadorActivity : Activity() {

    private var lista: LinearLayout? = null
    private var resumo: TextView? = null
    private var atuais: List<Pair<String, Int>> = emptyList()
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

    private fun protegidos(): Set<String> {
        val salvos = getSharedPreferences("cfg", MODE_PRIVATE)
            .getStringSet("protegidos", emptySet()) ?: emptySet()
        val s = HashSet<String>(salvos)
        s.add("com.dts.freefireth")
        s.add("com.dts.freefiremax")
        s.add("moe.shizuku.privileged.api")
        s.add(packageName)
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null) s.add(ime.substringBefore("/"))
        val home = Intent(Intent.ACTION_MAIN)
        home.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        val p = res?.activityInfo?.packageName
        if (p != null) s.add(p)
        return s
    }

    private fun carregar() {
        val l = lista ?: return
        val r = resumo ?: return
        if (!Atalhos.shizukuOk()) {
            l.removeAllViews()
            r.text = "Ligue o Shizuku e autorize o app para ver os apps em execução"
            return
        }
        r.text = "Carregando..."
        Thread {
            val saida = Atalhos.rodar("ps -A -o RSS,NAME")
            val mapa = HashMap<String, Int>()
            for (ln in saida.lines()) {
                val p = ln.trim().split(Regex("\\s+"))
                if (p.size < 2) continue
                val rss = p[0].toIntOrNull() ?: continue
                val pkg = p[1].substringBefore(':')
                if (pkg == packageName) continue
                if (packageManager.getLaunchIntentForPackage(pkg) == null) continue
                mapa[pkg] = (mapa[pkg] ?: 0) + rss
            }
            val ord = mapa.entries.sortedByDescending { it.value }.map { Pair(it.key, it.value) }
            runOnUiThread { mostrar(ord) }
        }.start()
    }

    private fun mostrar(ord: List<Pair<String, Int>>) {
        atuais = ord
        val l = lista ?: return
        l.removeAllViews()
        var total = 0
        for (par in ord) total += par.second
        resumo?.text = ord.size.toString() + " apps em execução, " + (total / 1024) + " MB"
        for (par in ord) l.addView(linha(par.first, par.second))
        Atalhos.aplicarFonte(l, tf)
    }

    private fun fecharApp(pkg: String) {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return
        }
        Thread {
            Atalhos.rodar("am force-stop " + pkg)
            Thread.sleep(400)
            runOnUiThread { carregar() }
        }.start()
    }

    private fun fecharTodos() {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return
        }
        val prot = protegidos()
        val alvos = atuais.map { it.first }.filter { !prot.contains(it) }
        Thread {
            for (p in alvos) {
                Atalhos.rodar("am force-stop " + p)
            }
            Atalhos.aviso(this, alvos.size.toString() + " apps fechados")
            Thread.sleep(500)
            runOnUiThread { carregar() }
        }.start()
    }

    private fun linha(pkg: String, rss: Int): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        l.background = arred(Atalhos.cartaoTema(this), 8)
        l.setPadding(dp(12), dp(10), dp(12), dp(10))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(5)
        l.layoutParams = lp

        val img = ImageView(this)
        try {
            img.setImageDrawable(packageManager.getApplicationIcon(pkg))
        } catch (e: Throwable) {
        }
        img.layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), 0, 0, 0)
        col.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        val n = TextView(this)
        n.text = Atalhos.nomeDe(this, pkg)
        n.setTextColor(Color.WHITE)
        n.textSize = 15f

        val m = TextView(this)
        m.text = (rss / 1024).toString() + " MB"
        m.setTextColor(Atalhos.suaveTema(this))
        m.textSize = 12f

        col.addView(n)
        col.addView(m)

        val fechar = TextView(this)
        fechar.text = "Fechar"
        fechar.setTextColor(Color.BLACK)
        fechar.textSize = 13f
        fechar.gravity = Gravity.CENTER
        fechar.background = arred(Atalhos.acento(this), 8)
        fechar.setPadding(dp(14), dp(8), dp(14), dp(8))
        fechar.setOnClickListener { fecharApp(pkg) }

        l.addView(img)
        l.addView(col)
        l.addView(fechar)
        return l
    }

    private fun botao(texto: String, acao: () -> Unit): TextView {
        val b = TextView(this)
        b.text = texto
        b.setTextColor(Atalhos.claroTema(this))
        b.textSize = 13f
        b.gravity = Gravity.CENTER
        b.background = arred(Atalhos.cartaoTema(this), 8)
        b.setPadding(dp(16), dp(10), dp(16), dp(10))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.rightMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { acao() }
        return b
    }

    override fun onResume() {
        super.onResume()
        carregar()
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
        titulo.text = "Gerenciador de tarefas"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 20f

        topo.addView(voltar)
        topo.addView(titulo)
        col.addView(topo)

        val r = TextView(this)
        r.setTextColor(Atalhos.suaveTema(this))
        r.textSize = 13f
        r.setPadding(dp(4), dp(10), 0, dp(10))
        resumo = r
        col.addView(r)

        val botoes = LinearLayout(this)
        botoes.orientation = LinearLayout.HORIZONTAL
        botoes.setPadding(0, 0, 0, dp(10))
        botoes.addView(botao("Atualizar") { carregar() })
        botoes.addView(botao("Fechar todos") { fecharTodos() })
        col.addView(botoes)

        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        lista = l
        col.addView(l)

        setContentView(scroll)
        Atalhos.aplicarFonte(botoes, tf)
    }
}
