package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class NotasActivity : Activity() {

    private var campo: EditText? = null

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#202020")
        window.navigationBarColor = Color.parseColor("#202020")

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#202020"))
        raiz.setPadding(dp(12), dp(8), dp(12), dp(12))

        val topo = LinearLayout(this)
        topo.orientation = LinearLayout.HORIZONTAL
        topo.gravity = Gravity.CENTER_VERTICAL
        topo.setPadding(0, 0, 0, dp(8))

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.setTextColor(Color.WHITE)
        voltar.textSize = 26f
        voltar.setPadding(dp(4), 0, dp(16), 0)
        voltar.setOnClickListener { finish() }

        val titulo = TextView(this)
        titulo.text = "Notas"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 18f
        titulo.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        val limpar = TextView(this)
        limpar.text = "Limpar"
        limpar.setTextColor(Color.parseColor("#4CC2FF"))
        limpar.textSize = 14f
        limpar.setPadding(dp(8), dp(8), dp(8), dp(8))

        topo.addView(voltar)
        topo.addView(titulo)
        topo.addView(limpar)

        val fundo = GradientDrawable()
        fundo.setColor(Color.parseColor("#2B2B2B"))
        fundo.cornerRadius = dp(10).toFloat()

        val c = EditText(this)
        c.hint = "Escreva aqui..."
        c.setHintTextColor(Color.parseColor("#8A8A8A"))
        c.setTextColor(Color.WHITE)
        c.textSize = 16f
        c.background = fundo
        c.setPadding(dp(14), dp(12), dp(14), dp(12))
        c.gravity = Gravity.TOP or Gravity.START
        c.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        c.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        c.setText(getSharedPreferences("cfg", MODE_PRIVATE).getString("nota", ""))
        campo = c

        limpar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Limpar a nota?")
                .setPositiveButton("Limpar") { _, _ -> c.setText("") }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        raiz.addView(topo)
        raiz.addView(c)
        setContentView(raiz)
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("cfg", MODE_PRIVATE)
            .edit()
            .putString("nota", campo?.text?.toString() ?: "")
            .apply()
    }
}
