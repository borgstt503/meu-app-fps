package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

private class Ferr(val nome: String, val icone: Drawable, val acao: () -> Unit)

class DesktopActivity : Activity() {

    private val pedirFundo = 7
    private var raiz: FrameLayout? = null
    private var menuIniciar: LinearLayout? = null
    private var menuBandeja: LinearLayout? = null
    private var painelView: View? = null
    private var grade: GridLayout? = null
    private var campoBusca: EditText? = null
    private var bateriaTxt: TextView? = null
    private var todosApps: List<ResolveInfo> = emptyList()
    private var ferrs: List<Ferr> = emptyList()
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

    private fun iconeLetra(letra: String, cor: Int): Drawable {
        val tam = 96
        val bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = cor
        c.drawRoundRect(RectF(0f, 0f, tam.toFloat(), tam.toFloat()), 22f, 22f, p)
        p.color = Color.WHITE
        p.textSize = 52f
        p.textAlign = Paint.Align.CENTER
        val y = tam / 2f - (p.descent() + p.ascent()) / 2f
        c.drawText(letra, tam / 2f, y, p)
        return BitmapDrawable(resources, bmp)
    }

    private fun iconeDe(pacote: String, cor: Int): Drawable {
        return try {
            packageManager.getApplicationIcon(pacote)
        } catch (e: Throwable) {
            iconeLetra(pacote.substringAfterLast('.').take(1).uppercase(), cor)
        }
    }

    private fun iconeOpera(): Drawable {
        val p = Atalhos.pacoteOpera(this)
        return if (p != null) iconeDe(p, Color.parseColor("#E62117"))
        else iconeLetra("O", Color.parseColor("#E62117"))
    }

