package com.meuapp.fps

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView

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

    private fun abrir(titulo: String, url: String) {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("titulo", titulo)
        i.putExtra("url", url)
        try {
            startForegroundService(i)
        } catch (e: Throwable) {
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

    private fun item(texto: String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.BLACK)
        t.setBackgroundColor(Color.parseColor("#FF8A00"))
        t.textSize = 14f
        t.setPadding(dp(14), dp(8), dp(14), dp(8))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.rightMargin = dp(8)
        t.layoutParams = lp
        t.setOnClickListener { acao() }
        return t
    }

    private fun mostrar() {
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val linha = LinearLayout(this)
        linha.orientation = LinearLayout.HORIZONTAL
        linha.gravity = Gravity.CENTER_VERTICAL
        linha.setBackgroundColor(Color.parseColor("#CC121212"))
        linha.setPadding(dp(12), dp(8), dp(12), dp(8))

        linha.addView(item("YouTube") { abrir("YouTube", "https://m.youtube.com") })
        linha.addView(item("Google") { abrir("Google", "https://www.google.com") })
        linha.addView(item("Chrome") { abrir("Chrome", "https://www.google.com") })
        linha.addView(item("TikTok") { abrir("TikTok", "https://www.tiktok.com") })
        linha.addView(item("Fechar janelas") { fecharJanelas() })
        linha.addView(item("Esconder barra (F11)") { esconder() })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        wm?.addView(linha, params)
        barra = linha
    }

    override fun onUnbind(intent: Intent?): Boolean {
        esconder()
        return super.onUnbind(intent)
    }
}
