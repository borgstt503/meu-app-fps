package com.meuapp.fps

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class CursorActivity : Activity() {

    private val pedir = 21

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(i, pedir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pedir && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null && Atalhos.salvarCursor(this, uri)) {
                Atalhos.setI(this, "cursor", 4)
                val d = Intent(this, DesktopActivity::class.java)
                d.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(d)
            } else {
                Toast.makeText(this, "Imagem inválida. Use um PNG.", Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }
}
