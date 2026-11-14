package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
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

    private fun shizukuOk(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
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

    private fun abrirComoJanela(pacote: String) {
        if (!shizukuOk()) {
            Toast.makeText(this, "Shizuku desligado, abrindo em tela cheia", Toast.LENGTH_SHORT).show()
            abrirApp(pacote)
            return
        }
        Thread {
            val r = rodar(
                "am start -p " + pacote +
                " --windowingMode 5 -a android.intent.action.MAIN" +
                " -c android.intent.category.LAUNCHER 2>&1"
            )
            if (r.isNotBlank()) {
                runOnUiThread {
                    Toast.makeText(this, r.take(150), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
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

    private fun mostrarApps() {
        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(principal, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        lista.setPadding(dp(12), dp(12), dp(12), dp(12))

        val dica = TextView(this)
        dica.text = "Toque: abrir normal. Segure: tentar abrir em janela."
        dica.setTextColor(Color.DKGRAY)
        dica.textSize = 12f
        dica.setPadding(0, 0, 0, dp(8))
        lista.addView(dica)

        val scroll = ScrollView(this)
        scroll.addView(lista)
        val dialogo = AlertDialog.Builder(this).setView(scroll).create()

        for (ri in apps) {
            val pacote = ri.activityInfo.packageName
            val linha = LinearLayout(this)
            linha.orientation = LinearLayout.HORIZONTAL
            linha.gravity = Gravity.CENTER_VERTICAL
            linha.setPadding(0, dp(8), 0, dp(8))

            val img = ImageView(this)
            img.setImageDrawable(ri.loadIcon(packageManager))
            img.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))

            val nome = TextView(this)
            nome.text = ri.loadLabel(packageManager).toString()
            nome.textSize = 16f
            nome.setPadding(dp(16), 0, 0, 0)

            linha.addView(img)
            linha.addView(nome)
            linha.setOnClickListener {
                dialogo.dismiss()
                abrirApp(pacote)
            }
            linha.setOnLongClickListener {
                dialogo.dismiss()
                abrirComoJanela(pacote)
                true
            }
            lista.addView(linha)
        }
        dialogo.show()
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

    private fun botaoBarra(texto: String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.WHITE)
        t.textSize = 14f
        t.setPadding(0, 0, dp(16), 0)
        t.setOnClickListener { acao() }
        return t
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
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
        iniciar.text = "Apps"
        iniciar.setTextColor(laranja)
        iniciar.textSize = 18f
        iniciar.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )
        iniciar.setOnClickListener { mostrarApps() }

        val relogio = TextClock(this)
        relogio.format24Hour = "HH:mm"
        relogio.format12Hour = "HH:mm"
        relogio.setTextColor(Color.WHITE)
        relogio.textSize = 16f

        barra.addView(iniciar)
        barra.addView(botaoBarra("Ajustes") {
            startActivity(Intent(this, MainActivity::class.java))
        })
        val fundoBtn = botaoBarra("Fundo") { escolherFundo() }
        fundoBtn.setOnLongClickListener {
            fundoPadrao()
            true
        }
        barra.addView(fundoBtn)
        barra.addView(botaoBarra("Fechar janelas") { fecharJanelas() })
        barra.addView(relogio)

        r.addView(area)
        r.addView(barra)
        setContentView(r)
        aplicarFundo()
    }
}
