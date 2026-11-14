package com.meuapp.fps

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class DesktopActivity : Activity() {

    private val laranja = Color.parseColor("#FF8A00")

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
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

    private fun abrirTelaCheia(pacote: String) {
        val intent = packageManager.getLaunchIntentForPackage(pacote)
        if (intent == null) {
            Toast.makeText(this, "App nao encontrado: " + pacote, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private fun abrirJanela(pacote: String) {
        if (!shizukuOk()) {
            Toast.makeText(this, "Shizuku desligado, abrindo em tela cheia", Toast.LENGTH_SHORT).show()
            abrirTelaCheia(pacote)
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

    private fun criarIcone(pacote: String, nome: String, janela: Boolean): LinearLayout {
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
        txt.textSize = 12f
        txt.gravity = Gravity.CENTER
        txt.setPadding(0, dp(6), 0, 0)

        item.addView(img)
        item.addView(txt)
        item.setOnClickListener {
            if (janela) abrirJanela(pacote) else abrirTelaCheia(pacote)
        }
        return item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B0B12")
        window.navigationBarColor = Color.parseColor("#1E1E1E")

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#0B0B12"))

        val area = LinearLayout(this)
        area.orientation = LinearLayout.HORIZONTAL
        area.gravity = Gravity.TOP or Gravity.START
        area.setPadding(dp(8), dp(24), dp(8), 0)
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        area.addView(criarIcone("com.google.android.youtube", "YouTube", true))
        area.addView(criarIcone("com.google.android.googlequicksearchbox", "Google", true))
        area.addView(criarIcone("com.dts.freefireth", "Free Fire", false))

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(dp(16), 0, dp(16), 0)
        val fundo = GradientDrawable()
        fundo.setColor(Color.parseColor("#1E1E1E"))
        barra.background = fundo
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        )

        val iniciar = TextView(this)
        iniciar.text = "Sent"
        iniciar.setTextColor(laranja)
        iniciar.textSize = 18f
        iniciar.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )
        iniciar.setOnClickListener { abrirTelaCheia(packageName) }

        val relogio = TextClock(this)
        relogio.format24Hour = "HH:mm"
        relogio.format12Hour = "HH:mm"
        relogio.setTextColor(Color.WHITE)
        relogio.textSize = 16f

        barra.addView(iniciar)
        barra.addView(relogio)

        raiz.addView(area)
        raiz.addView(barra)
        setContentView(raiz)
    }
}
