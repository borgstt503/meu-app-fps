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
import android.graphics.drawable.ColorDrawable
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
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

private class Ferr(val nome: String, val icone: Drawable, val acao: () -> Unit)

private class ItemMenu(val texto: String, val acao: () -> Unit)

class DesktopActivity : Activity() {

    private val pedirFundo = 7
    private var raiz: FrameLayout? = null
    private var menuIniciar: LinearLayout? = null
    private var menuBandeja: LinearLayout? = null
    private var painelView: View? = null
    private var grade: GridLayout? = null
    private var campoBusca: EditText? = null
    private var bateriaTxt: TextView? = null
    private var areaView: GridLayout? = null
    private var centroView: LinearLayout? = null
    private var logoView: View? = null
    private var pesquisaView: View? = null
    private var todosApps: List<ResolveInfo> = emptyList()
    private var ferrs: List<Ferr> = emptyList()
    private var ultX = 0
    private var ultY = 0
    private val tf by lazy { Atalhos.typeface(this) }

    private val roxo by lazy { Atalhos.acento(this) }
    private val roxoTexto by lazy { Atalhos.claroTema(this) }
    private val roxoSuave by lazy { Atalhos.suaveTema(this) }
    private val corFundoT by lazy { Atalhos.fundoTema(this) }
    private val corCartaoT by lazy { Atalhos.cartaoTema(this) }
    private val corBordaT by lazy { Atalhos.bordaTema(this) }
    private val vermelho = Color.parseColor("#E62117")
    private val cinza = Color.parseColor("#555555")

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

    private fun ehOpera(p: String): Boolean {
        return p.startsWith("com.opera.")
    }

    private fun iconeDoPacote(p: String): Drawable {
        return try {
            packageManager.getApplicationIcon(p)
        } catch (e: Throwable) {
            val nome = Atalhos.sites[p]?.first ?: (if (ehOpera(p)) "Opera" else p.substringAfterLast('.'))
            val cor = if (p == "com.google.android.youtube" || ehOpera(p)) vermelho else cinza
            iconeLetra(nome.take(1).uppercase(), cor)
        }
    }

