package com.meuapp.fps

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class TeclaService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var barra: View? = null

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val k = event.keyCode
        if (k == KeyEvent.KEYCODE_F11 || k == KeyEvent.KEYCODE_F5) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (barra != null) esconder() else mostrar()
            }
            return true
        }
        return false
    }

    private fun esconder() {
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
        return lista.filter { existe(it) }
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
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", "fechar")
        try {
            startForegroundService(i)
        } catch (e: Throwable) {
        }
    }

    private fun abrirTela(cls: Class<*>) {
        val i = Intent(this, cls)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(i)
        } catch (e: Throwable) {
        }
    }

    private fun texto(t: String, acao: () -> Unit): TextView {
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
            esconder()
            acao()
        }
        return v
    }

    private fun controleBrilho(): SeekBar {
        val s = SeekBar(this)
        s.max = 255
        s.progress = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        s.progressTintList = ColorStateList.valueOf(Color.parseColor("#4CC2FF"))
        s.thumbTintList = ColorStateList.valueOf(Color.parseColor("#4CC2FF"))
        s.layoutParams = LinearLayout.LayoutParams(dp(110), dp(38))
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                val v = maxOf(sb?.progress ?: 128, 5)
                if (!Atalhos.shizukuOk()) {
                    Toast.makeText(this@TeclaService, "Ligue o Shizuku", Toast.LENGTH_SHORT).show()
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
        fundo.setColor(Color.parseColor("#E6202020"))
        fundo.cornerRadius = dp(24).toFloat()
        fundo.setStroke(dp(1), Color.parseColor("#3A3A3A"))

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
        sol.setTextColor(Color.parseColor("#D0D0D0"))
        sol.textSize = 16f
        sol.setPadding(dp(8), 0, 0, 0)
        linha.addView(sol)
        linha.addView(controleBrilho())

        linha.addView(texto("☰") { abrirTela(PainelActivity::class.java) })
        linha.addView(texto("×") { fecharJanelas() })
        linha.addView(texto("+") { abrirTela(AtalhosActivity::class.java) })

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
