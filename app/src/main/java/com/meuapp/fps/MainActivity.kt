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
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private var info: TextView? = null
    private var status: TextView? = null
    private var listaApps: LinearLayout? = null

    private val corFundo = Color.parseColor("#121212")
    private val corCartao = Color.parseColor("#1E1E1E")
    private val corBotao = Color.parseColor("#2A2A2A")
    private val laranja = Color.parseColor("#FF8A00")
    private val textoSuave = Color.parseColor("#B0B0B0")

    private val fixos = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "moe.shizuku.privileged.api"
    )

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun fundoArredondado(cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(raio).toFloat()
        return g
    }

    private fun criarCartao(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = fundoArredondado(corCartao, 16)
        c.setPadding(dp(20), dp(16), dp(20), dp(16))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(12)
        c.layoutParams = lp
        return c
    }

    private fun criarBotao(texto: String, principal: Boolean, acao: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.isAllCaps = false
        b.textSize = if (principal) 18f else 15f
        b.setTextColor(if (principal) Color.BLACK else Color.WHITE)
        b.background = fundoArredondado(if (principal) laranja else corBotao, 14)
        val altura = if (principal) dp(60) else dp(50)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, altura)
        lp.bottomMargin = dp(10)
        b.layoutParams = lp
        b.setOnClickListener { acao() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = corFundo
            window.navigationBarColor = corFundo

            val scroll = ScrollView(this)
            scroll.setBackgroundColor(corFundo)

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(dp(20), dp(40), dp(20), dp(40))
            scroll.addView(layout)

            val titulo = TextView(this)
            titulo.text = "Meu App FPS"
            titulo.textSize = 28f
            titulo.setTextColor(laranja)
            titulo.setPadding(dp(4), 0, 0, dp(4))

            val sub = TextView(this)
            sub.text = "Desempenho para o Free Fire"
            sub.textSize = 14f
            sub.setTextColor(textoSuave)
            sub.setPadding(dp(4), 0, 0, dp(20))

            val cartaoInfo = criarCartao()
            val t1 = TextView(this)
            t1.textSize = 16f
            t1.setTextColor(Color.WHITE)
            t1.setLineSpacing(0f, 1.3f)
            info = t1
            cartaoInfo.addView(t1)

            val cartaoStatus = criarCartao()
            val t2 = TextView(this)
            t2.textSize = 14f
            t2.setTextColor(textoSuave)
            t2.text = "Pronto."
            status = t2
            cartaoStatus.addView(t2)

            val lista = criarCartao()
            lista.visibility = View.GONE
            listaApps = lista

            layout.addView(titulo)
            layout.addView(sub)
            layout.addView(cartaoInfo)
            layout.addView(cartaoStatus)
            layout.addView(criarBotao("Modo Jogo", true) { modoJogo() })
            layout.addView(criarBotao("Restaurar", false) { restaurar() })
            layout.addView(criarBotao("Ligar overlay (FPS e hora)", false) { ligarOverlay() })
            layout.addView(criarBotao("Desligar overlay", false) { desligarOverlay() })
            layout.addView(criarBotao("Apps protegidos (nao fechar)", false) { alternarLista() })
            layout.addView(lista)
            layout.addView(criarBotao("Atualizar", false) { atualizar() })
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
            status?.text = "Shizuku nao esta rodando. Abra o Shizuku e toque em Iniciar."
            return false
        }
        if (!shizukuAutorizado()) {
            status?.text = "Autorize o app na janelinha do Shizuku e toque no botao de novo."
            try {
                Shizuku.requestPermission(1)
            } catch (e: Throwable) {
                status?.text = "Erro: " + e.toString()
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
            val aviso = TextView(this)
            aviso.text = "Marque os apps que NAO devem ser fechados:"
            aviso.setTextColor(laranja)
            aviso.setPadding(0, 0, 0, dp(8))
            l.addView(aviso)

            val salvos = HashSet<String>(prefs().getStringSet("protegidos", emptySet()) ?: emptySet())
            for (app in appsInstalados()) {
                val pkg = app.packageName
                if (fixos.contains(pkg) || pkg == packageName) continue
                val cb = CheckBox(this)
                cb.text = packageManager.getApplicationLabel(app).toString()
                cb.setTextColor(Color.WHITE)
                cb.buttonTintList = ColorStateList.valueOf(laranja)
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
            status?.text = "Erro: " + e.toString()
        }
    }

    private fun modoJogo() {
        if (!garantirShizuku()) return
        status?.text = "Ativando..."
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
                status?.text = "Modo Jogo ativado. Nao Perturbe ligado e " + alvos.size +
                    " apps fechados. Lembre: graficos Suave e FPS Normal no Free Fire."
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
            runOnUiThread {
                status?.text = "Restaurado: Nao Perturbe e animacoes voltaram ao normal."
            }
        }.start()
    }

    private fun ligarOverlay() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                status?.text = "Ative 'Exibir sobre outros apps' para o Meu App FPS e volte aqui."
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
                startActivity(intent)
                return
            }
            startForegroundService(Intent(this, OverlayService::class.java))
            status?.text = "Overlay ligado. Veja o topo da tela."
        } catch (e: Throwable) {
            status?.text = "Erro: " + e.toString()
        }
    }

    private fun desligarOverlay() {
        try {
            stopService(Intent(this, OverlayService::class.java))
            status?.text = "Overlay desligado."
        } catch (e: Throwable) {
            status?.text = "Erro: " + e.toString()
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
        status?.text = "Free Fire nao encontrado neste celular."
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
