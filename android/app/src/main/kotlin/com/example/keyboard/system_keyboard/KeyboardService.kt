package com.example.keyboard.system_keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection

class KeyboardService : InputMethodService() {

    private var isCaps = false

    override fun onCreateInputView(): View {
        val keyboardView = ComposeKeyboardView(this) { key ->
            handleKey(key)
        }
        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            800 // default height for now
        )
        keyboardView.layoutParams = layoutParams
        return keyboardView
    }

    private fun handleKey(key: String) {
        val ic: InputConnection? = currentInputConnection
        if (ic == null) return

        when (key) {
            "DEL" -> {
                ic.deleteSurroundingText(1, 0)
            }
            "ENTER" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
            }
            "SHIFT" -> {
                isCaps = !isCaps
                // A full implementation would update the view to show caps
            }
            "NUM" -> {
                // Toggle numeric keyboard
            }
            else -> {
                var charToAdd = key
                if (!isCaps) {
                    charToAdd = charToAdd.lowercase()
                }
                ic.commitText(charToAdd, 1)
            }
        }
    }
}
