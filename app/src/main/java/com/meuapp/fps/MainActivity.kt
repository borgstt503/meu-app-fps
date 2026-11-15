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
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private var info: TextView? = null
    private var listaApps: LinearLayout? = null

    private val corFundo = Color.parseColor("#202020")
    private val corCartao = Color.parseColor("#2B2B2B")
    private val suave = Color.parseColor("#A8A8A8")
    private val azul = Color.parseColor("#4CC2FF")

    private val fixos = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "moe.shizuku.privileged.api"
    )

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

    private fun rotulo(texto: String): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(azul)
        t.textSize = 12f
        t.setPadding(dp(4), dp(14), 0, dp(6))
        return t
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = corFundo
            window.navigationBarColor = corFundo

            val scroll = ScrollView(this)
            scroll.setBackgroundColor(corFundo)

            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.setPadding(dp(16), dp(12), dp(16), dp(28))
            scroll.addView(col)

            val topo = LinearLayout(this)
            topo.orientation = LinearLayout.HORIZONTAL
            topo.gravity = Gravity.CENTER_VERTICAL
            topo.setPadding(0, 0, 0, dp(8))

            val voltar = TextView(this)
            voltar.text = "<"
            voltar.setTextColor(Color.WHITE)
            voltar.textSize = 24f
            voltar.setPadding(dp(4), 0, dp(20), 0)
            voltar.setOnClickListener { finish() }

            val titulo = TextView(this)
            titulo.text = "Configurações"
            titulo.setTextColor(Color.WHITE)
            titulo.textSize = 22f

            topo.addView(voltar)
            topo.addView(titulo)
            col.addView(topo)

            val cartaoInfo = cartao()
            val nomePc = TextView(this)
            nomePc.text = "Este PC"
            nomePc.setTextColor(Color.WHITE)
            nomePc.textSize = 18f
            val t1 = TextView(this)
            t1.setTextColor(suave)
            t1.textSize = 13f
            t1.setLineSpacing(0f, 1.25f)
            info = t1
            cartaoInfo.addView(nomePc)
            cartaoInfo.addView(t1)
            col.addView(cartaoInfo)

            col.addView(rotulo("Jogo"))
            col.addView(linha("Modo Jogo", "Fecha apps, liga Não Perturbe e reduz animações") { modoJogo() })
            col.addView(linha("Restaurar", "Volta Não Perturbe e animações ao normal") { restaurar() })
            col.addView(linha("Apps protegidos", "Escolha os apps que nunca são fechados") { alternarLista() })
            val lista = cartao()
            lista.visibility = View.GONE
            listaApps = lista
            col.addView(lista)

            col.addView(rotulo("Overlay de FPS"))
            col.addView(linha("Ligar overlay", "FPS e hora no topo da tela") { ligarOverlay() })
            col.addView(linha("Desligar overlay", "Remove o texto do topo") { desligarOverlay() })

            col.addView(rotulo("Janelas livres"))
            col.addView(linha("Ativar janelas livres", "Depois reinicie o celular") { ativarJanelas(true) })
            col.addView(linha("Desativar janelas livres", "Depois reinicie o celular") { ativarJanelas(false) })

            col.addView(rotulo("Sistema"))
            col.addView(linha("Lançador padrão", "Escolha o Sent como tela inicial") {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            })
            col.addView(linha("Atalho F11 / F5", "Ative o serviço Sent atalhos em Acessibilidade") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })

            setContentView(scroll)

            Shizuku.addRequestPermissionResultListener { _, _ ->
                runOnUiThread { atualizar() }
            }
            atualizar()
        } catch (e: Throwable) {
            val erro = TextView(this)
            erro.text = "Erro: " + e.toString()
            setContentView(erro)
        }
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

    private fun alternarLista() {
        val l = listaApps ?: return
        if (l.visibility == View.VISIBLE) {
            l.visibility = View.GONE
            return
        }
        try {
            l.removeAllViews()
            val dica = TextView(this)
            dica.text = "Marque os apps que NÃO devem ser fechados:"
            dica.setTextColor(suave)
            dica.textSize = 12f
            dica.setPadding(0, 0, 0, dp(6))
            l.addView(dica)

            val salvos = HashSet<String>(prefs().getStringSet("protegidos", emptySet()) ?: emptySet())
            for (app in appsInstalados()) {
                val pkg = app.packageName
                if (fixos.contains(pkg) || pkg == packageName) continue
                val cb = CheckBox(this)
                cb.text = packageManager.getApplicationLabel(app).toString()
                cb.setTextColor(Color.WHITE)
                cb.buttonTintList = ColorStateList.valueOf(azul)
                cb.isChecked = salvos.contains(pkg)
                cb.setOnCheckedChangeListener { _, marcado ->
                    val atual = HashSet<String>(prefs().getStringSet("protegidos", emptySet()) ?: emptySet())
                    if (marcado) atual.add(pkg) else atual.remove(pkg)
                    prefs().edit().putStringSet("protegidos", atual).apply()
                }
                l.addView(cb)
            }
            l.visibility = View.VISIBLE
        } catch (e: Throwable) {
            aviso("Erro: " + e.toString())
        }
    }

    private fun modoJogo() {
        if (!garantirShizuku()) return
        aviso("Ativando...")
        val prot = protegidos()
        val alvos = appsInstalados().map { it.packageName }.filter { !prot.contains(it) }
        Thread {
            val antes = rodar("settings get global zen_mode").trim()
            if (antes.isNotEmpty() && !antes.startsWith("Erro")) {
                prefs().edit().putString("zen_antes", antes).apply()
            }
            rodar(
                "settings put global zen_mode 1; " +
                "settings put global window_animation_scale 0.5; " +
                "settings put global transition_animation_scale 0.5; " +
                "settings put global animator_duration_scale 0.5"
            )
            for (p in alvos) {
                rodar("am force-stop " + p)
            }
            rodar("am kill-all")
            runOnUiThread {
                aviso("Modo Jogo ativado: " + alvos.size + " apps fechados. No Free Fire use gráficos Suave.")
                atualizar()
                abrirJogo()
            }
        }.start()
    }

    private fun restaurar() {
        if (!garantirShizuku()) return
        Thread {
            val antes = prefs().getString("zen_antes", "0") ?: "0"
            val zen = if (antes.toIntOrNull() != null) antes else "0"
            rodar(
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
            rodar(
                "settings put global enable_freeform_support " + v + "; " +
                "settings put global force_resizable_activities " + v
            )
            runOnUiThread { aviso("Pronto. Reinicie o celular e ligue o Shizuku de novo.") }
        }.start()
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

    private fun atualizar() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mem = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mem)
            val livreMb = mem.availMem / (1024 * 1024)
            val totalMb = mem.totalMem / (1024 * 1024)

            val bateria = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (bateria?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            val nivel = bateria?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

            val sh = if (!shizukuRodando()) "desligado" else if (shizukuAutorizado()) "pronto" else "falta autorizar"

            info?.text = "RAM livre: $livreMb MB de $totalMb MB\nTemperatura: $temp °C\nBateria: $nivel%\nShizuku: $sh"
        } catch (e: Throwable) {
            info?.text = "Erro: " + e.toString()
        }
    }
}