    private fun rotuloDe(p: String): String {
        if (p == packageName) return "Este PC"
        val s = Atalhos.sites[p]
        if (s != null) return s.first
        if (ehOpera(p)) return "Opera"
        return if (Atalhos.instalado(this, p)) Atalhos.nomeDe(this, p) else p.substringAfterLast('.')
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

    private fun abrirPacote(p: String) {
        if (p == packageName) {
            abrirTela(MainActivity::class.java)
            return
        }
        val site = Atalhos.sites[p]
        if (site != null) {
            abrirJanelaWeb(site.first, site.second)
            return
        }
        if (ehOpera(p)) {
            abrirJanelaWeb("Opera", "https://www.google.com")
            return
        }
        abrirApp(p)
    }

    private fun abrirEmJanela(p: String) {
        val site = Atalhos.sites[p]
        if (site != null) {
            abrirJanelaWeb(site.first, site.second)
            return
        }
        if (ehOpera(p)) {
            abrirJanelaWeb("Opera", "https://www.google.com")
            return
        }
        Atalhos.abrirJanela(this, p)
    }

    private fun configApp(p: String) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + p))
            )
        } catch (e: Throwable) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(this, "Velocidade do mouse ajustada", Toast.LENGTH_SHORT).show()
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

    private fun escolherTema() {
        Atalhos.escolher(this, "Cor do tema", Atalhos.nomesTema) { i ->
            Atalhos.setI(this, "tema", i)
            recreate()
        }
    }

    private fun padraoDesktop(): List<String> {
        val l = ArrayList<String>()
        l.add(packageName)
        l.add("com.android.chrome")
        l.add(Atalhos.pacoteOpera(this) ?: "com.opera.browser")
        l.add("com.google.android.youtube")
        l.add("com.zhiliaoapp.musically")
        l.add("com.dts.freefireth")
        l.add("moe.shizuku.privileged.api")
        return l
    }

    private fun padraoBarra(): List<String> {
        return padraoDesktop().drop(1)
    }

    private fun lerLista(chave: String, padrao: List<String>): MutableList<String> {
        val s = getSharedPreferences("cfg", MODE_PRIVATE).getString(chave, null)
        if (s == null) return ArrayList(padrao)
        if (s.isEmpty()) return ArrayList()
        return ArrayList(s.split(","))
    }

    private fun salvarLista(chave: String, l: List<String>) {
        getSharedPreferences("cfg", MODE_PRIVATE).edit()
            .putString(chave, l.joinToString(","))
            .apply()
    }

    private fun alternarFixo(
        chave: String,
        padrao: List<String>,
        p: String,
        msgOn: String,
        msgOff: String
    ) {
        val l = lerLista(chave, padrao)
        if (l.contains(p)) {
            l.remove(p)
            Toast.makeText(this, msgOff, Toast.LENGTH_SHORT).show()
        } else {
            l.add(p)
            Toast.makeText(this, msgOn, Toast.LENGTH_SHORT).show()
        }
        salvarLista(chave, l)
        montarFixos()
    }

    private fun montarFixos() {
        preencherArea()
        preencherBarra()
    }

    private fun mostrarMenu(ancora: View, itens: List<ItemMenu>, ax: Int = -1, ay: Int = -1) {
        val largura = dp(250)
        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        val fundo = arred(Atalhos.comAlfa(corFundoT, 0xF2), 10)
        fundo.setStroke(dp(1), corBordaT)
        caixa.background = fundo
        caixa.setPadding(0, dp(6), 0, dp(6))

        val popup = PopupWindow(caixa, largura, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = dp(8).toFloat()

        for (item in itens) {
            val t = TextView(this)
            t.text = item.texto
            t.setTextColor(roxoTexto)
            t.textSize = 13f
            t.setPadding(dp(16), dp(10), dp(16), dp(10))
            t.setOnClickListener {
                popup.dismiss()
                item.acao()
            }
            caixa.addView(t)
        }
        Atalhos.aplicarFonte(caixa, tf)

        caixa.measure(
            View.MeasureSpec.makeMeasureSpec(largura, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val h = caixa.measuredHeight
        val loc = IntArray(2)
        ancora.getLocationOnScreen(loc)
        val tela = resources.displayMetrics
        val porPosicao = ax >= 0 && ay >= 0
        var x = if (porPosicao) ax else loc[0]
        var y = if (porPosicao) ay else loc[1] + ancora.height
        if (x + largura > tela.widthPixels - dp(8)) x = tela.widthPixels - largura - dp(8)
        if (x < dp(8)) x = dp(8)
        if (y + h > tela.heightPixels - dp(8)) {
            y = if (porPosicao) tela.heightPixels - h - dp(8) else loc[1] - h
        }
        if (y < dp(8)) y = dp(8)
        popup.showAtLocation(ancora, Gravity.NO_GRAVITY, x, y)
    }

    private fun mostrarMenuApp(ancora: View, p: String) {
        val itens = ArrayList<ItemMenu>()
        itens.add(ItemMenu("Abrir") {
            fecharMenus()
            abrirPacote(p)
        })
        if (p != packageName) {
            itens.add(ItemMenu("Abrir em janela") {
                fecharMenus()
                abrirEmJanela(p)
            })
        }
        val naBarra = lerLista("barra_lista", padraoBarra()).contains(p)
        itens.add(
            ItemMenu(if (naBarra) "Desafixar da barra de tarefas" else "Fixar na barra de tarefas") {
                fecharMenus()
                alternarFixo(
                    "barra_lista", padraoBarra(), p,
                    "Fixado na barra de tarefas", "Removido da barra de tarefas"
                )
            }
        )
        val naArea = lerLista("desk_lista", padraoDesktop()).contains(p)
        itens.add(
            ItemMenu(if (naArea) "Remover da Área de Trabalho" else "Adicionar à Área de Trabalho") {
                fecharMenus()
                alternarFixo(
                    "desk_lista", padraoDesktop(), p,
                    "Adicionado à Área de Trabalho", "Removido da Área de Trabalho"
                )
            }
        )
        if (p != packageName && Atalhos.instalado(this, p)) {
            itens.add(ItemMenu("Configurações do Aplicativo") {
                fecharMenus()
                configApp(p)
            })
        }
        mostrarMenu(ancora, itens)
    }

    private fun mostrarMenuArea() {
        val a = areaView ?: return
        val itens = listOf(
            ItemMenu("Trocar plano de fundo") { escolherFundo() },
            ItemMenu("Fundo padrão") { fundoPadrao() },
            ItemMenu("Cor do tema: " + Atalhos.nomesTema[Atalhos.indiceTema(this)]) { escolherTema() },
            ItemMenu("Gerenciador de tarefas") { abrirTela(GerenciadorActivity::class.java) },
            ItemMenu("Painel rápido") { alternarPainel() },
            ItemMenu("Configurações") { abrirTela(MainActivity::class.java) },
            ItemMenu("Atualizar") { recreate() }
        )
        mostrarMenu(a, itens, ultX, ultY)
    }

    private fun comMenu(v: View, p: String) {
        v.setOnLongClickListener {
            mostrarMenuApp(v, p)
            true
        }
        v.setOnContextClickListener {
            mostrarMenuApp(v, p)
            true
        }
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
        pacote: String?,
        acao: () -> Unit
    ): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL

        val img = ImageView(this)
        img.setImageDrawable(icone)
        img.layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))

        val t = TextView(this)
        t.text = nome
        t.setTextColor(roxoTexto)
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
        if (pacote != null) comMenu(item, pacote)
        Atalhos.aplicarFonte(item, tf)
        return item
    }

    private fun preencherGrade(filtro: String) {
        val g = grade ?: return
        g.removeAllViews()
        for (f in ferrs) {
            if (filtro.isNotBlank() && !f.nome.contains(filtro, ignoreCase = true)) continue
            g.addView(itemGrade(f.nome, f.icone, null, f.acao))
        }
        for (ri in todosApps) {
            val nome = ri.loadLabel(packageManager).toString()
            if (filtro.isNotBlank() && !nome.contains(filtro, ignoreCase = true)) continue
            val pacote = ri.activityInfo.packageName
            g.addView(itemGrade(nome, ri.loadIcon(packageManager), pacote) { abrirApp(pacote) })
        }
    }

    private fun montarMenuIniciar(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        val fundo = arred(Atalhos.comAlfa(corFundoT, 0xF2), 14)
        fundo.setStroke(dp(1), corBordaT)
        m.background = fundo
        m.setPadding(dp(16), dp(14), dp(16), dp(10))
        m.visibility = View.GONE

        val busca = EditText(this)
        busca.hint = "Digite aqui para pesquisar"
        busca.setHintTextColor(roxoSuave)
        busca.setTextColor(Color.WHITE)
        busca.textSize = 13f
        busca.setSingleLine(true)
        busca.background = arred(corCartaoT, 18)
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
        rotulo.setTextColor(roxo)
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
        ajustes.setTextColor(roxoTexto)
        ajustes.textSize = 13f
        ajustes.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        ajustes.setOnClickListener {
            fecharMenus()
            abrirTela(MainActivity::class.java)
        }

        val sair = TextView(this)
        sair.text = "Fechar"
        sair.setTextColor(roxoSuave)
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

    private fun cartaoSecao(titulo: String, linhas: List<View>): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = arred(corCartaoT, 12)
        c.setPadding(0, dp(4), 0, dp(4))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(10), dp(4), dp(10), dp(6))
        c.layoutParams = lp

        val h = TextView(this)
        h.text = titulo
        h.setTextColor(roxo)
        h.textSize = 10f
        h.setPadding(dp(14), dp(8), dp(14), dp(2))
        c.addView(h)
        for (l in linhas) c.addView(l)
        return c
    }

    private fun linhaCfg(
        texto: String,
        valor: String?,
        fechar: Boolean = true,
        acao: () -> Unit
    ): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        l.setPadding(dp(14), dp(10), dp(14), dp(10))

        val t = TextView(this)
        t.text = texto
        t.setTextColor(roxoTexto)
        t.textSize = 13f
        t.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        l.addView(t)

        if (valor != null) {
            val v = TextView(this)
            v.text = valor
            v.setTextColor(roxoSuave)
            v.textSize = 12f
            l.addView(v)
        }

        val seta = TextView(this)
        seta.text = "  ›"
        seta.setTextColor(roxoSuave)
        seta.textSize = 16f
        l.addView(seta)

        l.setOnClickListener {
            if (fechar) fecharMenus()
            acao()
        }
        return l
    }

    private fun linhaSwitch(texto: String, ler: () -> Boolean, mudar: () -> Unit): LinearLayout {
        val l = linhaCfg(texto, if (ler()) "Sim" else "Não", false) {}
        l.setOnClickListener {
            mudar()
            (l.getChildAt(1) as TextView).text = if (ler()) "Sim" else "Não"
        }
        return l
    }

    private fun montarMenuBandeja(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        val fundo = arred(Atalhos.comAlfa(corFundoT, 0xF2), 14)
        fundo.setStroke(dp(1), corBordaT)
        m.background = fundo
        m.setPadding(0, dp(4), 0, dp(4))
        m.visibility = View.GONE

        val itens = LinearLayout(this)
        itens.orientation = LinearLayout.VERTICAL

        itens.addView(
            cartaoSecao(
                "SISTEMA",
                listOf(
                    linhaCfg("Painel rápido", null) { alternarPainel() },
                    linhaCfg("Gerenciador de tarefas", null) { abrirTela(GerenciadorActivity::class.java) },
                    linhaCfg("Fechar apps (liberar RAM)", null) { limparApps() },
                    linhaCfg("Configurações", null) { abrirTela(MainActivity::class.java) }
                )
            )
        )

        itens.addView(
            cartaoSecao(
                "JANELAS",
                listOf(
                    linhaCfg("Fechar janelas", null) { fecharJanelas() },
                    linhaCfg("Tamanho", Atalhos.nomesTamanho[Atalhos.indiceTamanho(this)]) {
                        Atalhos.escolher(this, "Tamanho das janelas", Atalhos.nomesTamanho) { i ->
                            Atalhos.setI(this, "jan_tam", i)
                            recreate()
                        }
                    },
                    linhaCfg("Zoom da página", Atalhos.nomesZoom[Atalhos.indiceZoom(this)]) {
                        Atalhos.escolher(this, "Zoom da página", Atalhos.nomesZoom) { i ->
                            Atalhos.setI(this, "jan_zoom", i)
                            recreate()
                        }
                    },
                    linhaSwitch(
                        "Apps reais (com login)",
                        { Atalhos.modoApp(this) },
                        { Atalhos.setB(this, "modo_app", !Atalhos.modoApp(this)) }
                    ),
                    linhaSwitch(
                        "Bloqueio de anúncios",
                        { Atalhos.bloqueio(this) },
                        { Atalhos.setB(this, "bloq", !Atalhos.bloqueio(this)) }
                    ),
                    linhaCfg("Barra do F11", null) { abrirTela(AtalhosActivity::class.java) }
                )
            )
        )

        itens.addView(
            cartaoSecao(
                "APARÊNCIA",
                listOf(
                    linhaCfg("Cor do tema", Atalhos.nomesTema[Atalhos.indiceTema(this)]) { escolherTema() },
                    linhaCfg("Escala do app", Atalhos.nomesEscala[Atalhos.indiceEscala(this)]) {
                        Atalhos.escolher(this, "Escala do app", Atalhos.nomesEscala) { i ->
                            Atalhos.setI(this, "escala", i)
                            recreate()
                        }
                    },
                    linhaCfg("Fonte", Atalhos.fontes[Atalhos.indiceFonte(this)].second) {
                        Atalhos.escolher(this, "Fonte", Atalhos.fontes.map { it.second }) { i ->
                            Atalhos.setI(this, "fonte", i)
                            recreate()
                        }
                    },
                    linhaCfg("Cursor", Atalhos.cursores[Atalhos.indiceCursor(this)]) {
                        Atalhos.escolher(this, "Cursor", Atalhos.cursores) { i ->
                            Atalhos.setI(this, "cursor", i)
                            recreate()
                        }
                    },
                    linhaCfg("Trocar fundo", null) { escolherFundo() },
                    linhaCfg("Fundo padrão", null) { fundoPadrao() }
                )
            )
        )

        itens.addView(
            cartaoSecao(
                "MOUSE",
                listOf(
                    linhaCfg("Velocidade do mouse", null) {
                        Atalhos.escolher(this, "Velocidade do mouse", listOf("Lenta", "Normal", "Rápida")) { i ->
                            velocidadeCursor(if (i == 0) -4 else if (i == 1) 0 else 5)
                        }
                    }
                )
            )
        )

        val scroll = ScrollView(this)
        scroll.addView(itens)
        val altura = minOf(dp(380), (resources.displayMetrics.heightPixels * 0.72).toInt())
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
        txt.maxLines = 1
        txt.ellipsize = TextUtils.TruncateAt.END
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

    private fun viewDesktop(p: String): LinearLayout {
        val v = iconeDesktop(iconeDoPacote(p), rotuloDe(p)) { abrirPacote(p) }
        comMenu(v, p)
        return v
    }

    private fun preencherArea() {
        val a = areaView ?: return
        a.removeAllViews()
        for (p in lerLista("desk_lista", padraoDesktop())) {
            a.addView(viewDesktop(p))
        }
        for (f in ferrs) {
            a.addView(iconeDesktop(f.icone, f.nome, f.acao))
        }
        Atalhos.aplicarFonte(a, tf)
    }

    private fun iconeBarra(p: String): ImageView {
        val img = ImageView(this)
        img.setImageDrawable(iconeDoPacote(p))
        img.setPadding(dp(4), dp(4), dp(4), dp(4))
        val lp = LinearLayout.LayoutParams(dp(34), dp(34))
        lp.setMargins(dp(3), 0, dp(3), 0)
        img.layoutParams = lp
        img.setOnClickListener {
            fecharMenus()
            abrirPacote(p)
        }
        comMenu(img, p)
        return img
    }

    private fun preencherBarra() {
        val c = centroView ?: return
        c.removeAllViews()
        logoView?.let { c.addView(it) }
        pesquisaView?.let { c.addView(it) }
        for (p in lerLista("barra_lista", padraoBarra())) {
            c.addView(iconeBarra(p))
        }
        Atalhos.aplicarFonte(c, tf)
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
            q.setBackgroundColor(roxo)
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
        window.navigationBarColor = corFundoT

        ferrs = listOf(
            Ferr("Calculadora", iconeLetra("=", roxo)) {
                abrirTela(CalculadoraActivity::class.java)
            },
            Ferr("Notas", iconeLetra("N", Color.parseColor("#E0A800"))) {
                abrirTela(NotasActivity::class.java)
            },
            Ferr("Arquivos", iconeLetra("A", Color.parseColor("#2E9E6B"))) {
                abrirTela(ArquivosActivity::class.java)
            },
            Ferr("Tarefas", iconeLetra("T", Color.parseColor("#D9534F"))) {
                abrirTela(GerenciadorActivity::class.java)
            }
        )

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
        area.setOnLongClickListener {
            mostrarMenuArea()
            true
        }
        area.setOnContextClickListener {
            mostrarMenuArea()
            true
        }
        area.setOnTouchListener { _, e ->
            ultX = e.rawX.toInt()
            ultY = e.rawY.toInt()
            false
        }
        area.setOnHoverListener { _, e ->
            ultX = e.rawX.toInt()
            ultY = e.rawY.toInt()
            false
        }
        areaView = area

        val barra = FrameLayout(this)
        barra.setBackgroundColor(Atalhos.comAlfa(corFundoT, 0xE6))
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )

        val filete = View(this)
        filete.setBackgroundColor(Atalhos.comAlfa(corBordaT, 0x66))
        barra.addView(
            filete,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(1))
        )

        val centro = LinearLayout(this)
        centro.orientation = LinearLayout.HORIZONTAL
        centro.gravity = Gravity.CENTER_VERTICAL
        centro.setPadding(0, dp(1), 0, 0)
        centroView = centro

        logoView = logoIniciar()

        val pesquisa = TextView(this)
        pesquisa.text = "Pesquisar"
        pesquisa.setTextColor(roxoTexto)
        pesquisa.textSize = 12f
        pesquisa.gravity = Gravity.CENTER_VERTICAL
        pesquisa.setPadding(dp(16), 0, dp(16), 0)
        pesquisa.background = arred(Atalhos.comAlfa(roxo, 0x33), 16)
        val lpPesquisa = LinearLayout.LayoutParams(dp(150), dp(30))
        lpPesquisa.rightMargin = dp(10)
        pesquisa.layoutParams = lpPesquisa
        pesquisa.setOnClickListener { alternarIniciar() }
        pesquisaView = pesquisa

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
        bandeja.background = arred(Atalhos.comAlfa(roxo, 0x22), 8)
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
        data.setTextColor(roxoSuave)
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
            minOf(dp(300), larguraTela - dp(16)), FrameLayout.LayoutParams.WRAP_CONTENT,
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
        montarFixos()
    }
}
