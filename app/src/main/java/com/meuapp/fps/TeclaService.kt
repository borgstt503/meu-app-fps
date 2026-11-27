package com.meuapp.fps

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView

class TeclaService : AccessibilityService() {

    companion object {
        @Volatile
        var instancia: TeclaService? = null
    }

    private var wm: WindowManager? = null
    private var barra: View? = null
    private var painel: View? = null
    private var resolucao: View? = null
    private var taskbar: View? = null
    private var iniciar: View? = null
    private var controle: View? = null
    private var faixa: View? = null
    private var alca: View? = null
    private var ultimoF5 = 0L
    private var ultBorda = 0L

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun ac(): Int {
        return Atalhos.acento(this)
    }

    private fun claro(): Int {
        return Atalhos.claroTema(this)
    }

    private fun fundoT(a: Int): Int {
        return Atalhos.comAlfa(Atalhos.fundoTema(this), a)
    }

    private fun bordaT(): Int {
        return Atalhos.bordaTema(this)
    }

    private fun cartaoT(): Int {
        return Atalhos.cartaoTema(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instancia = this
        atualizarBorda()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    private fun mostrarPorBorda() {
        val agora = System.currentTimeMillis()
        if (agora - ultBorda < 900) return
        ultBorda = agora
        if (taskbar == null) alternarTaskbar()
    }

    private fun removerBorda() {
        try {
            faixa?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        faixa = null
        try {
            alca?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        alca = null
    }

    fun atualizarBorda() {
        removerBorda()
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Atalhos.getB(this, "borda", true)) {
            val f = View(this)
            f.setOnHoverListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_HOVER_ENTER ||
                    e.actionMasked == MotionEvent.ACTION_HOVER_MOVE
                ) {
                    mostrarPorBorda()
                }
                false
            }
            var iniY = 0f
            f.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> iniY = e.rawY
                    MotionEvent.ACTION_MOVE -> {
                        if (iniY - e.rawY > dp(20)) mostrarPorBorda()
                    }
                    MotionEvent.ACTION_UP -> {
                        if (Math.abs(iniY - e.rawY) < dp(6)) mostrarPorBorda()
                    }
                }
                true
            }
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dp(6),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            p.gravity = Gravity.BOTTOM
            try {
                wm?.addView(f, p)
                faixa = f
            } catch (e: Throwable) {
            }
        }

