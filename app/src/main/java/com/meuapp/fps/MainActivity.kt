package com.meuapp.fps

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private val pedirFundo = 11
    private var direita: LinearLayout? = null
    private val itensMenu = ArrayList<TextView>()
    private var listaAberta = false

    private val corFundo = Color.parseColor("#202020")
    private val corCartao = Color.parseColor("#2B2B2B")
    private val suave = Color.parseColor("#A8A8A8")
    private val azul = Color.parseColor("#4CC2FF")

    private val fixos = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "moe.shizuku.privileged.api"
    )

    private val tf by lazy { Atalhos.typeface(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Atalhos.comEscala(newBase))
    }

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
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun cartao(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = arred(corCartao, 8)
        c.setPadding(dp(16), dp(12), dp(16), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(6)
        c.layoutParams = lp
        return c
    }

    private fun linha(titulo: String, sub: String, acao: () -> Unit): LinearLayout {
        val c = cartao()
        val t = TextView(this)
        t.text = titulo
        t.setTextColor(Color.WHITE)
        t.textSize = 15f
        val s = TextView(this)
        s.text = sub
        s.setTextColor(suave)
        s.textSize = 12f
        c.addView(t)
        c.addView(s)
        c.setOnClickListener { acao() }
        return c
    }

    private fun titulo(texto: String): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.WHITE)
        t.textSize = 22f
        t.setPadding(dp(4), 0, 0, dp(10))
        return t
    }

    private fun cabecalho(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.HORIZONTAL
        c.gravity = Gravity.CENTER_VERTICAL
        c.background = arred(corCartao, 10)
        c.setPadding(dp(14), dp(14), dp(14), dp(14))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(10)
        c.layoutParams = lp

        val thumb = ImageView(this)
        thumb.setImageDrawable(Atalhos.fundoDrawable(this))
        thumb.scaleType = ImageView.ScaleType.CENTER_CROP
        thumb.background = arred(Color.BLACK, 10)
        thumb.outlineProvider = ViewOutlineProvider.BACKGROUND
        thumb.clipToOutline = true
        thumb.layoutParams = LinearLayout.LayoutParams(dp(120), dp(72))

        val txt = LinearLayout(this)
        txt.orientation = LinearLayout.VERTICAL
        txt.setPadding(dp(16), 0, 0, 0)

        val t1 = TextView(this)
        t1.text = "Este PC"
        t1.setTextColor(Color.WHITE)
        t1.textSize = 18f

        val t2 = TextView(this)
        t2.text = "Telefone - " + Build.MANUFACTURER.lowercase()
        t2.setTextColor(suave)
        t2.textSize = 13f

        val t3 = TextView(this)
        t3.text = "Escala do app: " + Atalhos.nomesEscala[Atalhos.indiceEscala(this)]
        t3.setTextColor(azul)
        t3.textSize = 12f

        txt.addView(t1)
        txt.addView(t2)
        txt.addView(t3)

        c.addView(thumb)
        c.addView(txt)
        return c
    }

    private fun simNao(b: Boolean): String {
        return if (b) "Ativado" else "Desativado"
    }

    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

    private fun shizukuRodando(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    private fun shizukuAutorizado(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    private fun garantirShizuku(): Boolean {
        if (!shizukuRodando()) {
            aviso("Shizuku não está rodando. Abra o Shizuku e toque em Iniciar.")
            return false
        }
        if (!shizukuAutorizado()) {
            aviso("Autorize o app na janelinha do Shizuku e toque de novo.")
            try {
                Shizuku.requestPermission(1)
            } catch (e: Throwable) {
                aviso("Erro: " + e.toString())
            }
            return false
        }
        return true
    }

    private fun appsInstalados(): List<ApplicationInfo> {
        val lista = packageManager.getInstalledApplications(0)
        return lista
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
    }

    private fun protegidos(): Set<String> {
        val salvos = prefs().getStringSet("protegidos", emptySet()) ?: emptySet()
        val s = HashSet<String>(salvos)
        s.addAll(fixos)
        s.add(packageName)
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null) s.add(ime.substringBefore("/"))
        val home = Intent(Intent.ACTION_MAIN)
        home.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        val pacoteHome = res?.activityInfo?.packageName
        if (pacoteHome != null) s.add(pacoteHome)
        return s
    }

    private fun textoInfo(): String {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mem = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mem)
            val livre = mem.availMem / (1024 * 1024)
            val total = mem.totalMem / (1024 * 1024)
            val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            val nivel = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val sh = if (!shizukuRodando()) "desligado"
            else if (shizukuAutorizado()) "pronto" else "falta autorizar"
            "RAM livre: " + livre + " MB de " + total + " MB\nTemperatura: " + temp +
                " °C\nBateria: " + nivel + "%\nShizuku: " + sh
        } catch (e: Throwable) {
            "Erro: " + e.toString()
        }
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
            if (Atalhos.salvarFundo(this, uri)) {
                mostrarSecao(1)
            } else {
                aviso("Erro ao trocar o fundo")
            }
        }
    }

    private fun modoJogo() {
        if (!garantirShizuku()) return
        aviso("Ativando...")
        val prot = protegidos()
        val alvos = appsInstalados().map { it.packageName }.filter { !prot.contains(it) }
        Thread {
            val antes = Atalhos.rodar("settings get global zen_mode").trim()
            if (antes.isNotEmpty() && !antes.startsWith("Erro")) {
                prefs().edit().putString("zen_antes", antes).apply()
            }
            Atalhos.rodar(
                "settings put global zen_mode 1; " +
                    "settings put global window_animation_scale 0.5; " +
                    "settings put global transition_animation_scale 0.5; " +
                    "settings put global animator_duration_scale 0.5"
            )
            for (p in alvos) {
                Atalhos.rodar("am force-stop " + p)
            }
            Atalhos.rodar("am kill-all")
            runOnUiThread {
                aviso("Modo Jogo ativado: " + alvos.size + " apps fechados. No Free Fire use gráficos Suave.")
                abrirJogo()
            }
        }.start()
    }

    private fun restaurar() {
        if (!garantirShizuku()) return
        Thread {
            val antes = prefs().getString("zen_antes", "0") ?: "0"
            val zen = if (antes.toIntOrNull() != null) antes else "0"
            Atalhos.rodar(
                "settings put global zen_mode " + zen + "; " +
                    "settings put global window_animation_scale 1.0; " +
                    "settings put global transition_animation_scale 1.0; " +
                    "settings put global animator_duration_scale 1.0"
            )
            runOnUiThread { aviso("Restaurado: Não Perturbe e animações voltaram ao normal.") }
        }.start()
    }

    private fun ativarJanelas(ligar: Boolean) {
        if (!garantirShizuku()) return
        val v = if (ligar) "1" else "0"
        Thread {
            Atalhos.rodar(
                "settings put global enable_freeform_support " + v + "; " +
                    "settings put global force_resizable_activities " + v
            )
            runOnUiThread { aviso("Pronto. Reinicie o celular e ligue o Shizuku de novo.") }
        }.start()
    }

    private fun fecharJanelas() {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", "fechar")
        startForegroundService(i)
    }

    private fun ligarOverlay() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                aviso("Ative 'Exibir sobre outros apps' para o Sent e volte aqui.")
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName)
                    )
                )
                return
            }
            startForegroundService(Intent(this, OverlayService::class.java))
            aviso("Overlay ligado. Veja o topo da tela.")
        } catch (e: Throwable) {
            aviso("Erro: " + e.toString())
        }
    }

    private fun desligarOverlay() {
        try {
            stopService(Intent(this, OverlayService::class.java))
            aviso("Overlay desligado.")
        } catch (e: Throwable) {
            aviso("Erro: " + e.toString())
        }
    }

    private fun abrirJogo() {
        val pacotes = listOf("com.dts.freefireth", "com.dts.freefiremax")
        for (p in pacotes) {
            val intent = packageManager.getLaunchIntentForPackage(p)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        aviso("Free Fire não encontrado neste celular.")
    }

    private fun secaoSistema(d: LinearLayout) {
        d.addView(titulo("Sistema"))
        d.addView(cabecalho())

        val info = cartao()
        val t = TextView(this)
        t.text = textoInfo()
        t.setTextColor(Color.WHITE)
        t.textSize = 13f
        t.setLineSpacing(0f, 1.25f)
        info.addView(t)
        d.addView(info)

        d.addView(linha("Lançador padrão", "Escolha o Sent como tela inicial") {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        })
        d.addView(linha("Atalho F11 / F5", "Ative o serviço Sent atalhos em Acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        d.addView(linha("Atualizar", "Recarregar as informações") { mostrarSecao(0) })
    }

    private fun secaoPersonalizar(d: LinearLayout) {
        d.addView(titulo("Personalizar"))
        d.addView(cabecalho())

        d.addView(linha("Plano de fundo", "Escolher uma foto da galeria") { escolherFundo() })
        d.addView(linha("Fundo padrão", "Voltar ao degradê") {
            Atalhos.apagarFundo(this)
            mostrarSecao(1)
        })
        d.addView(linha("Fonte", Atalhos.fontes[Atalhos.indiceFonte(this)].second) {
            Atalhos.escolher(this, "Fonte", Atalhos.fontes.map { it.second }) { i ->
                Atalhos.setI(this, "fonte", i)
                recreate()
            }
        })
        d.addView(linha("Cursor", Atalhos.cursores[Atalhos.indiceCursor(this)]) {
            Atalhos.escolher(this, "Cursor", Atalhos.cursores) { i ->
                Atalhos.setI(this, "cursor", i)
                recreate()
            }
        })
        d.addView(
            linha(
                "Resolução do app",
                "Escala " + Atalhos.nomesEscala[Atalhos.indiceEscala(this)] +
                    " (só o Sent, não a tela do celular)"
            ) {
                Atalhos.escolher(this, "Escala do app", Atalhos.nomesEscala) { i ->
                    Atalhos.setI(this, "escala", i)
                    recreate()
                }
            }
        )
    }

    private fun secaoJanelas(d: LinearLayout) {
        d.addView(titulo("Janelas"))

        d.addView(linha("Tamanho das janelas", Atalhos.nomesTamanho[Atalhos.indiceTamanho(this)]) {
            Atalhos.escolher(this, "Tamanho das janelas", Atalhos.nomesTamanho) { i ->
                Atalhos.setI(this, "jan_tam", i)
                mostrarSecao(2)
            }
        })
        d.addView(linha("Zoom da página", Atalhos.nomesZoom[Atalhos.indiceZoom(this)]) {
            Atalhos.escolher(this, "Zoom da página", Atalhos.nomesZoom) { i ->
                Atalhos.setI(this, "jan_zoom", i)
                mostrarSecao(2)
            }
        })
        d.addView(
            linha(
                "Abrir apps reais (com login)",
                simNao(Atalhos.mod
