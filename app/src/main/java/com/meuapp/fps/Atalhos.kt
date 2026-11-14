package com.meuapp.fps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            abrirNormal(c, pacote)
            return
        }
        Thread {
            rodar(
                "am start -p " + pacote +
                " --windowingMode 5 -a android.intent.action.MAIN" +
                " -c android.intent.category.LAUNCHER"
            )
        }.start()
    }
}
