package com.meuapp.fps

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.File

class DesktopActivity : Activity() {

    private val pedirFundo = 7
    private var raiz: FrameLayout? = null
    private var menuIniciar: LinearLayout? = null
    private var menuBandeja: LinearLayout? = null
    private var grade: GridLayout? = null
    private var campoBusca: EditText? = null
    private var todosApps: List<ResolveInfo> = emptyList()

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun arred(cor: Int, raio: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(cor)
        g.cornerRadius = dp(raio).toFloat()
        return g
    }

    private fun iconeDe(pacote: String, fallback: Int): Drawable {
        return try {
            packageManager.getApplicationIcon(pacote)
        } catch (e: Throwable) {
            arred(fallback, 10)
        }
    }

    private fun abrirApp(pacote: String) {
        val intent = packageManager.getLaunchIntentForPackage(pacote)
        if (intent == null) {
            Toast.makeText(this, "App não encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private fun shizukuOk(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
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

    private fun abrirComoJanela(pacote: String) {
        if (!shizukuOk()) {
            Toast.makeText(this, "Shizuku desligado, abrindo em tela cheia", Toast.LENGTH_SHORT).show()
            abrirApp(pacote)
            return
        }
        Thread {
            val r = rodar(
                "am start -p " + pacote +
                " --windowingMode 5 -a android.intent.action.MAIN" +
                " -c android.intent.category.LAUNCHER 2>&1"
            )
            if (r.isNotBlank()) {
                runOnUiThread {
                    Toast.makeText(this, r.take(150), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun abrirJanelaWeb(titulo: String, url: String) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative 'Exibir sobre outros apps' e volte", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
            return
        }
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("titulo", titulo)
        i.putExtra("url", url)
        startForegroundService(i)
    }

    private fun fecharJanelas() {
        val i = Intent(this, JanelaService::class.java)
        i.putExtra("acao", "fechar")
        startForegroundService(i)
    }

    private fun protegidos(): Set<String> {
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val s = HashSet<String>(prefs.getStringSet("protegidos", emptySet()) ?: emptySet())
        s.add("com.dts.freefireth")
        s.add("com.dts.freefiremax")
        s.add("moe.shizuku.privileged.api")
        s.add(packageName)
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (ime != null) s.add(ime.substringBefore("/"))
        return s
    }

    private fun limparApps() {
        if (!shizukuOk()) {
            Toast.makeText(this, "Ligue o Shizuku e autorize o app", Toast.LENGTH_LONG).show()
            return
        }
        val prot = protegidos()
        val alvos = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }
            .filter { !prot.contains(it) }
        Toast.makeText(this, "Fechando " + alvos.size + " apps...", Toast.LENGTH_SHORT).show()
        Thread {
            for (p in alvos) {
                rodar("am force-stop " + p)
            }
            rodar("am kill-all")
            runOnUiThread {
                Toast.makeText(this, alvos.size.toString() + " apps fechados", Toast.LENGTH_SHORT).show()
            }
        }.start()
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
            try {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    File(filesDir, "fundo.img").outputStream().use { saida ->
                        entrada.copyTo(saida)
                    }
                }
                aplicarFundo()
            } catch (e: Throwable) {
                Toast.makeText(this, "Erro ao trocar o fundo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fundoPadrao() {
        File(filesDir, "fundo.img").delete()
        aplicarFundo()
    }

    private fun aplicarFundo() {
        val r = raiz ?: return
        val padrao = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#1B1B2F"), Color.parseColor("#7A1F3D"))
        )
        val f = File(filesDir, "fundo.img")
        if (!f.exists()) {
            r.background = padrao
            return
        }
        try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > 2000 || o.outHeight / s > 2000) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val bmp = BitmapFactory.decodeFile(f.path, o2)
            if (bmp != null) r.background = BitmapDrawable(resources, bmp) else r.background = padrao
        } catch (e: Throwable) {
            r.background = padrao
        }
    }

    private fun fecharMenus() {
        menuIniciar?.visibility = View.GONE
        menuBandeja?.visibility = View.GONE
    }

    private fun alternarIniciar() {
        val m = menuIniciar ?: return
        menuBandeja?.visibility = View.GONE
        if (m.visibility == View.VISIBLE) {
            m.visibility = View.GONE
        } else {
            campoBusca?.setText("")
            preencherGrade("")
            m.visibility = View.VISIBLE
        }
    }

    private fun alternarBandeja() {
        val m = menuBandeja ?: return
        menuIniciar?.visibility = View.GONE
        m.visibility = if (m.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun carregarApps() {
        val principal = Intent(Intent.ACTION_MAIN)
        principal.addCategory(Intent.CATEGORY_LAUNCHER)
        todosApps = packageManager.queryIntentActivities(principal, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    private fun preencherGrade(filtro: String) {
        val g = grade ?: return
        g.removeAllViews()
        for (ri in todosApps) {
            val nome = ri.loadLabel(packageManager).toString()
            if (filtro.isNotBlank() && !nome.contains(filtro, ignoreCase = true)) continue
            val pacote = ri.activityInfo.packageName

            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL

            val img = ImageView(this)
            img.setImageDrawable(ri.loadIcon(packageManager))
            img.layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))

            val t = TextView(this)
            t.text = nome
            t.setTextColor(Color.WHITE)
            t.textSize = 10f
            t.maxLines = 1
            t.ellipsize = TextUtils.TruncateAt.END
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(3), 0, 0)

            item.addView(img)
            item.addView(t)

            val lp = GridLayout.LayoutParams()
            lp.width = dp(64)
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.setMargins(dp(2), dp(6), dp(2), dp(6))
            item.layoutParams = lp

            item.setOnClickListener {
                fecharMenus()
                abrirApp(pacote)
            }
            item.setOnLongClickListener {
                fecharMenus()
                abrirComoJanela(pacote)
                true
            }
            g.addView(item)
        }
    }

    private fun montarMenuIniciar(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        m.background = arred(Color.parseColor("#F2262222"), 12)
        m.setPadding(dp(16), dp(14), dp(16), dp(10))
        m.visibility = View.GONE

        val busca = EditText(this)
        busca.hint = "Digite aqui para pesquisar"
        busca.setHintTextColor(Color.parseColor("#B0B0B0"))
        busca.setTextColor(Color.WHITE)
        busca.textSize = 13f
        busca.setSingleLine(true)
        busca.background = arred(Color.parseColor("#3A3636"), 18)
        busca.setPadding(dp(16), 0, dp(16), 0)
        busca.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(36)
        )
        busca.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                preencherGrade(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        campoBusca = busca

        val rotulo = TextView(this)
        rotulo.text = "Aplicativos"
        rotulo.setTextColor(Color.WHITE)
        rotulo.textSize = 13f
        rotulo.setPadding(dp(4), dp(14), 0, dp(6))

        val g = GridLayout(this)
        g.columnCount = 5
        grade = g

        val scroll = ScrollView(this)
        scroll.addView(g)
        val altura = minOf(dp(210), (resources.displayMetrics.heightPixels * 0.45).toInt())
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, altura
        )

        val rodape = LinearLayout(this)
        rodape.orientation = LinearLayout.HORIZONTAL
        rodape.setPadding(dp(4), dp(10), dp(4), 0)

        val ajustes = TextView(this)
        ajustes.text = "Ajustes"
        ajustes.setTextColor(Color.WHITE)
        ajustes.textSize = 13f
        ajustes.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        ajustes.setOnClickListener {
            fecharMenus()
            startActivity(Intent(this, MainActivity::class.java))
        }

        val sair = TextView(this)
        sair.text = "Fechar"
        sair.setTextColor(Color.parseColor("#B0B0B0"))
        sair.textSize = 13f
        sair.setOnClickListener { fecharMenus() }

        rodape.addView(ajustes)
        rodape.addView(sair)

        m.addView(busca)
        m.addView(rotulo)
        m.addView(scroll)
        m.addView(rodape)
        return m
    }

    private fun linhaMenu(texto: String, acao: () -> Unit): TextView {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.WHITE)
        t.textSize = 14f
        t.setPadding(dp(16), dp(11), dp(16), dp(11))
        t.setOnClickListener {
            fecharMenus()
            acao()
        }
        return t
    }

    private fun montarMenuBandeja(): LinearLayout {
        val m = LinearLayout(this)
        m.orientation = LinearLayout.VERTICAL
        m.background = arred(Color.parseColor("#F2262222"), 12)
        m.setPadding(0, dp(6), 0, dp(6))
        m.visibility = View.GONE
        m.addView(linhaMenu("Fechar apps (liberar RAM)") { limparApps() })
        m.addView(linhaMenu("Fechar janelas") { fecharJanelas() })
        m.addView(linhaMenu("Trocar fundo") { escolherFundo() })
        m.addView(linhaMenu("Fundo padrão") { fundoPadrao() })
        m.addView(linhaMenu("Ajustes") { startActivity(Intent(this, MainActivity::class.java)) })
        return m
    }

    private fun iconeDesktop(pacote: String, nome: String, fallback: Int, acao: () -> Unit): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.setPadding(dp(8), dp(6), dp(8), dp(4))

        val img = ImageView(this)
        img.setImageDrawable(iconeDe(pacote, fallback))
        img.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))