        if (Atalhos.getB(this, "alca", true)) {
            val a = View(this)
            val g = GradientDrawable()
            g.setColor(Atalhos.comAlfa(ac(), 0xB0))
            g.cornerRadius = dp(5).toFloat()
            a.background = g
            a.setOnClickListener { alternarTaskbar() }
            a.setOnHoverListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_HOVER_ENTER) mostrarPorBorda()
                false
            }
            val pa = WindowManager.LayoutParams(
                dp(72),
                dp(10),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            pa.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            pa.y = dp(6)
            try {
                wm?.addView(a, pa)
                alca = a
            } catch (e: Throwable) {
            }
        }
    }

    private fun appNaFrente(): String? {
        return try {
            val r = rootInActiveWindow
            val p = r?.packageName?.toString()
            r?.recycle()
            p
        } catch (e: Throwable) {
            null
        }
    }

    private fun pacoteHome(): String? {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(i, 0)?.activityInfo?.packageName
    }

    private fun ignorar(p: String): Boolean {
        if (p == packageName) return true
        if (p == "com.android.systemui" || p == "android") return true
        if (p == "moe.shizuku.privileged.api") return true
        if (p == pacoteHome()) return true
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null && ime.startsWith(p)) return true
        return false
    }

    private fun ehNavegador(p: String): Boolean {
        val x = p.lowercase()
        return x.contains("chrome") || x.contains("opera") || x.contains("browser") ||
            x.contains("firefox") || x.contains("brave") || x.contains("edge")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val k = event.keyCode
        val tratar = k == KeyEvent.KEYCODE_F1 || k == KeyEvent.KEYCODE_F2 ||
            k == KeyEvent.KEYCODE_F3 || k == KeyEvent.KEYCODE_F4 ||
            k == KeyEvent.KEYCODE_F5 || k == KeyEvent.KEYCODE_F6 ||
            k == KeyEvent.KEYCODE_F7 || k == KeyEvent.KEYCODE_F8 ||
            k == KeyEvent.KEYCODE_F9 || k == KeyEvent.KEYCODE_F10 ||
            k == KeyEvent.KEYCODE_F11 || k == KeyEvent.KEYCODE_F12
        if (!tratar) return false

        if (k == KeyEvent.KEYCODE_F5 && JanelaService.total == 0) {
            val p = appNaFrente()
            if (p == null || ignorar(p) || ehNavegador(p)) return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (k) {
                KeyEvent.KEYCODE_F1 -> recentes()
                KeyEvent.KEYCODE_F2 -> alternarControle()
                KeyEvent.KEYCODE_F3 -> alternarTaskbar()
                KeyEvent.KEYCODE_F4 -> alternarFixar()
                KeyEvent.KEYCODE_F5 -> {
                    if (JanelaService.total > 0) recarregarJanela() else reiniciarApp()
                }
                KeyEvent.KEYCODE_F6 -> alternarResolucao()
                KeyEvent.KEYCODE_F7 -> alternarTeclado()
                KeyEvent.KEYCODE_F8 -> irParaInicio()
                KeyEvent.KEYCODE_F9 -> alternarPainel()
                KeyEvent.KEYCODE_F10 -> alternarOverlayFps()
                KeyEvent.KEYCODE_F12 -> alternarTela()
                else -> if (barra != null) esconder() else mostrar()
            }
        }
        return true
    }

    private fun enviarJanela(acao: String) {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", acao)
        try {
            startForegroundService(i)
        } catch (e: Throwable) {
        }
    }

    private fun recarregarJanela() {
        enviarJanela("recarregar")
    }

    private fun alternarTeclado() {
        if (JanelaService.total > 0) {
            enviarJanela("teclado")
        } else {
            Atalhos.aviso(this, "Nenhuma janela flutuante aberta")
        }
    }

    private fun recentes() {
        esconder()
        esconderTaskbar()
        esconderControle()
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    private fun irParaInicio() {
        esconder()
        esconderTaskbar()
        esconderControle()
        performGlobalAction(GLOBAL_ACTION_HOME)
        abrirTela(DesktopActivity::class.java)
    }

    private fun reiniciarApp() {
        val p = appNaFrente() ?: return
        val agora = System.currentTimeMillis()
        if (agora - ultimoF5 > 3000) {
            ultimoF5 = agora
            Atalhos.aviso(this, "Aperte F5 de novo para reiniciar " + Atalhos.nomeDe(this, p))
            return
        }
        ultimoF5 = 0
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return
        }
        val comp = packageManager.getLaunchIntentForPackage(p)
            ?.component?.flattenToShortString()
        Thread {
            Atalhos.rodar("am force-stop " + p)
            Thread.sleep(700)
            if (comp != null) Atalhos.rodar("am start -n " + comp)
        }.start()
    }

    private fun alternarTela() {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val ligada = pm.isInteractive
        Thread {
            Atalhos.rodar("input keyevent " + (if (ligada) "223" else "224"))
        }.start()
    }

    private fun alternarFixar() {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(this, "Ligue o Shizuku e autorize o app")
            return
        }
        Thread {
            val estado = Atalhos.rodar("dumpsys activity activities | grep -i LockTaskModeState")
            val travado = estado.isNotBlank() && !estado.contains("NONE", true)
            if (travado) {
                Atalhos.rodar("am task lock stop")
                Atalhos.aviso(this, "Tela desafixada")
                return@Thread
            }
            val r = Atalhos.rodar("dumpsys activity activities | grep ResumedActivity")
            val id = Regex("t(\\d+)\\}").find(r)?.groupValues?.get(1)
            if (id == null) {
                Atalhos.aviso(this, "Não achei o app da frente")
                return@Thread
            }
            val saida = Atalhos.rodar("am task lock " + id + " 2>&1")
            if (saida.contains("Error", true) || saida.contains("Exception", true)) {
                Atalhos.aviso(this, "Falhou: " + saida.take(120))
            } else {
                Atalhos.aviso(this, "Tela fixada. Aperte F4 de novo para soltar")
            }
        }.start()
    }

    private fun presetsRes(): List<String> {
        val s = getSharedPreferences("cfg", Context.MODE_PRIVATE).getString("res_presets", null)
        val base = if (s.isNullOrEmpty()) listOf("1080x1920") else s.split(",")
        val l = base.filter { it.matches(Regex("\\d+x\\d+")) }.toMutableList()
        if (!l.contains("1080x1920")) l.add(0, "1080x1920")
        return l
    }

    private fun fecharResolucao() {
        try {
            resolucao?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        resolucao = null
    }

    private fun botaoRes(texto: String, acao: () -> Unit): TextView {
        val b = TextView(this)
        b.text = texto
        b.setTextColor(claro())
        b.textSize = 14f
        b.gravity = Gravity.CENTER
        val g = GradientDrawable()
        g.setColor(cartaoT())
        g.cornerRadius = dp(10).toFloat()
        b.background = g
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        )
        lp.topMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { acao() }
        return b
    }

    private fun alternarResolucao() {
        if (resolucao != null) {
            fecharResolucao()
            return
        }
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        val fundo = GradientDrawable()
        fundo.setColor(fundoT(0xF2))
        fundo.cornerRadius = dp(14).toFloat()
        fundo.setStroke(dp(1), bordaT())
        caixa.background = fundo
        caixa.setPadding(dp(16), dp(14), dp(16), dp(14))

        val titulo = TextView(this)
        titulo.text = "Resolução da tela"
        titulo.setTextColor(ac())
        titulo.textSize = 14f

        val status = TextView(this)
        status.text = "Ligue o Shizuku para usar"
        status.setTextColor(claro())
        status.textSize = 12f
        status.setPadding(0, dp(8), 0, dp(4))

        fun rodarRes(cmd: String) {
            if (!Atalhos.shizukuOk()) {
                status.text = "Ligue o Shizuku e autorize o app"
                return
            }
            status.text = "Aplicando..."
            Thread {
                Atalhos.rodar(cmd)
                Thread.sleep(600)
                val r = Atalhos.rodar("wm size").trim()
                status.post { status.text = r }
            }.start()
        }

        val lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        for (p in presetsRes()) {
            lista.addView(botaoRes(p.replace("x", " x ")) { rodarRes("wm size " + p) })
        }
        lista.addView(botaoRes("Resetar resolução") { rodarRes("wm size reset") })
        lista.addView(botaoRes("Tamanho atual") { rodarRes("wm size") })
        lista.addView(
            botaoRes("Outra resolução...") {
                fecharResolucao()
                abrirTela(ResolucaoActivity::class.java)
            }
        )
        lista.addView(botaoRes("Fechar") { fecharResolucao() })

        val sc = ScrollView(this)
        sc.addView(lista)
        sc.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(230)
        )

        caixa.addView(titulo)
        caixa.addView(status)
        caixa.addView(sc)

        val params = WindowManager.LayoutParams(
            dp(300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        wm?.addView(caixa, params)
        resolucao = caixa
        if (Atalhos.shizukuOk()) rodarRes("wm size")
    }

    private fun overlayRodando(): Boolean {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            am.getRunningServices(100).any {
                it.service.className == OverlayService::class.java.name
            }
        } catch (e: Throwable) {
            false
        }
    }

    private fun alternarOverlayFps() {
        if (overlayRodando()) {
            stopService(Intent(this, OverlayService::class.java))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Atalhos.aviso(this, "Ative 'Exibir sobre outros apps' para o Sent")
            return
        }
        try {
            startForegroundService(Intent(this, OverlayService::class.java))
        } catch (e: Throwable) {
            Atalhos.aviso(this, "Não foi possível ligar o overlay")
        }
    }

    private fun esconderPainel() {
        try {
            painel?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        painel = null
    }

    private fun esconder() {
        esconderPainel()
        try {
            barra?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        barra = null
    }

    private fun esconderIniciar() {
        try {
            iniciar?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        iniciar = null
    }

    private fun esconderTaskbar() {
        esconderIniciar()
        try {
            taskbar?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        taskbar = null
    }

    private fun esconderControle() {
        try {
            controle?.let { wm?.removeView(it) }
        } catch (e: Throwable) {
        }
        controle = null
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

    private fun iconeDe(p: String): Drawable {
        return try {
            packageManager.getApplicationIcon(p)
        } catch (e: Throwable) {
            val nome = Atalhos.sites[p]?.first ?: p.substringAfterLast('.')
            val cor = if (p == "com.google.android.youtube") {
                Color.parseColor("#E62117")
            } else {
                Color.parseColor("#555555")
            }
            iconeLetra(nome.take(1).uppercase(), cor)
        }
    }

    private fun rotulo(p: String): String {
        if (Atalhos.instalado(this, p)) return Atalhos.nomeDe(this, p)
        return Atalhos.sites[p]?.first ?: p
    }

    private fun existe(pacote: String): Boolean {
        return Atalhos.instalado(this, pacote) || Atalhos.sites.containsKey(pacote)
    }

    private fun pacotesDaBarra(): List<String> {
        val salvos = getSharedPreferences("cfg", Context.MODE_PRIVATE)
            .getStringSet("f11", null)
        val lista = ArrayList<String>()
        if (salvos != null && salvos.isNotEmpty()) {
            lista.addAll(salvos)
        } else {
            val opera = Atalhos.pacoteOpera(this)
            if (opera != null) lista.add(opera)
            lista.add("com.android.chrome")
            lista.add("com.google.android.youtube")
            lista.add("com.zhiliaoapp.musically")
        }
        return lista.filter { existe(it) }.sortedBy { rotulo(it).lowercase() }
    }

    private fun padraoFixados(): List<String> {
        val l = ArrayList<String>()
        l.add("com.android.chrome")
        l.add(Atalhos.pacoteOpera(this) ?: "com.opera.browser")
        l.add("com.google.android.youtube")
        l.add("com.zhiliaoapp.musically")
        l.add("com.dts.freefireth")
        l.add("moe.shizuku.privileged.api")
        return l
    }

    private fun listaFixados(): List<String> {
        val s = getSharedPreferences("cfg", Context.MODE_PRIVATE).getString("barra_lista", null)
        val base: List<String> = if (s == null) {
            padraoFixados()
        } else if (s.isEmpty()) {
            emptyList()
        } else {
            s.split(",")
        }
        return base.filter { it != packageName && existe(it) }
    }

    private fun abrir(pacote: String) {
        val site = Atalhos.sites[pacote]
        if (site != null) {
            val i = Intent(this, JanelaService::class.java)
            i.putExtra("titulo", site.first)
            i.putExtra("url", site.second)
            try {
                startForegroundService(i)
            } catch (e: Throwable) {
            }
        } else {
            Atalhos.abrirJanela(this, pacote)
        }
    }

    private fun abrirDaBarra(p: String) {
        if (p == "com.dts.freefireth" || p == "com.dts.freefiremax" ||
            p == "moe.shizuku.privileged.api"
        ) {
            Atalhos.abrirNormal(this, p)
        } else {
            abrir(p)
        }
    }

    private fun fecharJanelas() {
        enviarJanela("fechar")
    }

    private fun abrirTela(cls: Class<*>) {
        val i = Intent(this, cls)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(i)
        } catch (e: Throwable) {
        }
    }

    private fun alternarPainel() {
        if (painel != null) {
            esconderPainel()
            return
        }
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = Painel.build(this) { esconderPainel() }
        val largura = minOf(dp(344), resources.displayMetrics.widthPixels - dp(20))
        val params = WindowManager.LayoutParams(
            largura,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.y = dp(64)
        params.x = dp(10)
        v.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) {
                esconderPainel()
                true
            } else {
                false
            }
        }
        wm?.addView(v, params)
        painel = v
    }

    private fun texto(t: String, fechaBarra: Boolean, acao: () -> Unit): TextView {
        val v = TextView(this)
        v.text = t
        v.setTextColor(claro())
        v.textSize = 20f
        v.gravity = Gravity.CENTER
        v.setPadding(dp(10), 0, dp(10), 0)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)
        )
        v.setOnClickListener {
            if (fechaBarra) esconder()
            acao()
        }
        return v
    }

    private fun chip(t: String, acao: () -> Unit): TextView {
        val v = TextView(this)
        v.text = t
        v.setTextColor(claro())
        v.textSize = 13f
        v.gravity = Gravity.CENTER
        val g = GradientDrawable()
        g.setColor(cartaoT())
        g.cornerRadius = dp(10).toFloat()
        v.background = g
        v.setPadding(dp(12), 0, dp(12), 0)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)
        )
        lp.setMargins(dp(3), 0, dp(3), 0)
        v.layoutParams = lp
        v.setOnClickListener { acao() }
        return v
    }

    private fun alternarControle() {
        if (controle != null) {
            esconderControle()
            return
        }
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val fundo = GradientDrawable()
        fundo.setColor(fundoT(0xF2))
        fundo.cornerRadius = dp(24).toFloat()
        fundo.setStroke(dp(1), bordaT())

        val linha = LinearLayout(this)
        linha.orientation = LinearLayout.HORIZONTAL
        linha.gravity = Gravity.CENTER_VERTICAL
        linha.background = fundo
        linha.setPadding(dp(10), dp(8), dp(10), dp(8))

        linha.addView(chip("Esq") { Janelas.metade(this, false) })
        linha.addView(chip("Dir") { Janelas.metade(this, true) })
        linha.addView(chip("−") { Janelas.mudarTamanho(this, 0.87f) })
        linha.addView(chip("+") { Janelas.mudarTamanho(this, 1.15f) })
        linha.addView(chip("Min") { Janelas.minimizar(this) })
        linha.addView(chip("Máx") { Janelas.maximizar(this) })
        linha.addView(chip("Padrão") { Janelas.padrao(this) })
        linha.addView(chip("×") { esconderControle() })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dp(70)

        wm?.addView(linha, params)
        controle = linha
    }

    private fun logoStart(): LinearLayout {
        val caixa = LinearLayout(this)
        caixa.gravity = Gravity.CENTER
        val lpCaixa = LinearLayout.LayoutParams(dp(38), dp(38))
        lpCaixa.setMargins(dp(2), 0, dp(6), 0)
        caixa.layoutParams = lpCaixa

        val grade = GridLayout(this)
        grade.rowCount = 2
        grade.columnCount = 2
        for (i in 0 until 4) {
            val q = View(this)
            q.setBackgroundColor(ac())
            val lp = GridLayout.LayoutParams()
            lp.width = dp(9)
            lp.height = dp(9)
            lp.setMargins(dp(1), dp(1), dp(1), dp(1))
            q.layoutParams = lp
            grade.addView(q)
        }
        caixa.addView(grade)
        caixa.setOnClickListener { alternarIniciar() }
        return caixa
    }

    private fun iconeTaskbar(p: String): ImageView {
        val img = ImageView(this)
        img.setImageDrawable(iconeDe(p))
        img.setPadding(dp(4), dp(4), dp(4), dp(4))
        val lp = LinearLayout.LayoutParams(dp(38), dp(38))
        lp.setMargins(dp(3), 0, dp(3), 0)
        img.layoutParams = lp
        img.setOnClickListener {
            esconderTaskbar()
            abrirDaBarra(p)
        }
        img.setOnLongClickListener {
            esconderTaskbar()
            Atalhos.abrirNormal(this, p)
            true
        }
        return img
    }

    private fun alternarIniciar() {
        if (iniciar != null) {
            esconderIniciar()
            return
        }
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(principal, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val g = GridLayout(this)
        g.columnCount = 5
        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL

            val img = ImageView(this)
            img.setImageDrawable(ri.loadIcon(packageManager))
            img.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))

            val nome = TextView(this)
            nome.text = ri.loadLabel(packageManager).toString()
            nome.setTextColor(claro())
            nome.textSize = 10f
            nome.maxLines = 1
            nome.ellipsize = TextUtils.TruncateAt.END
            nome.gravity = Gravity.CENTER
            nome.setPadding(0, dp(3), 0, 0)

            item.addView(img)
            item.addView(nome)

            val lp = GridLayout.LayoutParams()
            lp.width = dp(72)
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.setMargins(dp(2), dp(6), dp(2), dp(6))
            item.layoutParams = lp

            item.setOnClickListener {
                esconderTaskbar()
                abrirDaBarra(pkg)
            }
            item.setOnLongClickListener {
                esconderTaskbar()
                Atalhos.abrirNormal(this, pkg)
                true
            }
            g.addView(item)
        }

        val scroll = ScrollView(this)
        scroll.addView(g)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(250)
        )

        val titulo = TextView(this)
        titulo.text = "Aplicativos  (toque: janela, segure: normal)"
        titulo.setTextColor(ac())
        titulo.textSize = 12f
        titulo.setPadding(dp(4), 0, 0, dp(6))

        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        val fundo = GradientDrawable()
        fundo.setColor(fundoT(0xF2))
        fundo.cornerRadius = dp(14).toFloat()
        fundo.setStroke(dp(1), bordaT())
        caixa.background = fundo
        caixa.setPadding(dp(12), dp(12), dp(12), dp(8))
        caixa.addView(titulo)
        caixa.addView(scroll)

        val largura = minOf(dp(440), resources.displayMetrics.widthPixels - dp(16))
        val params = WindowManager.LayoutParams(
            largura,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = dp(110)

        wm?.addView(caixa, params)
        iniciar = caixa
    }

    private fun alternarTaskbar() {
        if (taskbar != null) {
            esconderTaskbar()
            return
        }
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val fundo = GradientDrawable()
        fundo.setColor(fundoT(0xF2))
        fundo.cornerRadius = dp(24).toFloat()
        fundo.setStroke(dp(1), bordaT())

        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        caixa.gravity = Gravity.CENTER_HORIZONTAL
        caixa.background = fundo
        caixa.setPadding(dp(10), dp(6), dp(10), dp(8))

        val apps = LinearLayout(this)
        apps.orientation = LinearLayout.HORIZONTAL
        apps.gravity = Gravity.CENTER_VERTICAL
        apps.addView(logoStart())
        for (p in listaFixados()) {
            apps.addView(iconeTaskbar(p))
        }

        val relogio = TextClock(this)
        relogio.format24Hour = "HH:mm"
        relogio.format12Hour = "HH:mm"
        relogio.setTextColor(claro())
        relogio.textSize = 13f
        relogio.setPadding(dp(12), 0, dp(6), 0)
        apps.addView(relogio)

        val tools = LinearLayout(this)
        tools.orientation = LinearLayout.HORIZONTAL
        tools.gravity = Gravity.CENTER_VERTICAL
        tools.addView(chip("FPS") {
            esconderTaskbar()
            alternarOverlayFps()
        })
        tools.addView(chip("Painel") {
            esconderTaskbar()
            alternarPainel()
        })
        tools.addView(chip("Resolução") {
            esconderTaskbar()
            alternarResolucao()
        })
        tools.addView(chip("Janela") {
            esconderTaskbar()
            alternarControle()
        })
        tools.addView(chip("Recentes") { recentes() })
        tools.addView(chip("−") { Janelas.mudarTamanho(this, 0.87f) })
        tools.addView(chip("+") { Janelas.mudarTamanho(this, 1.15f) })
        tools.addView(chip("Início") { irParaInicio() })
        tools.addView(chip("×") { fecharJanelas() })

        val rolagem = HorizontalScrollView(this)
        rolagem.isHorizontalScrollBarEnabled = false
        rolagem.setPadding(0, dp(6), 0, 0)
        rolagem.addView(tools)

        caixa.addView(apps)
        caixa.addView(rolagem)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = dp(22)

        caixa.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) {
                esconderTaskbar()
                true
            } else {
                false
            }
        }

        wm?.addView(caixa, params)
        taskbar = caixa
    }

    private fun controleBrilho(): SeekBar {
        val s = SeekBar(this)
        s.max = 255
        s.progress = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        s.progressTintList = ColorStateList.valueOf(ac())
        s.thumbTintList = ColorStateList.valueOf(ac())
        s.layoutParams = LinearLayout.LayoutParams(dp(110), dp(38))
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {}

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                val v = maxOf(sb?.progress ?: 128, 5)
                if (!Atalhos.shizukuOk()) {
                    Atalhos.aviso(this@TeclaService, "Ligue o Shizuku")
                } else {
                    Thread {
                        Atalhos.rodar(
                            "settings put system screen_brightness_mode 0; " +
                                "settings put system screen_brightness " + v
                        )
                    }.start()
                }
            }
        })
        return s
    }

    private fun mostrar() {
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val fundo = GradientDrawable()
        fundo.setColor(fundoT(0xE6))
        fundo.cornerRadius = dp(24).toFloat()
        fundo.setStroke(dp(1), bordaT())

        val linha = LinearLayout(this)
        linha.orientation = LinearLayout.HORIZONTAL
        linha.gravity = Gravity.CENTER_VERTICAL
        linha.background = fundo
        linha.setPadding(dp(10), dp(6), dp(10), dp(6))

        for (p in pacotesDaBarra()) {
            val img = ImageView(this)
            img.setImageDrawable(iconeDe(p))
            val lp = LinearLayout.LayoutParams(dp(38), dp(38))
            lp.setMargins(dp(4), 0, dp(4), 0)
            img.layoutParams = lp
            img.setPadding(dp(3), dp(3), dp(3), dp(3))
            img.setOnClickListener {
                esconder()
                abrir(p)
            }
            img.setOnLongClickListener {
                esconder()
                Atalhos.abrirNormal(this, p)
                true
            }
            linha.addView(img)
        }

        val sol = TextView(this)
        sol.text = "☀"
        sol.setTextColor(claro())
        sol.textSize = 16f
        sol.setPadding(dp(8), 0, 0, 0)
        linha.addView(sol)
        linha.addView(controleBrilho())

        linha.addView(texto("▢", true) { recentes() })
        linha.addView(texto("☰", false) { alternarPainel() })
        linha.addView(texto("⛶", true) { alternarResolucao() })
        linha.addView(texto("×", true) { fecharJanelas() })
        linha.addView(texto("+", true) { abrirTela(AtalhosActivity::class.java) })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dp(10)

        wm?.addView(linha, params)
        barra = linha
    }

    override fun onUnbind(intent: Intent?): Boolean {
        removerBorda()
        esconder()
        esconderTaskbar()
        esconderControle()
        fecharResolucao()
        instancia = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instancia = null
        super.onDestroy()
    }
}
