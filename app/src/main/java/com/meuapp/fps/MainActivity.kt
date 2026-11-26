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
    private var secaoAtual = 0
    private var listaAberta = false

    private val corFundo by lazy { Atalhos.fundoTema(this) }
    private val corCartao by lazy { Atalhos.cartaoTema(this) }
    private val suave by lazy { Atalhos.suaveTema(this) }
    private val azul by lazy { Atalhos.acento(this) }

    private val fixos = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "moe.shizuku.privileged.api"
    )

    private val tf by lazy { Atalhos.typeface(this) }

    private val atalhosAjuda = listOf(
        Pair("F1", "Janelas recentes (a tela do quadrado). Deslize um app para fechar"),
        Pair("F2", "Controle da janela da frente: Esq, Dir, menor, maior, Min, Máx e Padrão"),
        Pair("F3", "Barra de tarefas flutuante com seus apps fixados"),
        Pair("F4", "Fixa ou solta a tela (trava barra de notificações e Home)"),
        Pair("F5", "Recarrega a janela flutuante. Sem janela, reinicia o app da frente (aperte 2 vezes)"),
        Pair("F6", "Resolução da tela: presets, resetar e outra resolução"),
        Pair("F7", "Liga ou desliga o teclado na janela flutuante de site"),
        Pair("F8", "Volta para a tela inicial do Sent"),
        Pair("F9", "Painel rápido: Wi-Fi, Bluetooth, brilho, volume e mais"),
        Pair("F10", "Mostra ou esconde o FPS e a hora"),
        Pair("F11", "Barra de apps no topo, com brilho e atalhos"),
        Pair("F12", "Apaga a tela. Para ligar, use o botão de ligar")
    )

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

    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

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

    private fun linha(ic: String, titulo: String, sub: String, acao: () -> Unit): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.HORIZONTAL
        c.gravity = Gravity.CENTER_VERTICAL
        c.background = arred(corCartao, 8)
        c.setPadding(dp(16), dp(12), dp(16), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(6)
        c.layoutParams = lp

        val i = TextView(this)
        i.text = ic
        i.textSize = 20f
        i.setPadding(0, 0, dp(14), 0)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )

        val t = TextView(this)
        t.text = titulo
        t.setTextColor(Color.WHITE)
        t.textSize = 15f

        val s = TextView(this)
        s.text = sub
        s.setTextColor(suave)
        s.textSize = 12f

        col.addView(t)
        col.addView(s)

        val seta = TextView(this)
        seta.text = "›"
        seta.setTextColor(suave)
        seta.textSize = 22f

        c.addView(i)
        c.addView(col)
        c.addView(seta)
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

    private fun simNao(b: Boolean): String {
        return if (b) "Ativado" else "Desativado"
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
        t3.text = "Tema: " + Atalhos.nomesTema[Atalhos.indiceTema(this)] +
            "   Escala: " + Atalhos.nomesEscala[Atalhos.indiceEscala(this)]
        t3.setTextColor(azul)
        t3.textSize = 12f

        txt.addView(t1)
        txt.addView(t2)
        txt.addView(t3)
        c.addView(thumb)
        c.addView(txt)
        return c
    }

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
                aviso("Modo Jogo ativado: " + alvos.size + " apps fechados.")
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

    private fun rodarSecao(cmd: String, secao: Int) {
        if (!garantirShizuku()) return
        Thread {
            Atalhos.rodar(cmd)
            Thread.sleep(300)
            runOnUiThread { mostrarSecao(secao) }
        }.start()
    }

    private fun orientacaoAtual(): String {
        val auto = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        if (auto == 1) return "Automática"
        return when (Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION, 0)) {
            1 -> "Paisagem"
            2 -> "Retrato invertido"
            3 -> "Paisagem invertida"
            else -> "Retrato"
        }
    }

    private fun infoMouse(destino: TextView) {
        if (!Atalhos.shizukuOk()) {
            destino.text = "Ligue o Shizuku para listar os mouses"
            return
        }
        Thread {
            val s = Atalhos.rodar("cat /proc/bus/input/devices")
            val achados = ArrayList<String>()
            for (bloco in s.split("\n\n")) {
                if (!Regex("Handlers=.*mouse").containsMatchIn(bloco)) continue
                val nome = Regex("Name=\"([^\"]*)\"").find(bloco)?.groupValues?.get(1) ?: continue
                val vp = Regex("Vendor=(\\w+) Product=(\\w+)").find(bloco)
                achados.add(
                    nome + (if (vp != null) "  (" + vp.groupValues[1] + ":" + vp.groupValues[2] + ")" else "")
                )
            }
            val vel = Settings.System.getInt(contentResolver, "pointer_speed", 0)
            val texto = (if (achados.isEmpty()) "Nenhum mouse detectado" else achados.joinToString("\n")) +
                "\nVelocidade do ponteiro: " + vel +
                "\nDPI: o Android não informa. Ele é ajustado no botão DPI do próprio mouse."
            runOnUiThread { destino.text = texto }
        }.start()
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

        d.addView(linha("🏠", "Lançador padrão", "Escolha o Sent como tela inicial") {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        })
        d.addView(linha("⌨️", "Atalhos de teclado", "Ative o serviço Sent atalhos em Acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        d.addView(linha("📋", "Gerenciador de tarefas", "Apps em execução e memória") {
            startActivity(Intent(this, GerenciadorActivity::class.java))
        })
        d.addView(linha("🔄", "Atualizar", "Recarregar as informações") { mostrarSecao(0) })
    }

    private fun secaoPersonalizar(d: LinearLayout) {
        d.addView(titulo("Personalizar"))
        d.addView(cabecalho())

        d.addView(linha("🖼️", "Plano de fundo", "Escolher uma foto da galeria") { escolherFundo() })
        d.addView(linha("♻️", "Fundo padrão", "Voltar ao degradê") {
            Atalhos.apagarFundo(this)
            mostrarSecao(1)
        })
        d.addView(linha("🎨", "Cor do tema", Atalhos.nomesTema[Atalhos.indiceTema(this)]) {
            Atalhos.escolher(this, "Cor do tema", Atalhos.nomesTema) { i ->
                Atalhos.setI(this, "tema", i)
                recreate()
            }
        })
        d.addView(linha("🔤", "Fonte", Atalhos.fontes[Atalhos.indiceFonte(this)].second) {
            Atalhos.escolher(this, "Fonte", Atalhos.fontes.map { it.second }) { i ->
                Atalhos.setI(this, "fonte", i)
                recreate()
            }
        })
        d.addView(linha("🖱️", "Cursor", Atalhos.cursores[Atalhos.indiceCursor(this)]) {
            Atalhos.escolher(this, "Cursor", Atalhos.cursores) { i ->
                Atalhos.setI(this, "cursor", i)
                recreate()
            }
        })
        d.addView(
            linha(
                "📐",
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

        d.addView(linha("📏", "Tamanho das janelas", Atalhos.nomesTamanho[Atalhos.indiceTamanho(this)]) {
            Atalhos.escolher(this, "Tamanho das janelas", Atalhos.nomesTamanho) { i ->
                Atalhos.setI(this, "jan_tam", i)
                mostrarSecao(2)
            }
        })
        d.addView(linha("🔍", "Zoom da página", Atalhos.nomesZoom[Atalhos.indiceZoom(this)]) {
            Atalhos.escolher(this, "Zoom da página", Atalhos.nomesZoom) { i ->
                Atalhos.setI(this, "jan_zoom", i)
                mostrarSecao(2)
            }
        })
        d.addView(
            linha("🪟", "Abrir apps reais em janela livre", simNao(Atalhos.modoApp(this))) {
                Atalhos.setB(this, "modo_app", !Atalhos.modoApp(this))
                mostrarSecao(2)
            }
        )
        d.addView(linha("🛡️", "Bloqueio de anúncios", simNao(Atalhos.bloqueio(this))) {
            Atalhos.setB(this, "bloq", !Atalhos.bloqueio(this))
            mostrarSecao(2)
        })
        d.addView(linha("✅", "Ativar janelas livres", "Depois reinicie o celular") { ativarJanelas(true) })
        d.addView(linha("⛔", "Desativar janelas livres", "Depois reinicie o celular") { ativarJanelas(false) })
        d.addView(linha("✖️", "Fechar janelas", "Fecha as janelas de site abertas") { fecharJanelas() })
    }

    private fun secaoJogo(d: LinearLayout) {
        d.addView(titulo("Jogo"))

        d.addView(linha("🎮", "Modo Jogo", "Fecha apps, liga Não Perturbe e reduz animações") { modoJogo() })
        d.addView(linha("↩️", "Restaurar", "Volta Não Perturbe e animações ao normal") { restaurar() })
        d.addView(
            linha(
                "🔒",
                "Apps protegidos",
                if (listaAberta) "Toque para esconder a lista" else "Escolha os apps que nunca são fechados"
            ) {
                listaAberta = !listaAberta
                mostrarSecao(3)
            }
        )

        if (listaAberta) {
            val c = cartao()
            val dica = TextView(this)
            dica.text = "Marque os apps que NÃO devem ser fechados:"
            dica.setTextColor(suave)
            dica.textSize = 12f
            dica.setPadding(0, 0, 0, dp(6))
            c.addView(dica)

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
                c.addView(cb)
            }
            d.addView(c)
        }
    }

    private fun secaoOverlay(d: LinearLayout) {
        d.addView(titulo("Overlay de FPS"))
        d.addView(linha("📊", "Ligar overlay", "FPS e hora no topo da tela (F10 também)") { ligarOverlay() })
        d.addView(linha("🚫", "Desligar overlay", "Remove o texto do topo") { desligarOverlay() })
    }

    private fun secaoTela(d: LinearLayout) {
        d.addView(titulo("Tela"))

        val travada = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 0

        d.addView(linha("📱", "Orientação da tela", orientacaoAtual()) {
            Atalhos.escolher(
                this, "Orientação da tela",
                listOf("Automática", "Paisagem", "Retrato", "Paisagem invertida")
            ) { i ->
                val cmd = when (i) {
                    0 -> "settings put system accelerometer_rotation 1"
                    1 -> "settings put system accelerometer_rotation 0; settings put system user_rotation 1"
                    2 -> "settings put system accelerometer_rotation 0; settings put system user_rotation 0"
                    else -> "settings put system accelerometer_rotation 0; settings put system user_rotation 3"
                }
                rodarSecao(cmd, 5)
            }
        })
        d.addView(
            linha("🔐", "Bloqueio de rotação", if (travada) "Travada" else "Automática") {
                rodarSecao(
                    "settings put system accelerometer_rotation " + (if (travada) "1" else "0"),
                    5
                )
            }
        )
        d.addView(linha("🖥️", "Resolução da tela", "Qualquer resolução, com presets salvos (F6)") {
            startActivity(Intent(this, ResolucaoActivity::class.java))
        })
        d.addView(linha("🔆", "Painel rápido", "Brilho, volume, Wi-Fi e mais (F9)") {
            startActivity(Intent(this, PainelActivity::class.java))
        })
    }

    private fun secaoMouse(d: LinearLayout) {
        d.addView(titulo("Mouse"))

        val info = cartao()
        val t = TextView(this)
        t.text = "Procurando..."
        t.setTextColor(Color.WHITE)
        t.textSize = 13f
        t.setLineSpacing(0f, 1.25f)
        info.addView(t)
        d.addView(info)
        infoMouse(t)

        d.addView(
            linha(
                "⚡",
                "Velocidade do ponteiro",
                "Valor atual: " + Settings.System.getInt(contentResolver, "pointer_speed", 0)
            ) {
                val valores = (-7..7).toList()
                Atalhos.escolher(this, "Velocidade do ponteiro", valores.map { it.toString() }) { i ->
                    rodarSecao("settings put system pointer_speed " + valores[i], 6)
                }
            }
        )
        d.addView(linha("🖱️", "Estilo do cursor", Atalhos.cursores[Atalhos.indiceCursor(this)]) {
            Atalhos.escolher(this, "Cursor", Atalhos.cursores) { i ->
                Atalhos.setI(this, "cursor", i)
                recreate()
            }
        })
        d.addView(linha("🔄", "Atualizar", "Ler o mouse de novo") { mostrarSecao(6) })
    }

    private fun secaoAjuda(d: LinearLayout) {
        d.addView(titulo("Ajuda"))

        val intro = cartao()
        val it = TextView(this)
        it.text = "Teclas de atalho do Sent. Elas funcionam com o serviço Sent atalhos ligado em " +
            "Acessibilidade e, na maioria, com o Shizuku rodando."
        it.setTextColor(suave)
        it.textSize = 12f
        intro.addView(it)
        d.addView(intro)

        for (par in atalhosAjuda) {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.HORIZONTAL
            c.gravity = Gravity.CENTER_VERTICAL
            c.background = arred(corCartao, 8)
            c.setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(6)
            c.layoutParams = lp

            val k = TextView(this)
            k.text = par.first
            k.setTextColor(Color.BLACK)
            k.textSize = 13f
            k.gravity = Gravity.CENTER
            k.background = arred(azul, 6)
            k.setPadding(dp(10), dp(4), dp(10), dp(4))

            val x = TextView(this)
            x.text = par.second
            x.setTextColor(Color.WHITE)
            x.textSize = 13f
            x.setPadding(dp(12), 0, 0, 0)
            x.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            c.addView(k)
            c.addView(x)
            d.addView(c)
        }

        val dicas = cartao()
        val dt = TextView(this)
        dt.text = "Dicas\n" +
            "• Clique direito (ou segure) em um app para fixar na barra ou na área de trabalho.\n" +
            "• Clique direito na área de trabalho: fundo, tema e gerenciador de tarefas.\n" +
            "• Chrome, Opera e outros apps abrem em janela livre pelo Shizuku.\n" +
            "• Sent versão 1.0"
        dt.setTextColor(Color.WHITE)
        dt.textSize = 12f
        dt.setLineSpacing(0f, 1.25f)
        dicas.addView(dt)
        d.addView(dicas)
    }

    private fun mostrarSecao(i: Int) {
        secaoAtual = i
        val d = direita ?: return
        d.removeAllViews()

        for (k in itensMenu.indices) {
            val t = itensMenu[k]
            if (k == i) {
                t.background = arred(corCartao, 8)
                t.setTextColor(Color.WHITE)
            } else {
                t.background = null
                t.setTextColor(suave)
            }
        }

        when (i) {
            0 -> secaoSistema(d)
            1 -> secaoPersonalizar(d)
            2 -> secaoJanelas(d)
            3 -> secaoJogo(d)
            4 -> secaoOverlay(d)
            5 -> secaoTela(d)
            6 -> secaoMouse(d)
            else -> secaoAjuda(d)
        }
        Atalhos.aplicarFonte(d, tf)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = corFundo
            window.navigationBarColor = corFundo

            val raiz = LinearLayout(this)
            raiz.orientation = LinearLayout.HORIZONTAL
            raiz.setBackgroundColor(corFundo)

            val menu = LinearLayout(this)
            menu.orientation = LinearLayout.VERTICAL
            menu.setPadding(dp(10), dp(14), dp(10), dp(10))

            val voltar = TextView(this)
            voltar.text = "‹  Configurações"
            voltar.setTextColor(Color.WHITE)
            voltar.textSize = 15f
            voltar.setPadding(dp(8), dp(6), dp(8), dp(16))
            voltar.setOnClickListener { finish() }
            menu.addView(voltar)

            val nomes = listOf(
                "Sistema", "Personalizar", "Janelas", "Jogo",
                "Overlay de FPS", "Tela", "Mouse", "Ajuda"
            )
            for (i in nomes.indices) {
                val t = TextView(this)
                t.text = nomes[i]
                t.textSize = 14f
                t.setPadding(dp(12), dp(11), dp(12), dp(11))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(3)
                t.layoutParams = lp
                t.setOnClickListener { mostrarSecao(i) }
                itensMenu.add(t)
                menu.addView(t)
            }

            val scrollMenu = ScrollView(this)
            scrollMenu.addView(menu)
            scrollMenu.layoutParams = LinearLayout.LayoutParams(
                dp(170), LinearLayout.LayoutParams.MATCH_PARENT
            )

            val scroll = ScrollView(this)
            scroll.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            )
            val dir = LinearLayout(this)
            dir.orientation = LinearLayout.VERTICAL
            dir.setPadding(dp(8), dp(14), dp(16), dp(24))
            scroll.addView(dir)
            direita = dir

            raiz.addView(scrollMenu)
            raiz.addView(scroll)
            Atalhos.aplicarFonte(menu, tf)
            setContentView(raiz)

            Shizuku.addRequestPermissionResultListener { _, _ ->
                runOnUiThread { mostrarSecao(secaoAtual) }
            }
            mostrarSecao(0)
        } catch (e: Throwable) {
            val erro = TextView(this)
            erro.text = "Erro: " + e.toString()
            setContentView(erro)
        }
    }
}
