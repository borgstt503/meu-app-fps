package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.PointerIcon
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.File

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
    val nomesZoom = listOf("100%", "125%", "150%", "200%")

    val fontes = listOf(
        Pair("sans-serif", "Padrão"),
        Pair("sans-serif-light", "Leve"),
        Pair("sans-serif-thin", "Fina"),
        Pair("sans-serif-medium", "Média"),
        Pair("sans-serif-black", "Negrito forte"),
        Pair("sans-serif-condensed", "Condensada"),
        Pair("sans-serif-smallcaps", "Versalete"),
        Pair("serif", "Serifada"),
        Pair("monospace", "Monoespaçada"),
        Pair("serif-monospace", "Serifada mono"),
        Pair("casual", "Casual"),
        Pair("cursive", "Cursiva")
    )

    val cursores = listOf("Seta Windows (branca)", "Seta preta", "Cruz", "Ponto")

    val escalas = floatArrayOf(0.7f, 0.8f, 0.9f, 1.0f, 1.15f, 1.3f)
    val nomesEscala = listOf("70%", "80%", "90%", "100%", "115%", "130%")

    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun getI(c: Context, k: String, padrao: Int): Int {
        return prefs(c).getInt(k, padrao)
    }

    fun setI(c: Context, k: String, v: Int) {
        prefs(c).edit().putInt(k, v).apply()
    }

    fun getB(c: Context, k: String, padrao: Boolean): Boolean {
        return prefs(c).getBoolean(k, padrao)
    }

    fun setB(c: Context, k: String, v: Boolean) {
        prefs(c).edit().putBoolean(k, v).apply()
    }

    fun indiceTamanho(c: Context): Int {
        return getI(c, "jan_tam", 1).coerceIn(0, tamanhos.size - 1)
    }

    fun indiceZoom(c: Context): Int {
        return getI(c, "jan_zoom", 0).coerceIn(0, zooms.size - 1)
    }

    fun indiceFonte(c: Context): Int {
        return getI(c, "fonte", 0).coerceIn(0, fontes.size - 1)
    }

    fun indiceCursor(c: Context): Int {
        return getI(c, "cursor", 0).coerceIn(0, cursores.size - 1)
    }

    fun indiceEscala(c: Context): Int {
        return getI(c, "escala", 3).coerceIn(0, escalas.size - 1)
    }

    fun bloqueio(c: Context): Boolean {
        return getB(c, "bloq", true)
    }

    fun modoApp(c: Context): Boolean {
        return getB(c, "modo_app", false)
    }

    fun comEscala(base: Context): Context {
        val e = escalas[indiceEscala(base)]
        if (e == 1.0f) return base
        val cfg = Configuration(base.resources.configuration)
        cfg.densityDpi = (cfg.densityDpi * e).toInt()
        return base.createConfigurationContext(cfg)
    }

    fun typeface(c: Context): Typeface {
        return Typeface.create(fontes[indiceFonte(c)].first, Typeface.NORMAL)
    }

    fun aplicarFonte(v: View, tf: Typeface) {
        if (v is TextView) v.typeface = tf
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) aplicarFonte(v.getChildAt(i), tf)
        }
    }

    fun escolher(a: Activity, titulo: String, opcoes: List<String>, aoEscolher: (Int) -> Unit) {
        AlertDialog.Builder(a)
            .setTitle(titulo)
            .setItems(opcoes.toTypedArray()) { _, i -> aoEscolher(i) }
            .show()
    }

    fun hotspot(estilo: Int, t: Float): Pair<Float, Float> {
        return if (estilo <= 1) Pair(t * 0.10f, t * 0.05f) else Pair(t / 2f, t / 2f)
    }

    fun desenharCursor(cv: Canvas, estilo: Int, t: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.strokeJoin = Paint.Join.ROUND
        p.strokeCap = Paint.Cap.ROUND
        if (estilo <= 1) {
            val path = Path()
            path.moveTo(t * 0.10f, t * 0.05f)
            path.lineTo(t * 0.10f, t * 0.80f)
            path.lineTo(t * 0.28f, t * 0.64f)
            path.lineTo(t * 0.40f, t * 0.92f)
            path.lineTo(t * 0.52f, t * 0.87f)
            path.lineTo(t * 0.41f, t * 0.60f)
            path.lineTo(t * 0.66f, t * 0.60f)
            path.close()
            p.style = Paint.Style.FILL
            p.color = if (estilo == 0) Color.WHITE else Color.BLACK
            cv.drawPath(path, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = t * 0.07f
            p.color = if (estilo == 0) Color.BLACK else Color.WHITE
            cv.drawPath(path, p)
        } else if (estilo == 2) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = t * 0.16f
            p.color = Color.BLACK
            cv.drawLine(t * 0.5f, t * 0.12f, t * 0.5f, t * 0.88f, p)
            cv.drawLine(t * 0.12f, t * 0.5f, t * 0.88f, t * 0.5f, p)
            p.strokeWidth = t * 0.08f
            p.color = Color.WHITE
            cv.drawLine(t * 0.5f, t * 0.12f, t * 0.5f, t * 0.88f, p)
            cv.drawLine(t * 0.12f, t * 0.5f, t * 0.88f, t * 0.5f, p)
        } else {
            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            cv.drawCircle(t / 2f, t / 2f, t * 0.28f, p)
            p.color = Color.WHITE
            cv.drawCircle(t / 2f, t / 2f, t * 0.20f, p)
        }
    }

    fun pointerIcon(c: Context): PointerIcon {
        val t = (28 * c.resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(t, t, Bitmap.Config.ARGB_8888)
        val est = indiceCursor(c)
        desenharCursor(Canvas(bmp), est, t.toFloat())
        val h = hotspot(est, t.toFloat())
        return PointerIcon.create(bmp, h.first, h.second)
    }

    fun fundoPadrao(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#1B1B2F"), Color.parseColor("#7A1F3D"))
        )
    }

    fun fundoDrawable(c: Context): Drawable {
        val f = File(c.filesDir, "fundo.img")
        if (!f.exists()) return fundoPadrao()
        return try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > 2000 || o.outHeight / s > 2000) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val bmp = BitmapFactory.decodeFile(f.path, o2)
            if (bmp != null) BitmapDrawable(c.resources, bmp) else fundoPadrao()
        } catch (e: Throwable) {
            fundoPadrao()
        }
    }

    fun salvarFundo(c: Context, uri: Uri): Boolean {
        return try {
            c.contentResolver.openInputStream(uri)?.use { entrada ->
                File(c.filesDir, "fundo.img").outputStream().use { saida ->
                    entrada.copyTo(saida)
                }
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun apagarFundo(c: Context) {
        File(c.filesDir, "fundo.img").delete()
    }

    fun instalado(c: Context, pacote: String): Boolean {
        return try {
            c.packageManager.getApplicationInfo(pacote, 0)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun pacoteOpera(c: Context): String? {
        for (p in operas) {
            if (instalado(c, p)) return p
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
            var r = rodar("am start -n " + comp + " --windowingMode 5 -f 0x10000000 2>&1")
            if (r.contains("Error", true) || r.contains("Exception", true)) {
                r = rodar("am start-activity -n " + comp + " --windowingMode 5 --display 0 2>&1")
            }
            if (r.contains("Error", true) || r.contains("Exception", true)) {
                aviso(c, "Janela livre falhou: " + r.take(140))
                abrirNormal(c, pacote)
            }
        }.start()
    }
}
