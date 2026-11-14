package com.meuapp.fps

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class ArquivosActivity : Activity() {

    private val raizDir: File = Environment.getExternalStorageDirectory()
    private var atual: File = raizDir
    private var caminho: TextView? = null
    private var lista: LinearLayout? = null

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun permissao(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true
    }

    private fun tamanho(b: Long): String {
        if (b < 1024) return b.toString() + " B"
        val kb = b / 1024.0
        if (kb < 1024) return String.format("%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    private fun subir() {
        val pai = atual.parentFile
        if (atual.path != raizDir.path && pai != null) {
            atual = pai
            mostrar()
        } else {
            finish()
        }
    }

    private fun novaPasta() {
        val campo = EditText(this)
        campo.hint = "Nome da pasta"
        AlertDialog.Builder(this)
            .setTitle("Nova pasta")
            .setView(campo)
            .setPositiveButton("Criar") { _, _ ->
                val nome = campo.text.toString().trim()
                if (nome.isNotEmpty()) {
                    File(atual, nome).mkdirs()
                    mostrar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun apagar(f: File) {
        AlertDialog.Builder(this)
            .setTitle("Apagar?")
            .setMessage(f.name)
            .setPositiveButton("Apagar") { _, _ ->
                val ok = f.deleteRecursively()
                Toast.makeText(this, if (ok) "Apagado" else "Não foi possível apagar", Toast.LENGTH_SHORT).show()
                mostrar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun linha(f: File): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        l.setPadding(dp(8), dp(11), dp(8), dp(11))

        val icone = TextView(this)
        icone.text = if (f.isDirectory) "📁" else "📄"
        icone.textSize = 20f
        icone.setPadding(0, 0, dp(14), 0)

        val nome = TextView(this)
        nome.text = f.name
        nome.setTextColor(Color.WHITE)
        nome.textSize = 15f
        nome.maxLines = 1
        nome.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        l.addView(icone)
        l.addView(nome)

        if (!f.isDirectory) {
            val t = TextView(this)
            t.text = tamanho(f.length())
            t.setTextColor(Color.parseColor("#A8A8A8"))
            t.textSize = 12f
            l.addView(t)
        }

        l.setOnClickListener {
            if (f.isDirectory) {
                atual = f
                mostrar()
            } else {
                Toast.makeText(this, "Abrir arquivos ainda não está disponível", Toast.LENGTH_SHORT).show()
            }
        }
        l.setOnLongClickListener {
            apagar(f)
            true
        }
        return l
    }

    private fun mostrar() {
        val l = lista ?: return
        l.removeAllViews()
        caminho?.text = atual.path.replace(raizDir.path, "Armazenamento interno")

        if (!permissao()) {
            val aviso = TextView(this)
            aviso.text = "Para ver os arquivos, permita o acesso a todos os arquivos."
            aviso.setTextColor(Color.WHITE)
            aviso.textSize = 15f
            aviso.setPadding(dp(8), dp(16), dp(8), dp(12))

            val botao = TextView(this)
            botao.text = "Permitir acesso"
            botao.setTextColor(Color.BLACK)
            botao.textSize = 15f
            botao.gravity = Gravity.CENTER
            val fundo = GradientDrawable()
            fundo.setColor(Color.parseColor("#4CC2FF"))
            fundo.cornerRadius = dp(10).toFloat()
            botao.background = fundo
            botao.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
            )
            botao.setOnClickListener {
                if (Build.VERSION.SDK_INT >= 30) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + packageName)
                        )
                    )
                }
            }
            l.addView(aviso)
            l.addView(botao)
            return
        }

        val itens = atual.listFiles()
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        if (itens.isEmpty()) {
            val vazio = TextView(this)
            vazio.text = "Pasta vazia"
            vazio.setTextColor(Color.parseColor("#A8A8A8"))
            vazio.setPadding(dp(8), dp(16), dp(8), dp(8))
            l.addView(vazio)
        }
        for (f in itens) l.addView(linha(f))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        subir()
    }

    override fun onResume() {
        super.onResume()
        mostrar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#202020")
        window.navigationBarColor = Color.parseColor("#202020")

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#202020"))
        raiz.setPadding(dp(12), dp(8), dp(12), dp(8))

        val topo = LinearLayout(this)
        topo.orientation = LinearLayout.HORIZONTAL
        topo.gravity = Gravity.CENTER_VERTICAL
        topo.setPadding(0, 0, 0, dp(6))

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.setTextColor(Color.WHITE)
        voltar.textSize = 26f
        voltar.setPadding(dp(4), 0, dp(16), 0)
        voltar.setOnClickListener { subir() }

        val cam = TextView(this)
        cam.setTextColor(Color.WHITE)
        cam.textSize = 14f
        cam.maxLines = 1
        cam.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        caminho = cam

        val nova = TextView(this)
        nova.text = "+ Pasta"
        nova.setTextColor(Color.parseColor("#4CC2FF"))
        nova.textSize = 14f
        nova.setPadding(dp(8), dp(8), dp(8), dp(8))
        nova.setOnClickListener { novaPasta() }

        topo.addView(voltar)
        topo.addView(cam)
        topo.addView(nova)

        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        lista = l

        val scroll = ScrollView(this)
        scroll.addView(l)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        raiz.addView(topo)
        raiz.addView(scroll)
        setContentView(raiz)
    }
}
