package com.meuapp.fps

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class DesktopActivity : Activity() {

    private val laranja = Color.parseColor("#FF8A00")

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
            criarIcone("com.dts.freefireth", "Free Fire") {
                abrirApp("com.dts.freefireth")
            }
        )

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
        iniciar.setOnClickListener { abrirApp(packageName) }

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
        barra.addView(fechar)
        barra.addView(relogio)

        raiz.addView(area)
        raiz.addView(barra)
        setContentView(raiz)
    }
}
