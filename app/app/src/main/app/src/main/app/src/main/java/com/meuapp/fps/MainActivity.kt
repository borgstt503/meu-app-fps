package com.meuapp.fps

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        info = TextView(this)
        info.textSize = 20f

        val botao = Button(this)
        botao.text = "Atualizar"
        botao.setOnClickListener { atualizar() }

        layout.addView(info)
        layout.addView(botao)
        setContentView(layout)

        atualizar()
    }

    private fun atualizar() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val livreMb = mem.availMem / (1024 * 1024)
        val totalMb = mem.totalMem / (1024 * 1024)

        val bateria = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bateria?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val nivel = bateria?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

        info.text = "RAM livre: $livreMb MB de $totalMb MB\nTemperatura: $temp °C\nBateria: $nivel%"
    }
}
