package com.example.keyboard.system_keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection

class KeyboardService : InputMethodService() {

    private lateinit var keyboardView: ComposeKeyboardView
    private var lastSpaceTime: Long = 0
    private var currentComposingWord = ""
    
    // Combined dictionary for English, Hinglish, Nimadi, and Malvi
    private val dictionary = listOf(
        // English
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I", 
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "hello", "how", "are", "good", "morning", "night", "yes", "no", "ok", "okay",
        "thanks", "thank", "please", "sorry", "love", "much", "time", "can", "know",
        
        // Hindi / Hinglish
        "hi", "bro", "bhai", "kya", "kar", "raha", "hai", "kaise", "ho", "aaj",
        "kal", "mera", "tera", "nahi", "haan", "yaar", "chal", "kaha", "idhar", 
        "udhar", "jaldi", "aao", "karo", "mujhe", "tujhe", "hum", "tum", "aap", "baat",
        
        // Nimadi & Malvi (Regional)
        "kai", "kathe", "jaano", "aayo", "choro", "chori", "kha", "pivo", "bhari", 
        "motiyar", "dikra", "kahu", "padio", "jaye", "karto", "kay", "katha", 
        "jana", "aana", "bhaya", "dada", "bhiya", "bataw", "riyo", "he", "ni", 
        "tharo", "maro", "hove", "aai", "gai", "dikro" "kar" "rayu"
    )

    override fun onCreateInputView(): View {
        keyboardView = ComposeKeyboardView(this) { key ->
            handleKey(key)
        }
        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            800 // default height for now
        )
        keyboardView.layoutParams = layoutParams
        return keyboardView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (this::keyboardView.isInitialized) {
            keyboardView.updateTheme()
        }
    }

    private fun handleKey(key: String) {
        val ic: InputConnection? = currentInputConnection
        if (ic == null) return

        when (key) {
            "⌫", "DEL" -> {
                ic.deleteSurroundingText(1, 0)
            }
            "🔍", "ENTER", "↵" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            "SPACE", "English(India)" -> {
                val now = System.currentTimeMillis()
                if (now - lastSpaceTime < 500) {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    lastSpaceTime = 0
                } else {
                    ic.commitText(" ", 1)
                    lastSpaceTime = now
                }
            }
            "⇧", "SHIFT", "abc", "!#1" -> {
                // Handled internally by ComposeKeyboardView
            }
            "🌐" -> {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
            "﹀" -> {
                requestHideSelf(0)
            }
            else -> {
                ic.commitText(key, 1)
            }
        }
        
        updateCapsState(ic)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        currentInputConnection?.let { 
            updateCapsState(it)
            checkSuggestions(it)
        }
    }

    private fun checkSuggestions(ic: InputConnection) {
        if (!this::keyboardView.isInitialized) return
        
        val textBefore = ic.getTextBeforeCursor(20, 0) ?: ""
        // Get the last alphabetical word before cursor
        val lastWordMatch = Regex("([a-zA-Z]+)$").find(textBefore)
        
        if (lastWordMatch != null) {
            currentComposingWord = lastWordMatch.value
            val prefix = currentComposingWord.lowercase()
            
            // Generate suggestions matching the prefix (but not the exact prefix itself)
            val suggestions = dictionary
                .filter { it.lowercase().startsWith(prefix) && it.lowercase() != prefix }
                .take(3)
                
            // Capitalize suggestion if user is typing with caps
            val formattedSuggestions = suggestions.map { word ->
                if (currentComposingWord[0].isUpperCase()) {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                } else {
                    word
                }
            }
            keyboardView.updateSuggestions(formattedSuggestions)
        } else {
            currentComposingWord = ""
            keyboardView.updateSuggestions(emptyList())
        }
    }

    fun commitSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentComposingWord.length, 0)
        }
        ic.commitText("$word ", 1)
        currentComposingWord = ""
        keyboardView.updateSuggestions(emptyList())
    }

    private fun updateCapsState(ic: InputConnection) {
        if (!this::keyboardView.isInitialized) return
        
        val textBefore = ic.getTextBeforeCursor(2, 0) ?: ""
        val shouldCaps = textBefore.isEmpty() || textBefore.endsWith(". ") || textBefore.endsWith("? ") || textBefore.endsWith("! ") || textBefore.endsWith("\n")
        keyboardView.setAutoCaps(shouldCaps)
    }

    fun sendGif(gifUrl: String) {
        val ic = currentInputConnection ?: return
        
        Thread {
            try {
                // Download GIF
                val url = java.net.URL(gifUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                
                // Save to cache
                val dir = java.io.File(cacheDir, "gifs")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "keyboard_gif_${System.currentTimeMillis()}.gif")
                val outputStream = java.io.FileOutputStream(file)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()
                
                // Commit Content on Main Thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "com.example.keyboard.fileprovider",
                        file
                    )
                    
                    val description = android.content.ClipDescription("GIF", arrayOf("image/gif"))
                    val info = androidx.core.view.inputmethod.InputContentInfoCompat(uri, description, null)
                    
                    val success = androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
                        ic,
                        currentInputEditorInfo,
                        info,
                        androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                        null
                    )
                    
                    if (!success) {
                        android.widget.Toast.makeText(this, "GIF not supported in this text field", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(this, "Failed to load GIF", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
