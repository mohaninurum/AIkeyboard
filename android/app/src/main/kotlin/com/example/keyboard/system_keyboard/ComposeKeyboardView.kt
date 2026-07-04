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
        setBackgroundColor(Color.parseColor("#1E1E1E"))
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
                    setBackgroundColor(Color.parseColor("#333333"))
                    
                    val marginParams = LayoutParams(0, LayoutParams.MATCH_PARENT, if (key == "SPACE") 4f else 1f).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    layoutParams = marginParams

                    setOnClickListener {
                        keyPressListener(key)
                    }
                }
                rowLayout.addView(button)
            }
            addView(rowLayout)
        }
    }
}