    private fun abrirApp(pacote: String) {
        val intent = packageManager.getLaunchIntentForPackage(pacote)
        if (intent == null) {
            Toast.makeText(this, "App não encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private fun abrirTela(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    private fun abrirJanelaWeb(titulo: String, url: String) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative 'Exibir sobre outros apps' e volte", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
            return
        }
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("titulo", titulo)
        i.putExtra("url", url)
        startForegroundService(i)
    }

    private fun fecharJanelas() {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", "fechar")
        startForegroundService(i)
    }

    private fun velocidadeCursor(v: Int) {
        if (!Atalhos.shizukuOk()) {
            Toast.makeText(this, "Ligue o Shizuku e autorize o app", Toast.LENGTH_LONG).show()
            return
        }
        Thread {
            Atalhos.rodar("settings put system pointer_speed " + v)
            runOnUiThread {
                Toast.makeText(this, "Velocidade do cursor ajustada", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun protegidos(): Set<String> {
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val s = HashSet<String>(prefs.getStringSet("protegidos", emptySet()) ?: emptySet())
        s.add("com.dts.freefireth")
        s.add("com.dts.freefiremax")
        s.add("moe.shizuku.privileged.api")
        s.add(packageName)
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null) s.add(ime.substringBefore("/"))
        return s
    }

    private fun limparApps() {
        if (!Atalhos.shizukuOk()) {
            Toast.makeText(this, "Ligue o Shizuku e autorize o app", Toast.LENGTH_LONG).show()
            return
        }
        val prot = protegidos()
        val alvos = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }
            .filter { !prot.contains(it) }
        Toast.makeText(this, "Fechando " + alvos.size + " apps...", Toast.LENGTH_SHORT).show()
        Thread {
            for (p in alvos) {
                Atalhos.rodar("am force-stop " + p)
            }
            Atalhos.rodar("am kill-all")
            runOnUiThread {
                Toast.makeText(this, alvos.size.toString() + " apps fechados", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun escolherFundo() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(i, pedirFundo)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pedirFundo && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            if (Atalhos.salvarFundo(this, uri)) {
                aplicarFundo()
            } else {
                Toast.makeText(this, "Erro ao trocar o fundo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fundoPadrao() {
        Atalhos.apagarFundo(this)
        aplicarFundo()
    }

    private fun aplicarFundo() {
        raiz?.background = Atalhos.fundoDrawable(this)
    }

    private fun fecharPainel() {
        painelView?.let { raiz?.removeView(it) }
        painelView = null
    }

    private fun fecharMenus() {
        menuIniciar?.visibility = View.GONE
        menuBandeja?.visibility = View.GONE
        fecharPainel()
    }

    private fun alternarIniciar() {
        val m = menuIniciar ?: return
        val aberto = m.visibility == View.VISIBLE
        fecharMenus()
        if (!aberto) {
            campoBusca?.setText("")
            preencherGrade("")
            m.visibility = View.VISIBLE
        }
    }

    private fun alternarBandeja() {
        val m = menuBandeja ?: return
        val aberto = m.visibility == View.VISIBLE
        fecharMenus()
        if (!aberto) m.visibility = View.VISIBLE
    }

    private fun alternarPainel() {
        val r = raiz ?: return
        val aberto = painelView != null
        fecharMenus()
        if (aberto) return
        val v = Painel.build(this) { fecharPainel() }
        Atalhos.aplicarFonte(v, tf)
        val lp = FrameLayout.LayoutParams(
            minOf(dp(344), resources.displayMetrics.widthPixels - dp(16)),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
        lp.bottomMargin = dp(54)
        lp.rightMargin = dp(8)
        v.layoutParams = lp
        r.addView(v)
        painelView = v
    }

    private fun carregarApps() {
        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        todosApps = packageManager.queryIntentActivities(principal, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    private fun atualizarBateria() {
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val n = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        bateriaTxt?.text = n.toString() + "%"
    }

    private fun itemGrade(
        nome: String,
        icone: Drawable,
        acao: () -> Unit,
        longa: (() -> Unit)?
    ): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL

        val img = ImageView(this)
        img.setImageDrawable(icone)
        img.layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))

        val t = TextView(this)
        t.text = nome
        t.setTextColor(Color.WHITE)
        t.textSize = 10f
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.gravity = Gravity.CENTER
        t.setPadding(0, dp(3), 0, 0)

        item.addView(img)
        item.addView(t)

        val lp = GridLayout.LayoutParams()
        lp.width = dp(64)
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        lp.setMargins(dp(2), dp(6), dp(2), dp(6))
        item.layoutParams = lp

        item.setOnClickListener {
            fecharMenus()
            acao()
        }
        if (longa != null) {
            item.setOnLongClickListener {
                fecharMenus()
                longa.invoke()
                true
            }
        }
        Atalhos.aplicarFonte(item, tf)
        return item
    }

    private fun preencherGrade(filtro: String) {
        val g = grade ?: return
        g.removeAllViews()
        for (f in ferrs) {
            if (filtro.isNotBlank() && !f.nome.contains(filtro, ignoreCase = true)) continue
            g.addView(itemGrade(f.nome, f.icone, f.acao, null))
        }
        for (ri in todosApps) {
            val nome = ri.loadLabel(packageManager).toString()
            if (filtro.isNotBlank() && !nome.contains(filtro, ignoreCase = true)) continue
            val pacote = ri.activityInfo.packageName
            g.addView(
                itemGrade(
                    nome,
                    ri.loadIcon(packageManager),
                    { abrirApp(pacote) },
                    { Atalhos.abrirJanela(this, pacote) }
                )
            )
        }
    }

    private fun montarMenuIniciar(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        m.background = arred(Color.parseColor("#F2262222"), 14)
        m.setPadding(dp(16), dp(14), dp(16), dp(10))
        m.visibility = View.GONE

        val busca = EditText(this)
        busca.hint = "Digite aqui para pesquisar"
        busca.setHintTextColor(Color.parseColor("#B0B0B0"))
        busca.setTextColor(Color.WHITE)
        busca.textSize = 13f
        busca.setSingleLine(true)
        busca.background = arred(Color.parseColor("#3A3636"), 18)
        busca.setPadding(dp(16), 0, dp(16), 0)
        busca.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(36)
        )
        busca.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                preencherGrade(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}

            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        campoBusca = busca

        val rotulo = TextView(this)
        rotulo.text = "Aplicativos"
        rotulo.setTextColor(Color.WHITE)
        rotulo.textSize = 13f
        rotulo.setPadding(dp(4), dp(14), 0, dp(6))

        val g = GridLayout(this)
        g.columnCount = 5
        grade = g

        val scroll = ScrollView(this)
        scroll.addView(g)
        val altura = minOf(dp(210), (resources.displayMetrics.heightPixels * 0.45).toInt())
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, altura
        )

        val rodape = LinearLayout(this)
        rodape.orientation = LinearLayout.HORIZONTAL
        rodape.setPadding(dp(4), dp(10), dp(4), 0)

        val ajustes = TextView(this)
        ajustes.text = "Configurações"
        ajustes.setTextColor(Color.WHITE)
        ajustes.textSize = 13f
        ajustes.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        ajustes.setOnClickListener {
            fecharMenus()
            abrirTela(MainActivity::class.java)
        }

        val sair = TextView(this)
        sair.text = "Fechar"
        sair.setTextColor(Color.parseColor("#B0B0B0"))
        sair.textSize = 13f
        sair.setOnClickListener { fecharMenus() }

        rodape.addView(ajustes)
        rodape.addView(sair)

        m.addView(busca)
        m.addView(rotulo)
        m.addView(scroll)
        m.addView(rodape)
        return m
    }

    private fun cab(texto: String): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.parseColor("#4CC2FF"))
        t.textSize = 10f
        t.setPadding(dp(16), dp(10), dp(16), dp(2))
        return t
    }

    private fun linhaMenu(texto: String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.WHITE)
        t.textSize = 13f
        t.setPadding(dp(16), dp(9), dp(16), dp(9))
        t.setOnClickListener {
            fecharMenus()
            acao()
        }
        return t
    }

    private fun linhaCiclo(texto: () -> String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = texto()
        t.setTextColor(Color.WHITE)
        t.textSize = 13f
        t.setPadding(dp(16), dp(9), dp(16), dp(9))
        t.setOnClickListener {
            acao()
            t.text = texto()
        }
        return t
    }

    private fun montarMenuBandeja(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        m.background = arred(Color.parseColor("#F2262222"), 14)
        m.setPadding(0, dp(4), 0, dp(6))
        m.visibility = View.GONE

        val itens = LinearLayout(this)
        itens.orientation = LinearLayout.VERTICAL

        itens.addView(cab("SISTEMA"))
        itens.addView(linhaMenu("Painel rápido") { alternarPainel() })
        itens.addView(linhaMenu("Fechar apps (liberar RAM)") { limparApps() })
        itens.addView(linhaMenu("Configurações") { abrirTela(MainActivity::class.java) })

        itens.addView(cab("JANELAS"))
        itens.addView(linhaMenu("Fechar janelas") { fecharJanelas() })
        itens.addView(
            linhaMenu("Tamanho: " + Atalhos.nomesTamanho[Atalhos.indiceTamanho(this)]) {
                Atalhos.escolher(this, "Tamanho das janelas", Atalhos.nomesTamanho) { i ->
                    Atalhos.setI(this, "jan_tam", i)
                    recreate()
                }
            }
        )
        itens.addView(
            linhaMenu("Zoom da página: " + Atalhos.nomesZoom[Atalhos.indiceZoom(this)]) {
                Atalhos.escolher(this, "Zoom da página", Atalhos.nomesZoom) { i ->
                    Atalhos.setI(this, "jan_zoom", i)
                    recreate()
                }
            }
        )
        itens.addView(
            linhaCiclo(
                { "Apps reais (com login): " + (if (Atalhos.modoApp(this)) "Sim" else "Não") },
                { Atalhos.setB(this, "modo_app", !Atalhos.modoApp(this)) }
            )
        )
        itens.addView(
            linhaCiclo(
                { "Bloqueio de anúncios: " + (if (Atalhos.bloqueio(this)) "Sim" else "Não") },
                { Atalhos.setB(this, "bloq", !Atalhos.bloqueio(this)) }
            )
        )
        itens.addView(linhaMenu("Barra do F11") { abrirTela(AtalhosActivity::class.java) })

        itens.addView(cab("APARÊNCIA"))
        itens.addView(
            linhaMenu("Escala do app: " + Atalhos.nomesEscala[Atalhos.indiceEscala(this)]) {
                Atalhos.escolher(this, "Escala do app", Atalhos.nomesEscala) { i ->
                    Atalhos.setI(this, "escala", i)
                    recreate()
                }
            }
        )
        itens.addView(
            linhaMenu("Fonte: " + Atalhos.fontes[Atalhos.indiceFonte(this)].second) {
                Atalhos.escolher(this, "Fonte", Atalhos.fontes.map { it.second }) { i ->
                    Atalhos.setI(this, "fonte", i)
                    recreate()
                }
            }
        )
        itens.addView(
            linhaMenu("Cursor: " + Atalhos.cursores[Atalhos.indiceCursor(this)]) {
                Atalhos.escolher(this, "Cursor", Atalhos.cursores) { i ->
                    Atalhos.setI(this, "cursor", i)
                    recreate()
                }
            }
        )
        itens.addView(linhaMenu("Trocar fundo") { escolherFundo() })
        itens.addView(linhaMenu("Fundo padrão") { fundoPadrao() })

        itens.addView(cab("MOUSE"))
        itens.addView(linhaMenu("Velocidade lenta") { velocidadeCursor(-4) })
        itens.addView(linhaMenu("Velocidade normal") { velocidadeCursor(0) })
        itens.addView(linhaMenu("Velocidade rápida") { velocidadeCursor(5) })

        val scroll = ScrollView(this)
        scroll.addView(itens)
        val altura = minOf(dp(340), (resources.displayMetrics.heightPixels * 0.65).toInt())
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, altura
        )
        m.addView(scroll)
        return m
    }

    private fun iconeDesktop(icone: Drawable, nome: String, acao: () -> Unit): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.setPadding(dp(8), dp(6), dp(8), dp(4))

        val img = ImageView(this)
        img.setImageDrawable(icone)
        img.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))

