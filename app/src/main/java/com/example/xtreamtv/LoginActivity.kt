package com.example.xtreamtv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

class LoginActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("xt", MODE_PRIVATE)
        fun field(hint: String, key: String, pwd: Boolean = false) = EditText(this).apply {
            this.hint = hint
            setText(sp.getString(key, ""))
            setSingleLine()
            textSize = 22f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            if (pwd) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val title = TextView(this).apply {
            text = "Xtream TV"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        val s = field("Servidor (http://host:puerto)", "s")
        val u = field("Usuario", "u")
        val p = field("Contraseña", "p", true)
        val btn = Button(this).apply { text = "Entrar"; textSize = 22f }
        btn.setOnClickListener {
            val sv = s.text.toString(); val us = u.text.toString(); val pw = p.text.toString()
            sp.edit().putString("s", sv).putString("u", us).putString("p", pw).apply()
            btn.isEnabled = false
            thread {
                try {
                    val ui = Xtream(sv, us, pw).login().optJSONObject("user_info")
                    val ok = ui?.optString("auth") == "1"
                    runOnUiThread {
                        btn.isEnabled = true
                        if (ok) startActivity(Intent(this, MainActivity::class.java))
                        else Toast.makeText(this, "Credenciales no válidas", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btn.isEnabled = true
                        Toast.makeText(this, "No se pudo conectar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101018.toInt())
            setPadding(220, 80, 220, 80)
            listOf(title, s, u, p, btn).forEach { addView(it) }
        })
    }
}
