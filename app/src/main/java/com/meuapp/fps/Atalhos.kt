package com.meuapp.fps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import rikka.shizuku.Shizuku

object Atalhos {

    val sites = mapOf(
        "com.google.android.youtube" to Pair("YouTube", "https://www.youtube.com"),
        "com.google.android.googlequicksearchbox" to Pair("Google", "https://www.google.com"),
        "com.android.chrome" to Pair("Chrome", "https://www.google.com"),
        "com.zhiliaoapp.musically" to Pair("TikTok", "https://www.tiktok.com")
    )

    private val operas = listOf(
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.browser.beta",
        "com.opera.gx"
    )

    val tamanhos = floatArrayOf(0.4f, 0.6f, 0.8f, 0.95f)
    val nomesTamanho = listOf("Pequena", "Média", "Grande", "Quase cheia")
    val zooms = intArrayOf(100, 125, 150, 200)

    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun indiceTamanho(c: Context): Int {
        return prefs(c).getInt("jan_tam", 1).coerceIn(0, 3)
    }

    fun indiceZoom(c: Context): Int {
        return prefs(c).getInt("jan_zoom", 0).coerceIn(0, 3)
    }

    fun trocarTamanho(c: Context): Int {
        val n = (indiceTamanho(c) + 1) % 4
        prefs(c).edit().putInt("jan_tam", n).apply()
        return n
    }

    fun trocarZoom(c: Context): Int {
        val n = (indiceZoom(c) + 1) % 4
        prefs(c).edit().putInt("jan_zoom", n).apply()
        return n
    }

    fun pacoteOpera(c: Context): String? {
        for (p in operas) {
            try {
                c.packageManager.getApplicationInfo(p, 0)
                return p
            } catch (e: Throwable) {
            }
        }
        return null
    }

    fun nomeDe(c: Context, pacote: String): String {
        return try {
            c.packageManager.getApplicationLabel(
                c.packageManager.getApplicationInfo(pacote, 0)
            ).toString()
        } catch (e: Throwable) {
            pacote
        }
    }

    fun shizukuOk(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun rodar(comando: String): String {
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

    fun aviso(c: Context, msg: String) {
        val app = c.applicationContext
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun abrirNormal(c: Context, pacote: String) {
        val i = c.packageManager.getLaunchIntentForPackage(pacote) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            c.startActivity(i)
        } catch (e: Throwable) {
        }
    }

    fun abrirJanela(c: Context, pacote: String) {
        if (!shizukuOk()) {
            aviso(c, "Shizuku desligado: abrindo em tela cheia")
            abrirNormal(c, pacote)
            return
        }
        val comp = c.packageManager.getLaunchIntentForPackage(pacote)
            ?.component?.flattenToShortString()
        if (comp == null) {
            abrirNormal(c, pacote)
            return
        }
        Thread {
            val r = rodar("am start -n " + comp + " --windowingMode 5 -f 0x10000000 2>&1")
            if (r.contains("Error", true) || r.contains("Exception", true)) {
                aviso(c, "Janela livre falhou: " + r.take(140))
                abrirNormal(c, pacote)
            }
        }.start()
    }
}
