package com.meuapp.fps

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.io.ByteArrayInputStream

private class CursorView(c: Context, val estilo: Int) : View(c) {
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Atalhos.desenharCursor(canvas, estilo, width.toFloat())
    }
}

private class Moldura(
    c: Context,
    private val cursor: CursorView,
    private val tam: Float
) : FrameLayout(c) {

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                val h = Atalhos.hotspot(cursor.estilo, tam)
                cursor.visibility = View.VISIBLE
                cursor.x = ev.x - h.first
                cursor.y = ev.y - h.second
            }
            MotionEvent.ACTION_HOVER_EXIT -> cursor.visibility = View.GONE
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}

class JanelaService : Service() {

    private var wm: WindowManager? = null
    private val janelas = ArrayList<View>()

    private val uaPc = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val dominios = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google", "adnxs.com", "taboola.com", "outbrain.com",
        "criteo.com", "pubmatic.com", "rubiconproject.com", "openx.net",
        "amazon-adsystem.com", "popads.net", "propellerads.com",
        "exoclick.com", "adsterra.com", "mgid.com", "moatads.com",
        "adcolony.com", "smaato.net", "inmobi.com", "applovin.com",
        "ads.tiktok.com", "analytics.tiktok.com", "ads.youtube.com",
        "imasdk.googleapis.com", "adform.net", "casalemedia.com",
        "yieldmo.com", "sharethrough.com", "teads.tv", "revcontent.com",
        "zedo.com", "bidswitch.net", "smartadserver.com", "adsrvr.org",
        "media.net", "yandex.ru/ads", "an.facebook.com", "unityads.unity3d.com"
    )

    private val padroes = listOf(
        "pagead", "/adservice", "adsbygoogle", "/ads/", "/adserver",
        "googleadservices", "adsense", "popunder", "/api/stats/ads",
        "/get_midroll_", "ad_break", "/ads?"
    )

    private val scriptAnuncios = """
(function(){
  if (window.__sentAd) return;
  window.__sentAd = 1;
  var css = "ins.adsbygoogle,.adsbygoogle,[id^='google_ads'],[id^='div-gpt-ad'],#tads,#tadsb,#bottomads,[data-text-ad],[aria-label='Anúncio'],[aria-label='Anúncios'],ytd-ad-slot-renderer,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-banner-promo-renderer,#player-ads,.ytp-ad-module,.ytp-ad-overlay-container,.video-ads,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer{display:none !important}";
  var s = document.createElement('style');
  s.innerHTML = css;
  (document.head || document.documentElement).appendChild(s);
  setInterval(function(){
    var v = document.querySelector('.ad-showing video');
    if (v && isFinite(v.duration)) { v.muted = true; v.currentTime = v.duration; }
    var b = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern');
    if (b) { b.click(); }
  }, 500);
})();
"""

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Atalhos.comEscala(base))
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

        val pacote = when (titulo) {
            "Opera" -> Atalhos.pacoteOpera(this)
            "YouTube" -> "com.google.android.youtube"
            "Google" -> "com.google.android.googlequicksearchbox"
            "Chrome" -> "com.android.chrome"
            "TikTok" -> "com.zhiliaoapp.musically"
            else -> null
        }
        if (pacote != null && Atalhos.instalado(this, pacote) &&
            (titulo == "Opera" || Atalhos.modoApp(this))
        ) {
            Atalhos.abrirJanela(this, pacote)
            if (janelas.isEmpty()) stopSelf()
            return START_NOT_STICKY
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
        b.setPadding(dp(9), 0, dp(9), 0)
        b.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)
        )
        return b
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun criarJanela(titulo: String, url: String) {
        val m = resources.displayMetrics
        val frac = Atalhos.tamanhos[Atalhos.indiceTamanho(this)]
        var curW = (m.widthPixels * frac).toInt()
        var curH = (m.heightPixels * minOf(0.9f, frac + 0.1f)).toInt()
        val maxW = (m.widthPixels * 0.98f).toInt()
        val maxH = (m.heightPixels * 0.92f).toInt()
        val minW = dp(240)
        val minH = dp(170)
        val tf = Atalhos.typeface(this)

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
        t.maxLines = 1
        t.gravity = Gravity.CENTER_VERTICAL
        t.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f)

        val voltar = botao("‹")
        val teclado = botao("Aa")
        val menos = botao("−")
        val mais = botao("+")
        val min = botao("_")
        val max = botao("□")
        val fechar = botao("×")

        barra.addView(t)
        barra.addView(voltar)
        barra.addView(teclado)
        barra.addView(menos)
        barra.addView(mais)
        barra.addView(min)
        barra.addView(max)
        barra.addView(fechar)
        Atalhos.aplicarFonte(barra, tf)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = uaPc
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.settings.textZoom = Atalhos.zooms[Atalhos.indiceZoom(this)]
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request != null && !request.isForMainFrame &&
                    Atalhos.bloqueio(this@JanelaService)
                ) {
                    val u = request.url.toString().lowercase()
                    val h = request.url.host ?: ""
                    if (dominios.any { h.contains(it) } || padroes.any { u.contains(it) }) {
                        return WebResourceResponse(
                            "text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (Atalhos.bloqueio(this@JanelaService)) {
                    view?.evaluateJavascript(scriptAnuncios, null)
                }
            }
        }
        web.webChromeClient = WebChromeClient()
        web.loadUrl(url)

        val tamCursor = dp(22)
        val cursor = CursorView(this, Atalhos.indiceCursor(this))
        cursor.visibility = View.GONE

        val area = Moldura(this, cursor, tamCursor.toFloat())
        area.addView(
            web,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        area.addView(cursor, FrameLayout.LayoutParams(tamCursor, tamCursor))
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        raiz.addView(barra)
        raiz.addView(area)

        val params = WindowManager.LayoutParams(
            curW,
            curH,
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

        var minimizado = false
        var maximizado = false
        var antW = curW
        var antH = curH

        fun aplicar() {
            if (!minimizado) {
                params.width = curW
                params.height = curH
                wm?.updateViewLayout(raiz, params)
            }
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

        mais.setOnClickListener {
            curW = minOf((curW * 1.15f).toInt(), maxW)
            curH = minOf((curH * 1.15f).toInt(), maxH)
            maximizado = false
            aplicar()
        }

        menos.setOnClickListener {
            curW = maxOf((curW * 0.87f).toInt(), minW)
            curH = maxOf((curH * 0.87f).toInt(), minH)
            maximizado = false
            aplicar()
        }

        val escondiveis = listOf<View>(voltar, teclado, menos, mais, max)
        min.setOnClickListener {
            minimizado = !minimizado
            if (minimizado) {
                area.visibility = View.GONE
                for (v in escondiveis) v.visibility = View.GONE
                params.width = dp(240)
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                min.text = "↑"
                wm?.updateViewLayout(raiz, params)
            } else {
                area.visibility = View.VISIBLE
                for (v in escondiveis) v.visibility = View.VISIBLE
                min.text = "_"
                aplicar()
            }
        }

        max.setOnClickListener {
            if (!maximizado) {
                antW = curW
                antH = curH
                curW = maxW
                curH = maxH
                params.x = (m.widthPixels - maxW) / 2
                params.y = dp(12)
            } else {
                curW = antW
                curH = antH
            }
            maximizado = !maximizado
            aplicar()
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
