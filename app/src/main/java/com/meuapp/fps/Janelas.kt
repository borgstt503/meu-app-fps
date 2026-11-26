package com.meuapp.fps

import android.content.Context

object Janelas {

    private var frac = -1f

    private fun garantir(c: Context) {
        if (frac < 0f) frac = Atalhos.tamanhos[Atalhos.indiceTamanho(c)]
    }

    private fun idDaFrente(): String? {
        val r = Atalhos.rodar("dumpsys activity activities | grep ResumedActivity")
        return Regex("t(\\d+)\\}").find(r)?.groupValues?.get(1)
    }

    private fun aplicar(c: Context, l: Int, t: Int, r: Int, b: Int) {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(c, "Ligue o Shizuku e autorize o app")
            return
        }
        Thread {
            val id = idDaFrente()
            if (id == null) {
                Atalhos.aviso(c, "Não achei o app da frente")
                return@Thread
            }
            val s = Atalhos.rodar(
                "am task resize " + id + " " + l + " " + t + " " + r + " " + b + " 2>&1"
            )
            if (s.contains("Error", true) || s.contains("Exception", true)) {
                Atalhos.aviso(c, "Não consegui redimensionar: " + s.take(120))
            }
        }.start()
    }

    private fun centralizado(c: Context) {
        val m = c.resources.displayMetrics
        val w = (m.widthPixels * frac).toInt()
        val h = (m.heightPixels * minOf(0.92f, frac + 0.1f)).toInt()
        val l = (m.widthPixels - w) / 2
        val t = (m.heightPixels - h) / 2
        aplicar(c, l, t, l + w, t + h)
    }

    fun mudarTamanho(c: Context, fator: Float) {
        garantir(c)
        frac = (frac * fator).coerceIn(0.3f, 0.98f)
        centralizado(c)
    }

    fun padrao(c: Context) {
        frac = Atalhos.tamanhos[Atalhos.indiceTamanho(c)]
        centralizado(c)
    }

    fun maximizar(c: Context) {
        frac = 0.98f
        val m = c.resources.displayMetrics
        aplicar(c, 0, 0, m.widthPixels, m.heightPixels)
    }

    fun metade(c: Context, direita: Boolean) {
        val m = c.resources.displayMetrics
        val meio = m.widthPixels / 2
        if (direita) {
            aplicar(c, meio, 0, m.widthPixels, m.heightPixels)
        } else {
            aplicar(c, 0, 0, meio, m.heightPixels)
        }
    }

    fun minimizar(c: Context) {
        val m = c.resources.displayMetrics
        val w = (m.widthPixels * 0.26f).toInt()
        val h = (m.heightPixels * 0.30f).toInt()
        val mg = (16 * m.density).toInt()
        val base = (64 * m.density).toInt()
        aplicar(
            c,
            m.widthPixels - w - mg,
            m.heightPixels - h - base,
            m.widthPixels - mg,
            m.heightPixels - base
        )
    }
}
