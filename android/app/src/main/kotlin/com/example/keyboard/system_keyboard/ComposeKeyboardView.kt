package com.example.keyboard.system_keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout

class ComposeKeyboardView(context: Context, private val keyPressListener: (String) -> Unit) : LinearLayout(context) {

    private val buttons = mutableListOf<Button>()
    private var currentGlowColor = "#00FFFF"
    private var currentKeyBgColor = "#262626"
    private var currentKeyboardBg = "#111111"
    private var currentBorderColor = "#383838"

    private val repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var deleteRunnable: Runnable? = null

    private var isSymbolMode = false
    private var isCaps = false
    private var isEmojiMode = false
    private var isGifMode = false

    private val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    
    private val alphaRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
    )
    
    private val symbolRows = listOf(
        listOf("+", "-", "×", "÷", "=", "/", "_", "€", "£", "¥"),
        listOf("!", "@", "#", "$", "%", "^", "&", "*", "("),
        listOf("⇧", ")", "'", "\"", ":", ";", ",", "?", "⌫")
    )

    private val bottomRow = listOf("!#1", "🌐", "'", "English(India)", ".", "🔍")

    init {
        orientation = VERTICAL
        // Very tight padding like the screenshot
        setPadding(4, 12, 4, 12)
        buildLayout()
    }

    private fun buildLayout() {
        removeAllViews()
        buttons.clear()

        // --- TOOLBAR ---
        val toolbarIcons = listOf("⌨", "✿", "🎫", "🙂", "GIF", "✨", "👤", "+")
        val toolbarLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.8f).apply {
                setMargins(0, 4, 0, 12)
            }
        }
        for (icon in toolbarIcons) {
            val btn = Button(context).apply {
                text = icon
                if (icon == "🙂" && isEmojiMode) {
                    setTextColor(Color.parseColor("#FFCC00")) // Active yellow
                    // Highlight background shape like the screenshot
                    val highlightBg = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#333333"))
                        cornerRadius = 50f
                    }
                    background = highlightBg
                } else if (icon == "GIF" && isGifMode) {
                    setTextColor(Color.WHITE)
                    val highlightBg = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#333333"))
                        cornerRadius = 50f
                    }
                    background = highlightBg
                } else {
                    setTextColor(Color.parseColor("#BBBBBB"))
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                }
                textSize = 18f
                stateListAnimator = null
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(8, 0, 8, 0)
                }
                
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (icon == "🙂") {
                            isEmojiMode = true
                            isGifMode = false
                            buildLayout()
                        } else if (icon == "GIF") {
                            isGifMode = true
                            isEmojiMode = false
                            buildLayout()
                        } else if (icon == "⌨") {
                            isEmojiMode = false
                            isGifMode = false
                            buildLayout()
                        }
                    }
                    true
                }
            }
            toolbarLayout.addView(btn)
        }
        addView(toolbarLayout)

        if (isEmojiMode) {
            buildEmojiLayout()
            return
        }

        if (isGifMode) {
            buildGifLayout()
            return
        }

        val keys = mutableListOf<List<String>>()
        
        // 1. Number Row (Always present)
        keys.add(numberRow)
        
        // 2. Alphabets or Symbols
        if (isSymbolMode) {
            keys.addAll(symbolRows)
        } else {
            val processedAlpha = alphaRows.map { row ->
                row.map { key -> 
                    if (key.length == 1 && isCaps) key.uppercase() else key 
                }
            }
            keys.addAll(processedAlpha)
        }
        
        // 3. Bottom Row
        val modifiedBottom = bottomRow.toMutableList()
        if (isSymbolMode) {
            modifiedBottom[0] = "abc"
        } else {
            modifiedBottom[0] = "!#1"
        }
        keys.add(modifiedBottom)

        for (row in keys) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                // Use weight 1f for equal row height
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    setMargins(0, 4, 0, 4) // Tight row spacing
                }
            }

            for (key in row) {
                val button = Button(context).apply {
                    text = key
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    typeface = Typeface.SANS_SERIF // clean, modern font
                    
                    if (key.length > 1) {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    } else if (key == "⇧") {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    } else {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    }
                    
                    val weight = when (key.lowercase()) {
                        "english(india)", "space" -> 4.5f
                        "🔍", "⇧", "⌫", "!#1", "abc" -> 1.5f
                        else -> 1f
                    }
                    
                    val marginParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                        setMargins(3, 0, 3, 0) // Tight key spacing horizontally
                    }
                    layoutParams = marginParams
                    stateListAnimator = null // Remove default Android shadow
                    setAllCaps(false)
                    setPadding(0, 0, 0, 0) // Remove internal padding to maximize space
                    
                    setPadding(0, 0, 0, 0) // Remove internal padding to maximize space

                    setOnTouchListener { v, event ->
                        val btn = v as Button
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                btn.background = createKeyBackground(currentKeyBgColor, currentGlowColor, isPressed = true)
                                btn.setTextColor(Color.parseColor(currentGlowColor))
                                btn.setShadowLayer(25f, 0f, 0f, Color.parseColor(currentGlowColor))
                                btn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(50).start()
                                
                                if (key == "⌫" || key == "DEL") {
                                    handleInternalKey(key) // Trigger first delete immediately
                                    deleteRunnable = object : Runnable {
                                        override fun run() {
                                            handleInternalKey(key)
                                            repeatHandler.postDelayed(this, 50) // Repeat every 50ms
                                        }
                                    }
                                    repeatHandler.postDelayed(deleteRunnable!!, 400) // Start repeating after 400ms hold
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                btn.background = createKeyBackground(currentKeyBgColor, currentBorderColor, isPressed = false)
                                btn.setTextColor(Color.WHITE)
                                btn.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                                btn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                
                                if (key == "⌫" || key == "DEL") {
                                    deleteRunnable?.let { repeatHandler.removeCallbacks(it) }
                                    deleteRunnable = null
                                } else if (event.action == MotionEvent.ACTION_UP) {
                                    handleInternalKey(key)
                                }
                            }
                        }
                        true
                    }
                }
                buttons.add(button)
                rowLayout.addView(button)
            }
            addView(rowLayout)
        }
        
        updateTheme()
    }

    private fun buildEmojiLayout() {
        // Emoji Category Row
        val categoryIcons = listOf("🔍", "🕒", "🙂", "🐶", "🍴", "🏠", "🏀", "📖", "⁉", "🏳")
        val categoryLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.7f).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        for (icon in categoryIcons) {
            val btn = android.widget.TextView(context).apply {
                text = icon
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#BBBBBB"))
                textSize = 18f
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            categoryLayout.addView(btn)
        }
        addView(categoryLayout)

        // Emoji Grid (5 rows x 8 columns) matching screenshot exactly
        val emojiRows = listOf(
            listOf("😆", "🤪", "👋", "☝", "🏍", "👍", "🎂", "😐"),
            listOf("😂", "🚌", "❤", "😘", "🤭", "💋", "🤔", "😒"),
            listOf("🧡", "😴", "🍵", "🎊", "😅", "😇", "🥰", "🌚"),
            listOf("🥚", "😊", "✅", "🤒", "😍", "📱", "🔑", "🤩"),
            listOf("🥳", "🏡", "🐔", "😌", "🚫", "💯", "🚗", "🇮🇳")
        )

        for (row in emojiRows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            }
            for (emoji in row) {
                val btn = Button(context).apply {
                    text = emoji
                    textSize = 26f
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                    stateListAnimator = null
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                    
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                animate().scaleX(1.3f).scaleY(1.3f).setDuration(50).start()
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                if (event.action == MotionEvent.ACTION_UP) {
                                    keyPressListener(emoji)
                                }
                            }
                        }
                        true
                    }
                }
                rowLayout.addView(btn)
            }
            addView(rowLayout)
        }
    }

    private fun buildGifLayout() {
        val gifOptions = listOf(
            Pair("🐱 Cat Typing GIF", "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif"),
            Pair("😂 Laughing GIF", "https://media.giphy.com/media/13CoXDiaCcCoyk/giphy.gif"),
            Pair("👍 Thumbs Up GIF", "https://media.giphy.com/media/l3vR4CdLInXOhr3O0/giphy.gif"),
            Pair("😮 Wow GIF", "https://media.giphy.com/media/26AHONQ79FdWZhAI0/giphy.gif")
        )

        val scroll = android.widget.ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val title = android.widget.TextView(context).apply {
            text = "Select a GIF to send (Internet required)"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        container.addView(title)

        for (gif in gifOptions) {
            val btn = Button(context).apply {
                text = gif.first
                setTextColor(Color.WHITE)
                background = createKeyBackground("#222222", currentBorderColor, false)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 10, 0, 10)
                }
                
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            background = createKeyBackground(currentKeyBgColor, currentGlowColor, true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            background = createKeyBackground("#222222", currentBorderColor, false)
                            if (event.action == MotionEvent.ACTION_UP) {
                                (context as KeyboardService).sendGif(gif.second)
                            }
                        }
                    }
                    true
                }
            }
            container.addView(btn)
        }

        scroll.addView(container)
        addView(scroll)
    }

    private fun handleInternalKey(key: String) {
        if (key == "!#1" || key == "abc") {
            isSymbolMode = !isSymbolMode
            buildLayout()
            return
        }
        if (key == "⇧" || key == "SHIFT") {
            isCaps = !isCaps
            buildLayout()
            return
        }
        
        // Auto-revert caps after typing one character (if not in symbol mode)
        if (isCaps && key.length == 1 && !isSymbolMode) {
            keyPressListener(key) // Send the typed capital letter
            isCaps = false
            buildLayout() // Revert to small letters
            return
        }

        keyPressListener(key)
    }

    private fun createKeyBackground(bgColor: String, borderColor: String, isPressed: Boolean): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 24f // squarish with rounded corners
        drawable.setColor(Color.parseColor(bgColor))
        
        if (isPressed) {
            drawable.setStroke(6, Color.parseColor(borderColor))
        } else {
            // Persistent border for idle keys matching the theme style
            drawable.setStroke(3, Color.parseColor(borderColor))
        }
        return drawable
    }

    fun updateTheme() {
        val sharedPref = context.getSharedPreferences("KeyboardSettings", Context.MODE_PRIVATE)
        val themeName = sharedPref.getString("theme", "neon_cyan") ?: "neon_cyan"
        
        when (themeName) {
            "neon_cyan" -> { 
                currentKeyboardBg = "#020808"; currentKeyBgColor = "#051A1A"; 
                currentBorderColor = "#005555"; currentGlowColor = "#00FFFF" 
            }
            "neon_green" -> { 
                currentKeyboardBg = "#020802"; currentKeyBgColor = "#0A1A0A"; 
                currentBorderColor = "#115511"; currentGlowColor = "#39FF14" 
            }
            "cyberpunk" -> { 
                // Matches the screenshot exact style
                currentKeyboardBg = "#140518"; currentKeyBgColor = "#260B30"; 
                currentBorderColor = "#4A1A66"; currentGlowColor = "#FF00FF" 
            }
            "blood_red" -> { 
                currentKeyboardBg = "#110000"; currentKeyBgColor = "#220000"; 
                currentBorderColor = "#550000"; currentGlowColor = "#FF0000" 
            }
        }

        setBackgroundColor(Color.parseColor(currentKeyboardBg))
        
        for (btn in buttons) {
            btn.background = createKeyBackground(currentKeyBgColor, currentBorderColor, isPressed = false)
            btn.setTextColor(Color.WHITE)
        }
    }
}
