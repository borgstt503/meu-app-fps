package com.meuapp.fps

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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

    private val fixos = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "moe.shizuku.privileged.api"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val scroll = ScrollView(this)
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(48, 96, 48, 96)
            scroll.addView(layout)

            val t1 = TextView(this)
            t1.textSize = 20f
            info = t1

            val t2 = TextView(this)
            t2.textSize = 16f
            t2.setPadding(0, 32, 0, 32)
            status = t2

            val b1 = Button(this)
            b1.text = "Modo Jogo"
            b1.setOnClickListener { modoJogo() }

            val b2 = Button(this)
            b2.text = "Restaurar"
            b2.setOnClickListener { restaurar() }

            val b3 = Button(this)
            b3.text = "Ligar overlay (FPS e hora)"
            b3.setOnClickListener { ligarOverlay() }

            val b4 = Button(this)
            b4.text = "Desligar overlay"
            b4.setOnClickListener { desligarOverlay() }

            val b5 = Button(this)
            b5.text = "Apps protegidos (nao fechar)"
            b5.setOnClickListener { alternarLista() }

            val lista = LinearLayout(this)
            lista.orientation = LinearLayout.VERTICAL
            lista.visibility = View.GONE
            listaApps = lista

            val b6 = Button(this)
            b6.text = "Atualizar"
            b6.setOnClickListener { atualizar() }

            layout.addView(t1)
            layout.addView(t2)
            layout.addView(b1)
            layout.addView(b2)
            layout.addView(b3)
            layout.addView(b4)
            layout.addView(b5)
            layout.addView(lista)
            layout.addView(b6)
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
            aviso.setPadding(0, 16, 0, 16)
            l.addView(aviso)

            val salvos = HashSet<String>(prefs().getStringSet("protegidos", emptySet()) ?: emptySet())
            for (app in appsInstalados()) {
                val pkg = app.packageName
                if (fixos.contains(pkg) || pkg == packageName) continue
                val cb = CheckBox(this)
                cb.text = packageManager.getApplicationLabel(app).toString()
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
