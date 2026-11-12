package com.meuapp.fps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var texto: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val formato = SimpleDateFormat("HH:mm", Locale.getDefault())

    @Volatile private var fpsTexto = "--"
    @Volatile private var ativo = false
    private var camada: String? = null
    private var ultimaBusca = 0L

    private val pacotes = listOf("com.dts.freefireth", "com.dts.freefiremax")

    private val relogio = object : Runnable {
        override fun run() {
            texto?.text = "FPS " + fpsTexto + "   " + formato.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarNotificacao()
        if (texto == null) criarOverlay()
        iniciarMedicao()
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

    private fun iniciarMedicao() {
        if (ativo) return
        ativo = true
        Thread {
            while (ativo) {
                try {
                    fpsTexto = medir()
                } catch (e: Throwable) {
                    fpsTexto = "erro"
                }
                try {
                    Thread.sleep(1000)
                } catch (e: Throwable) {
                }
            }
        }.start()
    }

    private fun rodar(comando: String): String {
        return try {
            val metodo = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            metodo.isAccessible = true
            val proc = metodo.invoke(null, arrayOf("sh", "-c", comando), null, null) as Process
            val saida = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            saida
        } catch (e: Throwable) {
            ""
        }
    }

    private fun medir(): String {
        val rodando = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
        if (!rodando) return "(Shizuku off)"

        val autorizado = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
        if (!autorizado) return "(autorizar)"

        val agora = System.currentTimeMillis()
        if (camada == null) {
            if (agora - ultimaBusca < 3000) return "?"
            ultimaBusca = agora
            camada = acharCamada()
            if (camada == null) return "?"
        }

        val saida = rodar("dumpsys SurfaceFlinger --latency '" + camada + "'")
        val fps = calcular(saida)
        if (fps < 0) {
            camada = null
            return "?"
        }
        return fps.toString()
    }

    private fun acharCamada(): String? {
        val linhas = rodar("dumpsys SurfaceFlinger --list").lines()
        for (l in linhas) {
            val ehJogo = pacotes.any { l.contains(it) }
            val lixo = l.contains("Background") || l.contains("Bounds")
            if (ehJogo && l.contains("SurfaceView") && !lixo) return l.trim()
        }
        for (l in linhas) {
            val ehJogo = pacotes.any { l.contains(it) }
            val lixo = l.contains("Background") || l.contains("Bounds")
            if (ehJogo && !lixo) return l.trim()
        }
        return null
    }

    private fun calcular(saida: String): Int {
        val tempos = ArrayList<Long>()
        val linhas = saida.lines()
        for (i in 1 until linhas.size) {
            val p = linhas[i].trim().split(Regex("\\s+"))
            if (p.size >= 3) {
                val t = p[1].toLongOrNull() ?: continue
                if (t > 0L && t != Long.MAX_VALUE) tempos.add(t)
            }
        }
        if (tempos.size < 2) return -1
        val ultimo = tempos.maxOrNull() ?: return -1
        if (System.nanoTime() - ultimo > 2000000000L) return 0

        val janela = tempos.filter { it > ultimo - 1000000000L }.sorted()
        if (janela.size < 2) return 0
        val span = janela.last() - janela.first()
        if (span <= 0L) return 0
        return Math.round((janela.size - 1) * 1000000000.0 / span).toInt()
    }

    override fun onDestroy() {
        ativo = false
        handler.removeCallbacks(relogio)
        texto?.let { wm?.removeView(it) }
        texto = null
        super.onDestroy()
    }
}
