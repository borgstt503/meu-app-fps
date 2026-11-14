package com.meuapp.fps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class PainelActivity : Activity() {

    private val azul = Color.parseColor("#4CC2FF")
    private val ativos = HashMap<String, Boolean>()
    private val blocos = HashMap<String, TextView>()

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun arred(cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(raio).toFloat()
        return g
    }

    private fun aviso(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun pintar(nome: String) {
        val b = blocos[nome] ?: return
        val on = ativos[nome] == true
        b.background = arred(if (on) azul else Color.parseColor("#3A3A3A"), 10)
        b.setTextColor(if (on) Color.BLACK else Color.WHITE)
    }

    private fun ligado(s: String): Boolean {
        return s.isNotBlank() && s != "0" && s != "null" && !s.startsWith("Erro")
    }

    private fun lerEstados() {
        if (!Atalhos.shizukuOk()) return
        Thread {
            val wifi = Atalhos.rodar("settings get global wifi_on").trim()
            val bt = Atalhos.rodar("settings get global bluetooth_on").trim()
            val zen = Atalhos.rodar("settings get global zen_mode").trim()
            val dns = Atalhos.rodar("settings get global private_dns_mode").trim()
            runOnUiThread {
                ativos["Wi-Fi"] = ligado(wifi)
                ativos["Bluetooth"] = ligado(bt)
                ativos["Não perturbe"] = ligado(zen)
                ativos["Sem anúncios"] = dns == "hostname"
                for (n in blocos.keys) pintar(n)
            }
        }.start()
    }

    private fun alternar(nome: String, ligar: String, desligar: String) {
        if (!Atalhos.shizukuOk()) {
            aviso("Ligue o Shizuku e autorize o app")
            return
        }
        val novo = ativos[nome] != true
        ativos[nome] = novo
        pintar(nome)
        Thread { Atalhos.rodar(if (novo) ligar else desligar) }.start()
    }

    private fun bloco(nome: String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = nome
        t.textSize = 12f
        t.gravity = Gravity.CENTER
        t.setPadding(dp(4), 0, dp(4), 0)
        val lp = GridLayout.LayoutParams()
        lp.width = dp(96)
        lp.height = dp(50)
        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
        t.layoutParams = lp
        t.setOnClickListener { acao() }
        blocos[nome] = t
        ativos[nome] = false
        pintar(nome)
        return t
    }

    private fun controle(
        titulo: String,
        max: Int,
        atual: Int,
        mudou: (Int, Boolean) -> Unit,
        fim: (Int) -> Unit
    ): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(8), dp(8), dp(8), 0)
        val t = TextView(this)
        t.text = titulo
        t.setTextColor(Color.parseColor("#D0D0D0"))
        t.textSize = 12f
        val s = SeekBar(this)
        s.max = max
        s.progress = atual
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {
                mudou(p, user)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                fim(sb?.progress ?: 0)
            }
        })
        l.addView(t)
        l.addView(s)
        return l
    }

    private fun camera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            finish()
        } catch (e: Throwable) {
            aviso("Câmera não encontrada")
        }
    }

    private fun ajustes() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            finish()
        } catch (e: Throwable) {
            aviso("Não foi possível abrir")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val grade = GridLayout(this)
        grade.columnCount = 3
        grade.addView(bloco("Wi-Fi") {
            alternar("Wi-Fi", "svc wifi enable", "svc wifi disable")
        })
        grade.addView(bloco("Bluetooth") {
            alternar("Bluetooth", "svc bluetooth enable", "svc bluetooth disable")
        })
        grade.addView(bloco("Não perturbe") {
            alternar(
                "Não perturbe",
                "settings put global zen_mode 1",
                "settings put global zen_mode 0"
            )
        })
        grade.addView(bloco("Sem anúncios") {
            alternar(
                "Sem anúncios",
                "settings put global private_dns_specifier dns.adguard-dns.com; " +
                    "settings put global private_dns_mode hostname",
                "settings put global private_dns_mode opportunistic"
            )
        })
        grade.addView(bloco("Câmera") { camera() })
        grade.addView(bloco("Ajustes") { ajustes() })

        val brilhoAtual = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val brilho = controle("Brilho", 255, brilhoAtual, { _, _ -> }, { v ->
            if (!Atalhos.shizukuOk()) {
                aviso("Ligue o Shizuku e autorize o app")
            } else {
                Thread {
                    Atalhos.rodar(
                        "settings put system screen_brightness_mode 0; " +
                            "settings put system screen_brightness " + maxOf(v, 5)
                    )
                }.start()
            }
        })

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = controle(
            "Volume",
            maxV,
            am.getStreamVolume(AudioManager.STREAM_MUSIC),
            { p, user ->
                if (user) {
                    try {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
                    } catch (e: Throwable) {
                    }
                }
            },
            {}
        )

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), dp(12), dp(12), dp(12))
        col.addView(grade)
        col.addView(brilho)
        col.addView(volume)

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)

        window.setBackgroundDrawable(arred(Color.parseColor("#F2262222"), 14))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setGravity(Gravity.BOTTOM or Gravity.END)
        window.setLayout(
            minOf(dp(344), resources.displayMetrics.widthPixels - dp(16)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val p = window.attributes
        p.y = dp(52)
        p.x = dp(8)
        window.attributes = p
        setFinishOnTouchOutside(true)

        lerEstados()
    }
}
