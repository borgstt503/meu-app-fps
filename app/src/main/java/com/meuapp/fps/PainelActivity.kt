package com.meuapp.fps

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager

class PainelActivity : Activity() {

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Painel.build(this) { finish() })

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
    }
}
