package com.meuapp.fps

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

class JanelaService : Service() {

    private var wm: WindowManager? = null
    private val janelas = ArrayList<View>()

    private val uaPc = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarNotificacao()
        if (wm == null) wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (intent?.getStringExtra("acao") == "fechar") {
            fecharTudo()
            stopSelf()
            return START_NOT_STICKY
        }
        val url = intent?.getStringExtra("url")
        val titulo = intent?.getStringExtra("titulo") ?: "Janela"

        if (titulo == "Opera") {
            val opera = Atalhos.pacoteOpera(this)
            if (opera != null) {
                Atalhos.abrirJanela(this, opera)
                if (janelas.isEmpty()) stopSelf()
                return START_NOT_STICKY
            }
        }

        if (url != null) criarJanela(titulo, url)
        return START_NOT_STICKY
    }

    private fun iniciarNotificacao() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("janelas", "Janelas", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notif = Notification.Builder(this, "janelas")
            .setContentTitle("Janelas abertas")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(2, notif, 0x40000000)
        } else {
            startForeground(2, notif)
        }
    }

    private fun botao(texto: String): TextView {
        val b = TextView(this)
        b.text = texto
        b.setTextColor(Color.parseColor("#D0D0D0"))
        b.textSize = 16f
        b.gravity = Gravity.CENTER
        b.setPadding(dp(11), 0, dp(11), 0)
        b.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)
        )
        return b
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun criarJanela(titulo: String, url: String) {
        val m = resources.displayMetrics
        val largura = (m.widthPixels * 0.6).toInt()
        val altura = (m.heightPixels * 0.7).toInt()
        val maxW = (m.widthPixels * 0.92).toInt()
        val maxH = (m.heightPixels * 0.85).toInt()

        val moldura = GradientDrawable()
        moldura.setColor(Color.parseColor("#202020"))
        moldura.cornerRadius = dp(14).toFloat()
        moldura.setStroke(dp(1), Color.parseColor("#3A3A3A"))

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.background = moldura
        raiz.clipToOutline = true

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setBackgroundColor(Color.parseColor("#2B2B2B"))
        barra.setPadding(dp(12), 0, dp(4), 0)

        val t = TextView(this)
        t.text = titulo
        t.setTextColor(Color.parseColor("#E8E8E8"))
        t.textSize = 13f
        t.gravity = Gravity.CENTER_VERTICAL
        t.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f)

        val voltar = botao("‹")
        val teclado = botao("Aa")
        val min = botao("−")
        val max = botao("□")
        val fechar = botao("×")

        barra.addView(t)
        barra.addView(voltar)
        barra.addView(teclado)
        barra.addView(min)
        barra.addView(max)
        barra.addView(fechar)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = uaPc
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.loadUrl(url)
        web.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        raiz.addView(barra)
        raiz.addView(web)

        val params = WindowManager.LayoutParams(
            largura,
            altura,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = dp(20) + janelas.size * dp(40)
        params.y = dp(40) + janelas.size * dp(40)

        var iniX = 0
        var iniY = 0
        var toqueX = 0f
        var toqueY = 0f
        barra.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    iniX = params.x
                    iniY = params.y
                    toqueX = e.rawX
                    toqueY = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = iniX + (e.rawX - toqueX).toInt()
                    params.y = iniY + (e.rawY - toqueY).toInt()
                    wm?.updateViewLayout(raiz, params)
                }
            }
            true
        }

        voltar.setOnClickListener {
            if (web.canGoBack()) web.goBack()
        }

        var focavel = false
        teclado.setOnClickListener {
            focavel = !focavel
            if (focavel) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            wm?.updateViewLayout(raiz, params)
            teclado.setTextColor(
                if (focavel) Color.parseColor("#4CC2FF") else Color.parseColor("#D0D0D0")
            )
        }

        var minimizado = false
        var maximizado = false

        min.setOnClickListener {
            minimizado = !minimizado
            if (minimizado) {
                web.visibility = View.GONE
                voltar.visibility = View.GONE
                teclado.visibility = View.GONE
                max.visibility = View.GONE
                params.width = dp(200)
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                min.text = "+"
            } else {
                web.visibility = View.VISIBLE
                voltar.visibility = View.VISIBLE
                teclado.visibility = View.VISIBLE
                max.visibility = View.VISIBLE
                params.width = if (maximizado) maxW else largura
                params.height = if (maximizado) maxH else altura
                min.text = "−"
            }
            wm?.updateViewLayout(raiz, params)
        }

        max.setOnClickListener {
            maximizado = !maximizado
            if (maximizado) {
                params.width = maxW
                params.height = maxH
                params.x = dp(10)
                params.y = dp(20)
            } else {
                params.width = largura
                params.height = altura
            }
            wm?.updateViewLayout(raiz, params)
        }

        fechar.setOnClickListener {
            try {
                wm?.removeView(raiz)
            } catch (e: Throwable) {
            }
            janelas.remove(raiz)
            web.destroy()
            if (janelas.isEmpty()) stopSelf()
        }

        wm?.addView(raiz, params)
        janelas.add(raiz)
    }

    private fun fecharTudo() {
        for (v in janelas) {
            try {
                wm?.removeView(v)
            } catch (e: Throwable) {
            }
        }
        janelas.clear()
    }

    override fun onDestroy() {
        fecharTudo()
        super.onDestroy()
    }
}
