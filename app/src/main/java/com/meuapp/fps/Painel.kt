package com.meuapp.fps

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object Painel {

    private class Tile(
        val caixa: LinearLayout,
        val titulo: TextView,
        val sub: TextView,
        var on: Boolean
    )

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun arred(c: Context, cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(c, raio).toFloat()
        return g
    }

    private fun ligado(s: String): Boolean {
        val t = s.trim()
        return t.isNotEmpty() && t != "0" && t != "null" && !t.startsWith("Erro")
    }

    private fun pintar(c: Context, t: Tile) {
        val fundo = if (t.on) Color.parseColor("#4CC2FF") else Color.parseColor("#3A3A3A")
        t.caixa.background = arred(c, fundo, 10)
        t.titulo.setTextColor(if (t.on) Color.BLACK else Color.WHITE)
        t.sub.setTextColor(
            if (t.on) Color.parseColor("#0F3347") else Color.parseColor("#A8A8A8")
        )
    }

    private fun criarTile(
        c: Context,
        nome: String,
        sub: String,
        longa: (() -> Unit)?,
        acao: (Tile) -> Unit
    ): Tile {
        val caixa = LinearLayout(c)
        caixa.orientation = LinearLayout.VERTICAL
        caixa.gravity = Gravity.CENTER_VERTICAL
        caixa.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6))

        val t1 = TextView(c)
        t1.text = nome
        t1.textSize = 12f
        t1.maxLines = 1

        val t2 = TextView(c)
        t2.text = sub
        t2.textSize = 10f
        t2.maxLines = 1
        t2.ellipsize = TextUtils.TruncateAt.END

        caixa.addView(t1)
        caixa.addView(t2)

        val lp = GridLayout.LayoutParams()
        lp.width = dp(c, 104)
        lp.height = dp(c, 54)
        lp.setMargins(dp(c, 3), dp(c, 3), dp(c, 3), dp(c, 3))
        caixa.layoutParams = lp

        val tile = Tile(caixa, t1, t2, false)
        pintar(c, tile)
        caixa.setOnClickListener { acao(tile) }
        if (longa != null) {
            caixa.setOnLongClickListener {
                longa()
                true
            }
        }
        return tile
    }

    private fun alternar(c: Context, t: Tile, ligar: String, desligar: String) {
        if (!Atalhos.shizukuOk()) {
            Atalhos.aviso(c, "Ligue o Shizuku e autorize o app")
            return
        }
        t.on = !t.on
        t.sub.text = if (t.on) "Ligado" else "Desligado"
        pintar(c, t)
        val cmd = if (t.on) ligar else desligar
        Thread { Atalhos.rodar(cmd) }.start()
    }

    private fun ssid(): String {
        val s = Atalhos.rodar("dumpsys wifi | grep -m1 mWifiInfo")
        val m = Regex("SSID: \"?([^\",]+)\"?,").find(s)
        val n = m?.groupValues?.get(1)
        return if (n == null || n.contains("unknown")) "Ligado" else n
    }

    private fun btLigado(c: Context): Boolean {
        return try {
            val m = c.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            m.adapter?.isEnabled == true
        } catch (e: Throwable) {
            false
        }
    }

    private fun dispositivoBt(c: Context): String {
        var texto = "Desligado"
        try {
            val m = c.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val a = m.adapter
            if (a != null && a.isEnabled) {
                val nomes = ArrayList<String>()
                for (d in a.bondedDevices) {
                    val conectado = try {
                        BluetoothDevice::class.java.getMethod("isConnected").invoke(d) as Boolean
                    } catch (e: Throwable) {
                        false
                    }
                    if (conectado) nomes.add(d.name ?: "Dispositivo")
                }
                texto = if (nomes.isEmpty()) "Ligado" else nomes.joinToString(", ")
            }
        } catch (e: Throwable) {
            texto = "Ligado"
        }
        return texto
    }

    private fun abrirTela(c: Context, i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            c.startActivity(i)
        } catch (e: Throwable) {
            Atalhos.aviso(c, "Não foi possível abrir")
        }
    }

    private fun controle(
        c: Context,
        nome: String,
        max: Int,
        atual: Int,
        aoVivo: Boolean,
        aplicar: (Int) -> Unit
    ): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(c, 8), dp(c, 10), dp(c, 8), 0)

        val t = TextView(c)
        t.setTextColor(Color.parseColor("#D0D0D0"))
        t.textSize = 12f

        fun texto(p: Int) {
            t.text = nome + "  " + (p * 100 / max) + "%"
        }
        texto(atual)

        val s = SeekBar(c)
        s.max = max
        s.progress = atual
        s.progressTintList = ColorStateList.valueOf(Color.parseColor("#4CC2FF"))
        s.thumbTintList = ColorStateList.valueOf(Color.parseColor("#4CC2FF"))
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {
                texto(p)
                if (aoVivo && user) aplicar(p)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (!aoVivo) aplicar(sb?.progress ?: 0)
            }
        })

        l.addView(t)
        l.addView(s)
        return l
    }

    fun build(c: Context, fechar: () -> Unit): View {
        val raiz = LinearLayout(c)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.background = arred(c, Color.parseColor("#F2262222"), 14)
        raiz.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10))

        val grade = GridLayout(c)
        grade.columnCount = 3

        val wifi = criarTile(c, "Wi-Fi", "...", {
            fechar()
            abrirTela(c, Intent(Settings.ACTION_WIFI_SETTINGS))
        }) { t -> alternar(c, t, "svc wifi enable", "svc wifi disable") }

        val bt = criarTile(c, "Bluetooth", "...", {
            fechar()
            abrirTela(c, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }) { t -> alternar(c, t, "svc bluetooth enable", "svc bluetooth disable") }

        val zen = criarTile(c, "Não perturbe", "...", null) { t ->
            alternar(
                c, t,
                "settings put global zen_mode 1",
                "settings put global zen_mode 0"
            )
        }

        val dns = criarTile(c, "Sem anúncios", "...", null) { t ->
            alternar(
                c, t,
                "settings put global private_dns_specifier dns.adguard-dns.com; " +
                    "settings put global private_dns_mode hostname",
                "settings put global private_dns_mode opportunistic"
            )
        }

        val camera = criarTile(c, "Câmera", "Abrir", null) {
            fechar()
            abrirTela(c, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }

        val ajustes = criarTile(c, "Ajustes", "Sistema", null) {
            fechar()
            abrirTela(c, Intent(Settings.ACTION_SETTINGS))
        }

        grade.addView(wifi.caixa)
        grade.addView(bt.caixa)
        grade.addView(zen.caixa)
        grade.addView(dns.caixa)
        grade.addView(camera.caixa)
        grade.addView(ajustes.caixa)

        val brilhoAtual = Settings.System.getInt(
            c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128
        )
        val brilho = controle(c, "Brilho", 255, brilhoAtual, false) { v ->
            if (!Atalhos.shizukuOk()) {
                Atalhos.aviso(c, "Ligue o Shizuku e autorize o app")
            } else {
                Thread {
                    Atalhos.rodar(
                        "settings put system screen_brightness_mode 0; " +
                            "settings put system screen_brightness " + maxOf(v, 5)
                    )
                }.start()
            }
        }

        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = controle(
            c, "Volume", maxV, am.getStreamVolume(AudioManager.STREAM_MUSIC), true
        ) { v ->
            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
            } catch (e: Throwable) {
            }
        }

        val bat = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val nivel = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val rodape = TextView(c)
        rodape.text = "Bateria  " + nivel + "%"
        rodape.setTextColor(Color.parseColor("#A8A8A8"))
        rodape.textSize = 12f
        rodape.setPadding(dp(c, 8), dp(c, 12), 0, 0)

        raiz.addView(grade)
        raiz.addView(brilho)
        raiz.addView(volume)
        raiz.addView(rodape)

        Thread {
            var wOn = false
            var zOn = false
            var dOn = false
            var nomeWifi = "Shizuku desligado"
            val shizuku = Atalhos.shizukuOk()
            if (shizuku) {
                wOn = ligado(Atalhos.rodar("settings get global wifi_on"))
                zOn = ligado(Atalhos.rodar("settings get global zen_mode"))
                dOn = Atalhos.rodar("settings get global private_dns_mode").trim() == "hostname"
                nomeWifi = if (wOn) ssid() else "Desligado"
            }
            val bOn = btLigado(c)
            val nomeBt = dispositivoBt(c)
            Handler(Looper.getMainLooper()).post {
                wifi.on = wOn
                wifi.sub.text = nomeWifi
                bt.on = bOn
                bt.sub.text = nomeBt
                zen.on = zOn
                zen.sub.text = if (zOn) "Ligado" else "Desligado"
                dns.on = dOn
                dns.sub.text = if (dOn) "Ligado" else "Desligado"
                pintar(c, wifi)
                pintar(c, bt)
                pintar(c, zen)
                pintar(c, dns)
            }
        }.start()

        return raiz
    }
}
