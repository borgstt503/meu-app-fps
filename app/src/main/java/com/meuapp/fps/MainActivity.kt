package com.meuapp.fps

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private var info: TextView? = null
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(48, 96, 48, 48)

            val t1 = TextView(this)
            t1.textSize = 20f
            info = t1

            val t2 = TextView(this)
            t2.textSize = 16f
            t2.setPadding(0, 32, 0, 32)
            status = t2

            val b1 = Button(this)
            b1.text = "Modo Jogo"
            b1.setOnClickListener { modoJogo() }

            val b2 = Button(this)
            b2.text = "Restaurar"
            b2.setOnClickListener { restaurar() }

            val b3 = Button(this)
            b3.text = "Atualizar"
            b3.setOnClickListener { atualizar() }

            layout.addView(t1)
            layout.addView(t2)
            layout.addView(b1)
            layout.addView(b2)
            layout.addView(b3)
            setContentView(layout)

            Shizuku.addRequestPermissionResultListener { _, _ ->
                runOnUiThread { atualizar() }
            }

            atualizar()
        } catch (e: Throwable) {
            val erro = TextView(this)
            erro.text = "Erro: " + e.toString()
            setContentView(erro)
        }
    }

    private fun shizukuRodando(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    private fun shizukuAutorizado(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    private fun garantirShizuku(): Boolean {
        if (!shizukuRodando()) {
            status?.text = "Shizuku nao esta rodando. Abra o Shizuku e toque em Iniciar."
            return false
        }
        if (!shizukuAutorizado()) {
            status?.text = "Autorize o app na janelinha do Shizuku e toque no botao de novo."
            try {
                Shizuku.requestPermission(1)
            } catch (e: Throwable) {
                status?.text = "Erro: " + e.toString()
            }
            return false
        }
        return true
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
            "Erro: " + e.toString()
        }
    }

    private fun modoJogo() {
        if (!garantirShizuku()) return
        status?.text = "Ativando..."
        Thread {
            val r = rodar(
                "settings put global window_animation_scale 0.5; " +
                "settings put global transition_animation_scale 0.5; " +
                "settings put global animator_duration_scale 0.5; " +
                "am kill-all"
            )
            runOnUiThread {
                if (r.startsWith("Erro")) {
                    status?.text = r
                } else {
                    status?.text = "Modo Jogo ativado. Lembre: gráficos Suave e FPS Normal dentro do Free Fire."
                    atualizar()
                    abrirJogo()
                }
            }
        }.start()
    }

    private fun restaurar() {
        if (!garantirShizuku()) return
        Thread {
            val r = rodar(
                "settings put global window_animation_scale 1.0; " +
                "settings put global transition_animation_scale 1.0; " +
                "settings put global animator_duration_scale 1.0"
            )
            runOnUiThread {
                status?.text = if (r.startsWith("Erro")) r else "Animações restauradas."
            }
        }.start()
    }

    private fun abrirJogo() {
        val pacotes = listOf("com.dts.freefireth", "com.dts.freefiremax")
        for (p in pacotes) {
            val intent = packageManager.getLaunchIntentForPackage(p)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        status?.text = "Free Fire nao encontrado neste celular."
    }

    private fun atualizar() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mem = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mem)
            val livreMb = mem.availMem / (1024 * 1024)
            val totalMb = mem.totalMem / (1024 * 1024)

            val bateria = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (bateria?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            val nivel = bateria?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

            val sh = if (!shizukuRodando()) "desligado" else if (shizukuAutorizado()) "pronto" else "falta autorizar"

            info?.text = "RAM livre: $livreMb MB de $totalMb MB\nTemperatura: $temp °C\nBateria: $nivel%\nShizuku: $sh"
        } catch (e: Throwable) {
            info?.text = "Erro: " + e.toString()
        }
    }
}
