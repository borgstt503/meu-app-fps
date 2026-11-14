package com.meuapp.fps

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.io.File

class DesktopActivity : Activity() {

    private val laranja = Color.parseColor("#FF8A00")
    private val pedirFundo = 7
    private var raiz: LinearLayout? = null

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun abrirApp(pacote: String) {
        val intent = packageManager.getLaunchIntentForPackage(pacote)
        if (intent == null) {
            Toast.makeText(this, "App nao encontrado: " + pacote, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private fun abrirJanelaWeb(titulo: String, url: String) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative 'Exibir sobre outros apps' e volte", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
            return
        }
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("titulo", titulo)
        i.putExtra("url", url)
        startForegroundService(i)
    }

    private fun fecharJanelas() {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", "fechar")
        startForegroundService(i)
    }

    private fun escolherFundo() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(i, pedirFundo)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pedirFundo && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    File(filesDir, "fundo.img").outputStream().use { saida ->
                        entrada.copyTo(saida)
                    }
                }
                aplicarFundo()
            } catch (e: Throwable) {
                Toast.makeText(this, "Erro ao trocar o fundo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fundoPadrao() {
        File(filesDir, "fundo.img").delete()
        aplicarFundo()
        Toast.makeText(this, "Fundo padrao", Toast.LENGTH_SHORT).show()
    }

    private fun aplicarFundo() {
        val r = raiz ?: return
        val f = File(filesDir, "fundo.img")
        if (!f.exists()) {
            r.setBackgroundColor(Color.parseColor("#0B0B12"))
            return
        }
        try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > 2000 || o.outHeight / s > 2000) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val bmp = BitmapFactory.decodeFile(f.path, o2)
            if (bmp != null) r.background = BitmapDrawable(resources, bmp)
        } catch (e: Throwable) {
            r.setBackgroundColor(Color.parseColor("#0B0B12"))
        }
    }

    private fun criarIcone(pacote: String, nome: String, acao: () -> Unit): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.setPadding(dp(16), dp(16), dp(16), dp(8))

        val img = ImageView(this)
        try {
            img.setImageDrawable(packageManager.getApplicationIcon(pacote))
        } catch (e: Throwable) {
            img.setBackgroundColor(Color.DKGRAY)
        }
        img.layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))

        val txt = TextView(this)
        txt.text = nome
        txt.setTextColor(Color.WHITE)
        txt.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        txt.textSize = 12f
        txt.gravity = Gravity.CENTER
        txt.setPadding(0, dp(6), 0, 0)

        item.addView(img)
        item.addView(txt)
        item.setOnClickListener { acao() }
        return item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B0B12")
        window.navigationBarColor = Color.parseColor("#1E1E1E")

        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        raiz = r

        val area = GridLayout(this)
        area.columnCount = 4
        area.setPadding(dp(8), dp(24), dp(8), 0)
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        area.addView(
            criarIcone("com.google.android.youtube", "YouTube") {
                abrirJanelaWeb("YouTube", "https://m.youtube.com")
            }
        )
        area.addView(
            criarIcone("com.google.android.googlequicksearchbox", "Google") {
                abrirJanelaWeb("Google", "https://www.google.com")
            }
        )
        area.addView(
            criarIcone("com.android.chrome", "Chrome") {
                abrirJanelaWeb("Chrome", "https://www.google.com")
            }
        )
        area.addView(
            criarIcone("com.zhiliaoapp.musically", "TikTok") {
                abrirJanelaWeb("TikTok", "https://www.tiktok.com")
            }
        )
        area.addView(
            criarIcone("com.dts.freefireth", "Free Fire") {
                abrirApp("com.dts.freefireth")
            }
        )

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(dp(16), 0, dp(16), 0)
        val fundo = GradientDrawable()
        fundo.setColor(Color.parseColor("#E61E1E1E"))
        barra.background = fundo
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        )

        val iniciar = TextView(this)
        iniciar.text = "Sent (ajustes)"
        iniciar.setTextColor(laranja)
        iniciar.textSize = 18f
        iniciar.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )
        iniciar.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val trocar = TextView(this)
        trocar.text = "Fundo"
        trocar.setTextColor(Color.WHITE)
        trocar.textSize = 14f
        trocar.setPadding(0, 0, dp(16), 0)
        trocar.setOnClickListener { escolherFundo() }
        trocar.setOnLongClickListener {
            fundoPadrao()
            true
        }

        val fechar = TextView(this)
        fechar.text = "Fechar janelas"
        fechar.setTextColor(Color.WHITE)
        fechar.textSize = 14f
        fechar.setPadding(0, 0, dp(16), 0)
        fechar.setOnClickListener { fecharJanelas() }

        val relogio = TextClock(this)
        relogio.format24Hour = "HH:mm"
        relogio.format12Hour = "HH:mm"
        relogio.setTextColor(Color.WHITE)
        relogio.textSize = 16f

        barra.addView(iniciar)
        barra.addView(trocar)
        barra.addView(fechar)
        barra.addView(relogio)

        r.addView(area)
        r.addView(barra)
        setContentView(r)
        aplicarFundo()
    }
}
