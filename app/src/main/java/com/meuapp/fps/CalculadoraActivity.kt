package com.meuapp.fps

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class CalculadoraActivity : Activity() {

    private var visor: TextView? = null
    private val expr = StringBuilder()

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun arred(cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(raio).toFloat()
        return g
    }

    private fun avaliar(e: String): Double? {
        val nums = ArrayList<Double>()
        val ops = ArrayList<Char>()
        val atual = StringBuilder()
        for (c in e) {
            if (c.isDigit() || c == '.') {
                atual.append(c)
            } else if (c == '-' && atual.isEmpty()) {
                atual.append(c)
            } else {
                val n = atual.toString().toDoubleOrNull() ?: return null
                nums.add(n)
                atual.setLength(0)
                ops.add(c)
            }
        }
        val ultimo = atual.toString().toDoubleOrNull() ?: return null
        nums.add(ultimo)

        val n2 = ArrayList<Double>()
        val o2 = ArrayList<Char>()
        n2.add(nums[0])
        for (i in ops.indices) {
            val op = ops[i]
            val prox = nums[i + 1]
            if (op == '×') {
                n2[n2.size - 1] = n2[n2.size - 1] * prox
            } else if (op == '÷') {
                if (prox == 0.0) return null
                n2[n2.size - 1] = n2[n2.size - 1] / prox
            } else {
                o2.add(op)
                n2.add(prox)
            }
        }
        var r = n2[0]
        for (i in o2.indices) {
            r = if (o2[i] == '+') r + n2[i + 1] else r - n2[i + 1]
        }
        return r
    }

    private fun atualizarVisor() {
        visor?.text = if (expr.isEmpty()) "0" else expr.toString()
    }

    private fun tecla(txt: String) {
        if (txt == "C") {
            expr.setLength(0)
        } else if (txt == "⌫") {
            if (expr.isNotEmpty()) expr.deleteCharAt(expr.length - 1)
        } else if (txt == "=") {
            val r = avaliar(expr.toString())
            if (r == null || r.isNaN() || r.isInfinite()) {
                visor?.text = "Erro"
                expr.setLength(0)
                return
            }
            val s = BigDecimal(r).setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
            expr.setLength(0)
            expr.append(s)
        } else {
            expr.append(txt)
        }
        atualizarVisor()
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

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.setTextColor(Color.WHITE)
        voltar.textSize = 26f
        voltar.setPadding(dp(4), 0, dp(16), 0)
        voltar.setOnClickListener { finish() }

        val titulo = TextView(this)
        titulo.text = "Calculadora"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 18f

        topo.addView(voltar)
        topo.addView(titulo)

        val v = TextView(this)
        v.text = "0"
        v.setTextColor(Color.WHITE)
        v.textSize = 34f
        v.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        v.maxLines = 1
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)
        )
        visor = v

        raiz.addView(topo)
        raiz.addView(v)

        val linhas = listOf(
            listOf("C", "⌫", "÷", "×"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "="),
            listOf("0", ".")
        )

        for (linha in linhas) {
            val l = LinearLayout(this)
            l.orientation = LinearLayout.HORIZONTAL
            l.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            for (i in linha.indices) {
                val txt = linha[i]
                val b = TextView(this)
                b.text = txt
                b.textSize = 22f
                b.gravity = Gravity.CENTER
                val igual = txt == "="
                b.setTextColor(if (igual) Color.BLACK else Color.WHITE)
                b.background = arred(
                    if (igual) Color.parseColor("#4CC2FF") else Color.parseColor("#2B2B2B"), 10
                )
                val peso = if (linha.size == 2 && i == 0) 3f else 1f
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, peso)
                lp.setMargins(dp(3), dp(3), dp(3), dp(3))
                b.layoutParams = lp
                b.setOnClickListener { tecla(txt) }
                l.addView(b)
            }
            raiz.addView(l)
        }

        setContentView(raiz)
    }
}
