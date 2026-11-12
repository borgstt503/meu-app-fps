package com.meuapp.fps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var texto: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val formato = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val relogio = object : Runnable {
        override fun run() {
            texto?.text = "FPS --   " + formato.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarNotificacao()
        if (texto == null) criarOverlay()
        return START_STICKY
    }

    private fun iniciarNotificacao() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("overlay", "Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notif = Notification.Builder(this, "overlay")
            .setContentTitle("Overlay ativo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, 0x40000000)
        } else {
            startForeground(1, notif)
        }
    }

    private fun criarOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val t = TextView(this)
        t.textSize = 12f
        t.setTextColor(Color.WHITE)
        t.setBackgroundColor(0x66000000)
        t.setPadding(20, 6, 20, 6)
        texto = t

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 8

        wm?.addView(t, params)
        handler.post(relogio)
    }

    override fun onDestroy() {
        handler.removeCallbacks(relogio)
        texto?.let { wm?.removeView(it) }
        texto = null
        super.onDestroy()
    }
}
