package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
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

    private fun protegidos(): Set<String> {
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val s = HashSet<String>(prefs.getStringSet("protegidos", emptySet()) ?: emptySet())
        s.add("com.dts.freefireth")
        s.add("com.dts.freefiremax")
        s.add("moe.shizuku.privileged.api")
        s.add(packageName)
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null) s.add(ime.substringBefore("/"))
        return s
    }

    private fun limparApps() {
        if (!shizukuOk()) {
            Toast.makeText(this, "Ligue o Shizuku e autorize o app", Toast.LENGTH_LONG).show()
            return
        }
        val prot = protegidos()
        val alvos = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }
            .filter { !prot.contains(it) }
        Toast.makeText(this, "Fechando " + alvos.size + " apps...", Toast.LENGTH_SHORT).show()
        Thread {
            for (p in alvos) {
                rodar("am force-stop " + p)
            }
            rodar("am kill-all")
            runOnUiThread {
                Toast.makeText(this, alvos.size.toString() + " apps fechados", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun menuBandeja() {
        val itens = arrayOf(
            "Fechar apps (liberar RAM)",
            "Fechar janelas",
            "Trocar fundo",
            "Fundo padrao",
            "Ajustes (Modo Jogo, overlay)"
        )
        AlertDialog.Builder(this).setItems(itens) { _, qual ->
            when (qual) {
                0 -> limparApps()
                1 -> fecharJanelas()
                2 -> escolherFundo()
                3 -> fundoPadrao()
                4 -> startActivity(Intent(this, MainActivity::class.java))
            }
        }.show()
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

    private fun iconeDesktop(pacote: String, nome: String, acao: () -> Unit): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.setPadding(dp(10), dp(10), dp(10), dp(6))

        val img = ImageView(this)
        try {
            img.setImageDrawable(packageManager.getApplicationIcon(pacote))
        } catch (e: Throwable) {
            img.setBackgroundColor(Color.DKGRAY)
        }
        img.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))

        val txt = TextView(this)
        txt.text = nome
        txt.setTextColor(Color.WHITE)
        txt.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        txt.textSize = 11f
        txt.gravity = Gravity.CENTER
        txt.setPadding(0, dp(4), 0, 0)

        item.addView(img)
        item.addView(txt)
        item.setOnClickListener { acao() }
        return item
    }

    private fun iconeBarra(pacote: String, acao: () -> Unit): ImageView {
        val img = ImageView(this)
        try {
            img.setImageDrawable(packageManager.getApplicationIcon(pacote))
        } catch (e: Throwable) {
            img.setBackgroundColor(Color.DKGRAY)
        }
        img.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        img.setPadding(dp(6), dp(6), dp(6), dp(6))
        img.setOnClickListener { acao() }
        return img
    }

    private fun logoIniciar(): GridLayout {
        val logo = GridLayout(this)
        logo.rowCount = 2
        logo.columnCount = 2
        logo.setPadding(dp(12), 0, dp(12), 0)
        for (i in 0 until 4) {
            val q = View(this)
            q.setBackgroundColor(Color.parseColor("#4CC2FF"))
            val lp = GridLayout.LayoutParams()
            lp.width = dp(10)
            lp.height = dp(10)
            lp.setMargins(dp(1), dp(1), dp(1), dp(1))
            q.layoutParams = lp
            logo.addView(q)
        }
        logo.setOnClickListener { mostrarApps() }
        return logo
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B0B12")
        window.navigationBarColor = Color.parseColor("#202020")

        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        raiz = r

        val area = GridLayout(this)
        area.orientation = GridLayout.VERTICAL
        area.rowCount = 3
        area.setPadding(dp(4), dp(8), dp(4), 0)
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        area.addView(
            iconeDesktop(packageName, "Este PC") {
                startActivity(Intent(this, MainActivity::class.java))
            }
        )
        area.addView(
            iconeDesktop("com.android.chrome", "Chrome") {
                abrirJanelaWeb("Chrome", "https://www.google.com")
            }
        )
        area.addView(
            iconeDesktop("com.google.android.youtube", "YouTube") {
                abrirJanelaWeb("YouTube", "https://www.youtube.com")
            }
        )
        area.addView(
            iconeDesktop("com.zhiliaoapp.musically", "TikTok") {
                abrirJanelaWeb("TikTok", "https://www.tiktok.com")
            }
        )
        area.addView(
            iconeDesktop("com.dts.freefireth", "Free Fire") {
                abrirApp("com.dts.freefireth")
            }
        )
        area.addView(
            iconeDesktop("moe.shizuku.privileged.api", "Shizuku") {
                abrirApp("moe.shizuku.privileged.api")
            }
        )

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        val fundoBarra = GradientDrawable()
        fundoBarra.setColor(Color.parseColor("#E6202020"))
        barra.background = fundoBarra
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )

        val esquerda = View(this)
        esquerda.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)

        val centro = LinearLayout(this)
        centro.orientation = LinearLayout.HORIZONTAL
        centro.gravity = Gravity.CENTER_VERTICAL

        val pesquisa = TextView(this)
        pesquisa.text = "Pesquisar"
        pesquisa.setTextColor(Color.parseColor("#D0D0D0"))
        pesquisa.textSize = 13f
        pesquisa.gravity = Gravity.CENTER_VERTICAL
        pesquisa.setPadding(dp(16), 0, dp(16), 0)
        val fundoPesquisa = GradientDrawable()
        fundoPesquisa.setColor(Color.parseColor("#3A3A3A"))
        fundoPesquisa.cornerRadius = dp(18).toFloat()
        pesquisa.background = fundoPesquisa
        val lpPesquisa = LinearLayout.LayoutParams(dp(170), dp(34))
        lpPesquisa.leftMargin = dp(4)
        lpPesquisa.rightMargin = dp(8)
        pesquisa.layoutParams = lpPesquisa
        pesquisa.setOnClickListener { abrirJanelaWeb("Google", "https://www.google.com") }

        centro.addView(logoIniciar())
        centro.addView(pesquisa)
        centro.addView(
            iconeBarra("com.google.android.youtube") {
                abrirJanelaWeb("YouTube", "https://www.youtube.com")
            }
        )
        centro.addView(
            iconeBarra("com.android.chrome") {
                abrirJanelaWeb("Chrome", "https://www.google.com")
            }
        )
        centro.addView(
            iconeBarra("com.zhiliaoapp.musically") {
                abrirJanelaWeb("TikTok", "https://www.tiktok.com")
            }
        )
        centro.addView(
            iconeBarra("com.dts.freefireth") {
                abrirApp("com.dts.freefireth")
            }
        )
        centro.addView(
            iconeBarra("moe.shizuku.privileged.api") {
                abrirApp("moe.shizuku.privileged.api")
            }
        )

        val direita = LinearLayout(this)
        direita.orientation = LinearLayout.HORIZONTAL
        direita.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        direita.setPadding(0, 0, dp(12), 0)
        direita.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

        val bandeja = TextView(this)
        bandeja.text = "^"
        bandeja.setTextColor(Color.WHITE)
        bandeja.textSize = 20f
        bandeja.gravity = Gravity.CENTER
        bandeja.setPadding(dp(14), 0, dp(14), 0)
        bandeja.setOnClickListener { menuBandeja() }

        val relogio = LinearLayout(this)
        relogio.orientation = LinearLayout.VERTICAL
        relogio.gravity = Gravity.END

        val hora = TextClock(this)
        hora.format24Hour = "HH:mm"
        hora.format12Hour = "HH:mm"
        hora.setTextColor(Color.WHITE)
        hora.textSize = 12f
        hora.gravity = Gravity.END

        val data = TextClock(this)
        data.format24Hour = "dd-MM-yyyy"
        data.format12Hour = "dd-MM-yyyy"
        data.setTextColor(Color.WHITE)
        data.textSize = 11f
        data.gravity = Gravity.END

        relogio.addView(hora)
        relogio.addView(data)

        direita.addView(bandeja)
        direita.addView(relogio)

        barra.addView(esquerda)
        barra.addView(centro)
        barra.addView(direita)

        r.addView(area)
        r.addView(barra)
        setContentView(r)
        aplicarFundo()
    }
}
