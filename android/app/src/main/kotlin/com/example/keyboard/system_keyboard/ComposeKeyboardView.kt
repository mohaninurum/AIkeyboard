package com.example.keyboard.system_keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class ComposeKeyboardView(context: Context, private val keyPressListener: (String) -> Unit) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        val sharedPref = context.getSharedPreferences("KeyboardSettings", Context.MODE_PRIVATE)
        val themeName = sharedPref.getString("theme", "neon_cyan") ?: "neon_cyan"
        
        var bgColorStr = "#000000"
        var keyBgColorStr = "#333333"
        var glowColorStr = "#00FFFF"
        
        when (themeName) {
            "neon_cyan" -> { bgColorStr = "#000000"; keyBgColorStr = "#333333"; glowColorStr = "#00FFFF" }
            "neon_green" -> { bgColorStr = "#111111"; keyBgColorStr = "#333333"; glowColorStr = "#39FF14" }
            "cyberpunk" -> { bgColorStr = "#2B0033"; keyBgColorStr = "#4B0082"; glowColorStr = "#FF00FF" }
            "blood_red" -> { bgColorStr = "#000000"; keyBgColorStr = "#333333"; glowColorStr = "#FF0000" }
        }

        setBackgroundColor(Color.parseColor(bgColorStr))
        setPadding(0, 16, 0, 16)

        val keys = listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("SHIFT", "Z", "X", "C", "V", "B", "N", "M", "DEL"),
            listOf("NUM", ",", "SPACE", ".", "ENTER")
        )

        for (row in keys) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            }

            for (key in row) {
                val button = Button(context).apply {
                    text = key
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor(keyBgColorStr))
                    
                    val marginParams = LayoutParams(0, LayoutParams.MATCH_PARENT, if (key == "SPACE") 4f else 1f).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    layoutParams = marginParams

                    setOnTouchListener { v, event ->
                        val btn = v as Button
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                btn.setBackgroundColor(android.graphics.Color.parseColor("#111111"))
                                btn.setTextColor(android.graphics.Color.parseColor(glowColorStr))
                                btn.setShadowLayer(30f, 0f, 0f, android.graphics.Color.parseColor(glowColorStr))
                                btn.animate().scaleX(1.15f).scaleY(1.15f).setDuration(80).start()
                            }
                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                btn.setBackgroundColor(android.graphics.Color.parseColor(keyBgColorStr))
                                btn.setTextColor(android.graphics.Color.WHITE)
                                btn.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                                btn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    keyPressListener(key)
                                }
                            }
                        }
                        true
                    }
                }
                rowLayout.addView(button)
            }
            addView(rowLayout)
        }
    }
}
