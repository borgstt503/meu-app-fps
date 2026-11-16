package com.meuapp.fps

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
        val tratar = k == KeyEvent.KEYCODE_F5 || k == KeyEvent.KEYCODE_F7 ||
            k == KeyEvent.KEYCODE_F8 || k == KeyEvent.KEYCODE_F9 ||
            k == KeyEvent.KEYCODE_F10 || k == KeyEvent.KEYCODE_F11
        if (!tratar) return false

        if (k == KeyEvent.KEYCODE_F5 && JanelaService.total == 0) {
            val p = appNaFrente()
            if (p == null || ignorar(p) || ehNavegador(p)) return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (k) {
                KeyEvent.KEYCODE_F5 -> {
                    if (JanelaService.total > 0) recarregarJanela() else reiniciarApp()
                }
                KeyEvent.KEYCODE_F7 -> alternarTeclado()
                KeyEvent.KEYCODE_F8 -> irParaInicio()
                KeyEvent.KEYCODE_F9 -> alternarPainel()
                KeyEvent.KEYCODE_F10 -> alternarOverlayFps()
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

    private fun existe(pacote: String): Boolean {
        return try {
            packageManager.getApplicationInfo(pacote, 0)
            true
        } catch (e: Throwable) {
            false
        }
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
        return lista.filter { existe(it) }.sortedBy { Atalhos.nomeDe(this, it).lowercase() }
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
            try {
                img.setImageDrawable(packageManager.getApplicationIcon(p))
            } catch (e: Throwable) {
            }
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
        return super.onUnbind(intent)
    }
}