        val txt = TextView(this)
        txt.text = nome
        txt.setTextColor(Color.WHITE)
        txt.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        txt.textSize = 10f
        txt.gravity = Gravity.CENTER
        txt.setPadding(0, dp(3), 0, 0)

        item.addView(img)
        item.addView(txt)
        item.setOnClickListener {
            fecharMenus()
            acao()
        }
        return item
    }

    private fun iconeBarra(pacote: String, fallback: Int, acao: () -> Unit): ImageView {
        val img = ImageView(this)
        img.setImageDrawable(iconeDe(pacote, fallback))
        img.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
        img.setPadding(dp(5), dp(5), dp(5), dp(5))
        img.setOnClickListener {
            fecharMenus()
            acao()
        }
        return img
    }

    private fun logoIniciar(): GridLayout {
        val logo = GridLayout(this)
        logo.rowCount = 2
        logo.columnCount = 2
        logo.setPadding(dp(10), 0, dp(10), 0)
        for (i in 0 until 4) {
            val q = View(this)
            q.setBackgroundColor(Color.parseColor("#4CC2FF"))
            val lp = GridLayout.LayoutParams()
            lp.width = dp(9)
            lp.height = dp(9)
            lp.setMargins(dp(1), dp(1), dp(1), dp(1))
            q.layoutParams = lp
            logo.addView(q)
        }
        logo.setOnClickListener { alternarIniciar() }
        return logo
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        fecharMenus()
    }

    override fun onResume() {
        super.onResume()
        carregarApps()
        preencherGrade(campoBusca?.text?.toString() ?: "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#80000000")
        window.navigationBarColor = Color.parseColor("#202020")

        val r = FrameLayout(this)
        raiz = r

        val coluna = LinearLayout(this)
        coluna.orientation = LinearLayout.VERTICAL
        coluna.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        val area = GridLayout(this)
        area.orientation = GridLayout.VERTICAL
        area.rowCount = 4
        area.setPadding(dp(4), dp(8), dp(4), 0)
        area.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        area.setOnClickListener { fecharMenus() }

        val vermelho = Color.parseColor("#E62117")
        val cinza = Color.parseColor("#555555")

        area.addView(
            iconeDesktop(packageName, "Este PC", cinza) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        )
        area.addView(
            iconeDesktop("com.android.chrome", "Chrome", cinza) {
                abrirJanelaWeb("Chrome", "https://www.google.com")
            }
        )
        area.addView(
            iconeDesktop("com.google.android.youtube", "YouTube", vermelho) {
                abrirJanelaWeb("YouTube", "https://www.youtube.com")
            }
        )
        area.addView(
            iconeDesktop("com.zhiliaoapp.musically", "TikTok", cinza) {
                abrirJanelaWeb("TikTok", "https://www.tiktok.com")
            }
        )
        area.addView(
            iconeDesktop("com.dts.freefireth", "Free Fire", cinza) {
                abrirApp("com.dts.freefireth")
            }
        )
        area.addView(
            iconeDesktop("moe.shizuku.privileged.api", "Shizuku", cinza) {
                abrirApp("moe.shizuku.privileged.api")
            }
        )

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setBackgroundColor(Color.parseColor("#E6202020"))
        barra.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        )

        val esquerda = View(this)
        esquerda.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)

        val centro = LinearLayout(this)
        centro.orientation = LinearLayout.HORIZONTAL
        centro.gravity = Gravity.CENTER_VERTICAL

        val pesquisa = TextView(this)
        pesquisa.text = "Pesquisar"
        pesquisa.setTextColor(Color.parseColor("#D0D0D0"))
        pesquisa.textSize = 12f
        pesquisa.gravity = Gravity.CENTER_VERTICAL
        pesquisa.setPadding(dp(14), 0, dp(14), 0)
        pesquisa.background = arred(Color.parseColor("#3A3A3A"), 16)
        val lpPesquisa = LinearLayout.LayoutParams(dp(140), dp(30))
        lpPesquisa.leftMargin = dp(2)
        lpPesquisa.rightMargin = dp(8)
        pesquisa.layoutParams = lpPesquisa
        pesquisa.setOnClickListener { alternarIniciar() }

        centro.addView(logoIniciar())
        centro.addView(pesquisa)
        centro.addView(iconeBarra("com.android.chrome", cinza) {
            abrirJanelaWeb("Chrome", "https://www.google.com")
        })
        centro.addView(iconeBarra("com.google.android.youtube", vermelho) {
            abrirJanelaWeb("YouTube", "https://www.youtube.com")
        })
        centro.addView(iconeBarra("com.zhiliaoapp.musically", cinza) {
            abrirJanelaWeb("TikTok", "https://www.tiktok.com")
        })
        centro.addView(iconeBarra("com.dts.freefireth", cinza) {
            abrirApp("com.dts.freefireth")
        })
        centro.addView(iconeBarra("moe.shizuku.privileged.api", cinza) {
            abrirApp("moe.shizuku.privileged.api")
        })

        val direita = LinearLayout(this)
        direita.orientation = LinearLayout.HORIZONTAL
        direita.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        direita.setPadding(0, 0, dp(12), 0)
        direita.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        )

        val seta = TextView(this)
        seta.text = "^"
        seta.setTextColor(Color.WHITE)
        seta.textSize = 18f
        seta.gravity = Gravity.CENTER
        seta.setPadding(dp(12), 0, dp(12), 0)
        seta.setOnClickListener { alternarBandeja() }

        val relogio = LinearLayout(this)
        relogio.orientation = LinearLayout.VERTICAL
        relogio.gravity = Gravity.END

        val hora = TextClock(this)
        hora.format24Hour = "HH:mm"
        hora.format12Hour = "HH:mm"
        hora.setTextColor(Color.WHITE)
        hora.textSize = 11f
        hora.gravity = Gravity.END

        val data = TextClock(this)
        data.format24Hour = "dd-MM-yyyy"
        data.format12Hour = "dd-MM-yyyy"
        data.setTextColor(Color.WHITE)
        data.textSize = 10f
        data.gravity = Gravity.END

        relogio.addView(hora)
        relogio.addView(data)
        direita.addView(seta)
        direita.addView(relogio)

        barra.addView(esquerda)
        barra.addView(centro)
        barra.addView(direita)

        coluna.addView(area)
        coluna.addView(barra)
        r.addView(coluna)

        val mi = montarMenuIniciar()
        val lpMi = FrameLayout.LayoutParams(
            dp(400), FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        lpMi.bottomMargin = dp(50)
        mi.layoutParams = lpMi
        menuIniciar = mi
        r.addView(mi)

        val mb = montarMenuBandeja()
        val lpMb = FrameLayout.LayoutParams(
            dp(240), FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
        lpMb.bottomMargin = dp(50)
        lpMb.rightMargin = dp(8)
        mb.layoutParams = lpMb
        menuBandeja = mb
        r.addView(mb)

        setContentView(r)
        aplicarFundo()
        carregarApps()
    }
}
