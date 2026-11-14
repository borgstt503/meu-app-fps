package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AtalhosActivity : Activity() {

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun prefs() = getSharedPreferences("cfg", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#202020")
        window.navigationBarColor = Color.parseColor("#202020")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#202020"))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(24))
        scroll.addView(col)

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
        titulo.text = "Atalhos do F11"
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 20f

        topo.addView(voltar)
        topo.addView(titulo)
        col.addView(topo)

        val dica = TextView(this)
        dica.text = "Marque os apps que aparecem na barra do F11. " +
            "Se não marcar nenhum, aparecem Opera, Chrome, YouTube e TikTok."
        dica.setTextColor(Color.parseColor("#A8A8A8"))
        dica.textSize = 13f
        dica.setPadding(dp(4), 0, 0, dp(10))
        col.addView(dica)

        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(principal, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val salvos = HashSet<String>(prefs().getStringSet("f11", emptySet()) ?: emptySet())

        for (ri in apps) {
            val pacote = ri.activityInfo.packageName
            val cb = CheckBox(this)
            cb.text = ri.loadLabel(packageManager).toString()
            cb.setTextColor(Color.WHITE)
            cb.buttonTintList = ColorStateList.valueOf(Color.parseColor("#4CC2FF"))
            cb.isChecked = salvos.contains(pacote)
            cb.setOnCheckedChangeListener { _, marcado ->
                val atual = HashSet<String>(prefs().getStringSet("f11", emptySet()) ?: emptySet())
                if (marcado) atual.add(pacote) else atual.remove(pacote)
                prefs().edit().putStringSet("f11", atual).apply()
            }
            col.addView(cb)
        }

        setContentView(scroll)
    }
}
