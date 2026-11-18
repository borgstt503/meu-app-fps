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
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class TeclaService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var barra: View? = null
    private var painel: View? = null
    private var resolucao: View? = null
    private var ultimoF5 = 0L

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

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
        val tratar = k == KeyEvent.KEYCODE_F4 || k == KeyEvent.KEYCODE_F5 ||
            k == KeyEvent.KEYCODE_F6 || k == KeyEvent.KEYCODE_F7 ||
            k == KeyEvent.KEYCODE_F8 || k == KeyEvent.KEYCODE_F9 ||
            k == KeyEvent.KEYCODE_F10 || k == KeyEvent.KEYCODE_F11 ||
            k == KeyEvent.KEYCODE_F12
        if (!tratar) return false

        if (k == KeyEvent.KEYCODE_F5 && JanelaService.total == 0) {
            val p = appNaFrente()
            if (p == null || ignorar(p) || ehNavegador(p)) return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (k) {
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

    private fun irParaInicio() {
        esconder()
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
        b.setTextColor(Color.parseColor("#E4D4FF"))
        b.textSize = 14f
        b.gravity = Gravity.CENTER
        val g = GradientDrawable()
        g.setColor(Color.parseColor("#3A2A5C"))
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
        fundo.setColor(Color.parseColor("#F21C1230"))
        fundo.cornerRadius = dp(14).toFloat()
        fundo.setStroke(dp(1), Color.parseColor("#4A2F7A"))
        caixa.background = fundo
        caixa.setPadding(dp(16), dp(14), dp(16), dp(14))

        val titulo = TextView(this)
        titulo.text = "Resolução da tela"
        titulo.setTextColor(Color.parseColor("#B388FF"))
        titulo.textSize = 14f

        val status = TextView(this)
        status.text = "Ligue o Shizuku para usar"
        status.setTextColor(Color.parseColor("#E4D4FF"))
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

        caixa.addView(titulo)
        caixa.addView(status)
        caixa.addView(botaoRes("1080 x 1920") { rodarRes("wm size 1080x1920") })
        caixa.addView(botaoRes("Resetar resolução") { rodarRes("wm size reset") })
        caixa.addView(botaoRes("Tamanho atual") { rodarRes("wm size") })
        caixa.addView(botaoRes("Fechar") { fecharResolucao() })

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
        v.setTextColor(Color.parseColor("#D0D0D0"))
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

    private fun controleBrilho(): SeekBar {
        val s = SeekBar(this)
        s.max = 255
        s.progress = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        s.progressTintList = ColorStateList.valueOf(Color.parseColor("#B388FF"))
        s.thumbTintList = ColorStateList.valueOf(Color.parseColor("#B388FF"))
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
        fundo.setColor(Color.parseColor("#E61C1230"))
        fundo.cornerRadius = dp(24).toFloat()
        fundo.setStroke(dp(1), Color.parseColor("#4A2F7A"))

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
        sol.setTextColor(Color.parseColor("#CDB4FF"))
        sol.textSize = 16f
        sol.setPadding(dp(8), 0, 0, 0)
        linha.addView(sol)
        linha.addView(controleBrilho())

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
        esconder()
        fecharResolucao()
        return super.onUnbind(intent)
    }
}