        val txt = TextView(this)
        txt.text = nome
        txt.setTextColor(Color.WHITE)
        txt.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        txt.textSize = 10f
        txt.gravity = Gravity.CENTER
        txt.setPadding(0, dp(3), 0, 0)

        item.addView(img)
        item.addView(txt)
        item.setOnClickListener {
            fecharMenus()
            acao()
        }
        return item
    }

    private fun iconeBarra(icone: Drawable, acao: () -> Unit): ImageView {
        val img = ImageView(this)
        img.setImageDrawable(icone)
        img.setPadding(dp(4), dp(4), dp(4), dp(4))
        val lp = LinearLayout.LayoutParams(dp(34), dp(34))
        lp.setMargins(dp(3), 0, dp(3), 0)
        img.layoutParams = lp
        img.setOnClickListener {
            fecharMenus()
            acao()
        }
        return img
    }

    private fun logoIniciar(): FrameLayout {
        val caixa = FrameLayout(this)
        val lpCaixa = LinearLayout.LayoutParams(dp(34), dp(34))
        lpCaixa.setMargins(dp(3), 0, dp(6), 0)
        caixa.layoutParams = lpCaixa

        val logo = GridLayout(this)
        logo.rowCount = 2
        logo.columnCount = 2
        for (i in 0 until 4) {
            val q = View(this)
            q.setBackgroundColor(Color.parseColor("#4CC2FF"))
            val lp = GridLayout.LayoutParams()
            lp.width = dp(9)
            lp.height = dp(9)
            lp.setMargins(dp(1), dp(1), dp(1), dp(1))
            q.layoutParams = lp
            logo.addView(q)
        }
        caixa.addView(
            logo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        caixa.setOnClickListener { alternarIniciar() }
        return caixa
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        fecharMenus()
    }

    override fun onResume() {
        super.onResume()
        carregarApps()
        preencherGrade(campoBusca?.text?.toString() ?: "")
        atualizarBateria()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#202020")

        ferrs = listOf(
            Ferr("Calculadora", iconeLetra("=", Color.parseColor("#3D7BFF"))) {
                abrirTela(CalculadoraActivity::class.java)
            },
            Ferr("Notas", iconeLetra("N", Color.parseColor("#E0A800"))) {
                abrirTela(NotasActivity::class.java)
            },
            Ferr("Arquivos", iconeLetra("A", Color.parseColor("#2E9E6B"))) {
                abrirTela(ArquivosActivity::class.java)
            }
        )

        val cinza = Color.parseColor("#555555")
        val vermelho = Color.parseColor("#E62117")

        val r = FrameLayout(this)
        raiz = r

        val coluna = LinearLayout(this)
        coluna.orientation = LinearLayout.VERTICAL
        coluna.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        val area = GridLayout(this)
        area.orientation = GridLayout.VERTICAL
        area.rowCount = 4
        area.setPadding(dp(4), dp(8), dp(4), 0)
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        area.setOnClickListener { fecharMenus() }

        area.addView(iconeDesktop(iconeDe(packageName, cinza), "Este PC") {
            abrirTela(MainActivity::class.java)
        })
        area.addView(iconeDesktop(iconeDe("com.android.chrome", cinza), "Chrome") {
            abrirJanelaWeb("Chrome", "https://www.google.com")
        })
        area.addView(iconeDesktop(iconeOpera(), "Opera") {
            abrirJanelaWeb("Opera", "https://www.google.com")
        })
        area.addView(iconeDesktop(iconeDe("com.google.android.youtube", vermelho), "YouTube") {
            abrirJanelaWeb("YouTube", "https://www.youtube.com")
        })
        area.addView(iconeDesktop(iconeDe("com.zhiliaoapp.musically", cinza), "TikTok") {
            abrirJanelaWeb("TikTok", "https://www.tiktok.com")
        })
        area.addView(iconeDesktop(iconeDe("com.dts.freefireth", cinza), "Free Fire") {
            abrirApp("com.dts.freefireth")
        })
        area.addView(iconeDesktop(iconeDe("moe.shizuku.privileged.api", cinza), "Shizuku") {
            abrirApp("moe.shizuku.privileged.api")
        })
        for (f in ferrs) {
            area.addView(iconeDesktop(f.icone, f.nome, f.acao))
        }

        val barra = FrameLayout(this)
        barra.setBackgroundColor(Color.parseColor("#E61C1C1C"))
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )

        val filete = View(this)
        filete.setBackgroundColor(Color.parseColor("#26FFFFFF"))
        barra.addView(
            filete,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(1))
        )

        val centro = LinearLayout(this)
        centro.orientation = LinearLayout.HORIZONTAL
        centro.gravity = Gravity.CENTER_VERTICAL
        centro.setPadding(0, dp(1), 0, 0)

        val pesquisa = TextView(this)
        pesquisa.text = "Pesquisar"
        pesquisa.setTextColor(Color.parseColor("#D0D0D0"))
        pesquisa.textSize = 12f
        pesquisa.gravity = Gravity.CENTER_VERTICAL
        pesquisa.setPadding(dp(16), 0, dp(16), 0)
        pesquisa.background = arred(Color.parseColor("#2EFFFFFF"), 16)
        val lpPesquisa = LinearLayout.LayoutParams(dp(150), dp(30))
        lpPesquisa.rightMargin = dp(10)
        pesquisa.layoutParams = lpPesquisa
        pesquisa.setOnClickListener { alternarIniciar() }

        centro.addView(logoIniciar())
        centro.addView(pesquisa)
        centro.addView(iconeBarra(iconeDe("com.android.chrome", cinza)) {
            abrirJanelaWeb("Chrome", "https://www.google.com")
        })
        centro.addView(iconeBarra(iconeOpera()) {
            abrirJanelaWeb("Opera", "https://www.google.com")
        })
        centro.addView(iconeBarra(iconeDe("com.google.android.youtube", vermelho)) {
            abrirJanelaWeb("YouTube", "https://www.youtube.com")
        })
        centro.addView(iconeBarra(iconeDe("com.zhiliaoapp.musically", cinza)) {
            abrirJanelaWeb("TikTok", "https://www.tiktok.com")
        })
        centro.addView(iconeBarra(iconeDe("com.dts.freefireth", cinza)) {
            abrirApp("com.dts.freefireth")
        })
        centro.addView(iconeBarra(iconeDe("moe.shizuku.privileged.api", cinza)) {
            abrirApp("moe.shizuku.privileged.api")
        })

        barra.addView(
            centro,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_HORIZONTAL
            )
        )

        val direita = LinearLayout(this)
        direita.orientation = LinearLayout.HORIZONTAL
        direita.gravity = Gravity.CENTER_VERTICAL
        direita.setPadding(0, dp(1), dp(10), 0)

        val seta = TextView(this)
        seta.text = "^"
        seta.setTextColor(Color.WHITE)
        seta.textSize = 18f
        seta.gravity = Gravity.CENTER
        seta.setPadding(dp(10), 0, dp(10), 0)
        seta.setOnClickListener { alternarBandeja() }

        val bandeja = TextView(this)
        bandeja.setTextColor(Color.WHITE)
        bandeja.textSize = 11f
        bandeja.maxLines = 1
        bandeja.gravity = Gravity.CENTER
        bandeja.setPadding(dp(10), 0, dp(10), 0)
        bandeja.background = arred(Color.parseColor("#22FFFFFF"), 8)
        val lpBandeja = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)
        )
        lpBandeja.rightMargin = dp(8)
        bandeja.layoutParams = lpBandeja
        bandeja.setOnClickListener { alternarPainel() }
        bateriaTxt = bandeja

        val relogio = LinearLayout(this)
        relogio.orientation = LinearLayout.VERTICAL
        relogio.gravity = Gravity.END
        relogio.setOnClickListener { alternarPainel() }

        val hora = TextClock(this)
        hora.format24Hour = "HH:mm"
        hora.format12Hour = "HH:mm"
        hora.setTextColor(Color.WHITE)
        hora.textSize = 11f
        hora.maxLines = 1
        hora.gravity = Gravity.END

        val data = TextClock(this)
        data.format24Hour = "dd/MM/yyyy"
        data.format12Hour = "dd/MM/yyyy"
        data.setTextColor(Color.parseColor("#D0D0D0"))
        data.textSize = 10f
        data.maxLines = 1
        data.gravity = Gravity.END

        relogio.addView(hora)
        relogio.addView(data)
        direita.addView(seta)
        direita.addView(bandeja)
        direita.addView(relogio)

        barra.addView(
            direita,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
        )

        coluna.addView(area)
        coluna.addView(barra)
        r.addView(coluna)

        val larguraTela = resources.displayMetrics.widthPixels
        val mi = montarMenuIniciar()
        val lpMi = FrameLayout.LayoutParams(
            minOf(dp(400), larguraTela - dp(16)), FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        lpMi.bottomMargin = dp(54)
        mi.layoutParams = lpMi
        menuIniciar = mi
        r.addView(mi)

        val mb = montarMenuBandeja()
        val lpMb = FrameLayout.LayoutParams(
            dp(250), FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
        lpMb.bottomMargin = dp(54)
        lpMb.rightMargin = dp(8)
        mb.layoutParams = lpMb
        menuBandeja = mb
        r.addView(mb)

        setContentView(r)
        r.pointerIcon = Atalhos.pointerIcon(this)
        aplicarFundo()
        Atalhos.aplicarFonte(r, tf)
        carregarApps()
        atualizarBateria()
    }
}
