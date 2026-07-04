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
import android.media.AudioAttributes
import android.media.SoundPool

class ComposeKeyboardView(context: Context, private val keyPressListener: (String) -> Unit) : LinearLayout(context) {

    private val buttons = mutableListOf<Button>()
    private var currentGlowColor = "#00FFFF"
    private var currentKeyBgColor = "#262626"
    private var currentKeyboardBg = "#111111"
    private var currentBorderColor = "#383838"
    private var currentSound = "default"
    private var isSoundEnabled = true
    private var isHapticEnabled = true

    private val repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var deleteRunnable: Runnable? = null

    private var isSymbolMode = false
    private var isCaps = false
    private var isEmojiMode = false
    private var isGifMode = false
    private var currentEmojiCategory = "🙂"
    
    private var topContainer: android.widget.FrameLayout? = null
    private var toolbarLayoutContainer: LinearLayout? = null
    private var suggestionContainer: LinearLayout? = null

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()

    private var keyPreviewPopup: android.widget.PopupWindow? = null
    private var keyPreviewText: android.widget.TextView? = null



    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA) // Use media volume to bypass strict system sound restrictions
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        soundPool?.let { pool ->
            soundMap["default"] = pool.load(context, R.raw.click, 1)
            soundMap["mechanical"] = pool.load(context, R.raw.mechanical, 1)
            soundMap["typewriter"] = pool.load(context, R.raw.typewriter, 1)
            soundMap["water_drop"] = pool.load(context, R.raw.water, 1)
            soundMap["wood"] = pool.load(context, R.raw.wood, 1)
        }
    }
    
    fun updateSuggestions(words: List<String>) {
        if (words.isEmpty()) {
            suggestionContainer?.parent?.let { (it as android.view.View).visibility = android.view.View.GONE }
            toolbarLayoutContainer?.visibility = android.view.View.VISIBLE
            return
        }
        
        toolbarLayoutContainer?.visibility = android.view.View.GONE
        suggestionContainer?.parent?.let { (it as android.view.View).visibility = android.view.View.VISIBLE }
        
        suggestionContainer?.removeAllViews()
        for (word in words) {
            val btn = Button(context).apply {
                text = word
                textSize = 16f
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                setPadding(40, 0, 40, 0)
                minWidth = 0
                minimumWidth = 0
                isAllCaps = false
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
                
                setOnClickListener {
                    (context as KeyboardService).commitSuggestion(word)
                }
            }
            suggestionContainer?.addView(btn)
            
            // Separator
            val sep = android.view.View(context).apply {
                layoutParams = LayoutParams(2, LayoutParams.MATCH_PARENT).apply { setMargins(10, 20, 10, 20) }
                setBackgroundColor(Color.parseColor("#444444"))
            }
            suggestionContainer?.addView(sep)
        }
    }

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

    private val bottomRow = listOf("!#1", "🌐", "'", "English(India)", ".", "﹀", "🔍")

    init {
        orientation = VERTICAL
        initSoundPool()
        // Very tight padding like the screenshot
        setPadding(4, 12, 4, 12)
        buildLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Enforce a specific, taller height for the entire keyboard
        val desiredHeightDp = 320f
        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, desiredHeightDp, resources.displayMetrics).toInt()
        val customHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, customHeightMeasureSpec)
    }

    fun setAutoCaps(caps: Boolean) {
        if (isCaps != caps && !isEmojiMode && !isGifMode && !isSymbolMode) {
            isCaps = caps
            updateCapsUI()
        }
    }

    private fun updateCapsUI() {
        for (btn in buttons) {
            val text = btn.text.toString()
            if (text.length == 1 && text[0].isLetter() && text != "⇧" && text != "⌫") {
                btn.text = if (isCaps) text.uppercase() else text.lowercase()
            } else if (text == "⇧") {
                if (isCaps) {
                    btn.setTextColor(Color.parseColor(currentGlowColor))
                } else {
                    btn.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun buildLayout() {
        removeAllViews()
        buttons.clear()

        // --- TOP BAR (Toolbar + Suggestions) ---
        topContainer = android.widget.FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.8f).apply {
                setMargins(0, 4, 0, 12)
            }
        }
        
        val toolbarIcons = listOf("⌨", "✿", "🎫", "🙂", "GIF", "✨", "👤", "+")
        toolbarLayoutContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        
        val suggestionScroll = android.widget.HorizontalScrollView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            isHorizontalScrollBarEnabled = false
            visibility = android.view.View.GONE // Hidden by default
        }
        suggestionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        suggestionScroll.addView(suggestionContainer)
        
        topContainer?.addView(toolbarLayoutContainer)
        topContainer?.addView(suggestionScroll)
        addView(topContainer)
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
                setPadding(0, 0, 0, 0)
                minWidth = 0
                minimumWidth = 0
                isAllCaps = false
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
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
            toolbarLayoutContainer?.addView(btn)
        }

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
                        "english(india)", "space" -> 4.0f
                        "🔍", "⇧", "⌫", "!#1", "abc" -> 1.5f
                        "🌐", "﹀" -> 1.0f
                        else -> 1f
                    }
                    
                    val marginParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                        setMargins(3, 0, 3, 0) // Tight key spacing horizontally
                    }
                    layoutParams = marginParams
                    stateListAnimator = null // Remove default Android shadow
                    setAllCaps(false)
                    setPadding(0, 0, 0, 0) // Remove internal padding to maximize space
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0

                    setOnTouchListener { v, event ->
                        val btn = v as Button
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // Standard Haptic & Sound
                                if (isHapticEnabled) {
                                    btn.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                playKeySound(btn)
                                
                                btn.background = createKeyBackground(currentKeyBgColor, currentGlowColor, isPressed = true)
                                btn.setTextColor(Color.parseColor(currentGlowColor))
                                btn.setShadowLayer(25f, 0f, 0f, Color.parseColor(currentGlowColor))
                                btn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(50).start()
                                showKeyPreview(btn)
                                
                                if (key == "⌫" || key == "DEL") {
                                    handleInternalKey(btn.text.toString()) // Trigger first delete immediately
                                    deleteRunnable = object : Runnable {
                                        override fun run() {
                                            handleInternalKey(btn.text.toString())
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
                                hideKeyPreview()
                                
                                if (key == "⌫" || key == "DEL") {
                                    deleteRunnable?.let { repeatHandler.removeCallbacks(it) }
                                    deleteRunnable = null
                                } else if (event.action == MotionEvent.ACTION_UP) {
                                    handleInternalKey(btn.text.toString())
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
        val categoryIcons = listOf("🕒", "🙂", "🐶", "🍴", "🏠", "🏀", "📖", "⁉", "🏳")
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
                if (icon == currentEmojiCategory) {
                    setTextColor(Color.parseColor("#FFCC00"))
                    val highlightBg = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#333333"))
                        cornerRadius = 20f
                    }
                    background = highlightBg
                } else {
                    setTextColor(Color.parseColor("#BBBBBB"))
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                }
                textSize = 18f
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        currentEmojiCategory = icon
                        buildLayout()
                    }
                    true
                }
            }
            categoryLayout.addView(btn)
        }
        addView(categoryLayout)

        val emojiMap = mapOf(
            "🕒" to listOf("👍", "❤", "😂", "😘", "🙏", "🔥", "😊", "✨", "💯", "😍", "🎉", "👌", "👏", "🙌", "😁", "😎"),
            "🙂" to listOf("😆", "🤪", "👋", "☝", "🏍", "👍", "🎂", "😐", "😂", "🚌", "❤", "😘", "🤭", "💋", "🤔", "😒", "🧡", "😴", "🍵", "🎊", "😅", "😇", "🥰", "🌚", "🥚", "😊", "✅", "🤒", "😍", "📱", "🔑", "🤩", "🥳", "🏡", "🐔", "😌", "🚫", "💯", "🚗", "🇮🇳", "😁", "😉", "😎", "😜", "😏", "😷", "🤠", "🥺"),
            "🐶" to listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜"),
            "🍴" to listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈", "🍒", "🍑", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥒", "🌶", "🌽", "🥕", "🥔", "🍠", "🥐", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🥞", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕"),
            "🏠" to listOf("🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽", "⛪", "🕌", "🕍", "⛩", "🕋", "⛲", "⛺", "🌁", "🌃", "🏙", "🌄", "🌅", "🌆", "🌇", "🌉", "♨", "🌌", "🎠", "🎡", "🎢", "💈", "🎪"),
            "🏀" to listOf("⚽", "⚾", "🏀", "🏐", "🏈", "🏉", "🎾", "🎳", "🏏", "🏑", "🏒", "🥍", "🏓", "🏸", "🥊", "🥋", "🥅", "⛳", "⛸", "🎣", "🎽", "🎿", "🛷", "🥌", "🎯", "🎱", "🔮", "🧿", "🎮", "🕹", "🎰", "🎲", "🧩", "🧸", "♠", "♥", "♦", "♣", "♟", "🃏"),
            "📖" to listOf("⌚", "📱", "📲", "💻", "⌨", "🖥", "🖨", "🖱", "🖲", "🕹", "🗜", "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥", "📽", "🎞", "📞", "☎", "📟", "📠", "📺", "📻", "🎙", "🎚", "🎛", "🧭", "⏱", "⏲", "⏰", "🕰", "⌛", "⏳", "📡", "🔋", "🔌"),
            "⁉" to listOf("❤", "🧡", "💛", "💚", "💙", "💜", "🖤", "💔", "❣", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮", "✝", "☪", "🕉", "☸", "✡", "🔯", "🕎", "☯", "☦", "🛐", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "♓"),
            "🏳" to listOf("🏁", "🚩", "🎌", "🏴", "🏳", "🏳️‍🌈", "🏴‍☠️", "🇦🇫", "🇦🇽", "🇦🇱", "🇩🇿", "🇦🇸", "🇦🇩", "🇦🇴", "🇦🇮", "🇦🇶", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇼", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪", "🇧🇿", "🇧🇯", "🇧🇲", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇮🇴", "🇻🇬")
        )

        val currentEmojis = emojiMap[currentEmojiCategory] ?: emojiMap["🙂"]!!

        val scroll = android.widget.ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 5f)
        }
        
        val gridContainer = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        val emojiRows = currentEmojis.chunked(8)
        for (row in emojiRows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 8, 0, 8)
                }
            }
            
            for (emoji in row) {
                val btn = Button(context).apply {
                    text = emoji
                    textSize = 28f
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                    stateListAnimator = null
                    setPadding(0, 0, 0, 0)
                    minWidth = 0
                    minimumWidth = 0
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                    
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (isHapticEnabled) {
                                    v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                playKeySound(v)
                                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(50).start()
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
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
            
            if (row.size < 8) {
                val missing = 8 - row.size
                for (i in 0 until missing) {
                    val empty = android.view.View(context).apply {
                        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                    }
                    rowLayout.addView(empty)
                }
            }
            
            gridContainer.addView(rowLayout)
        }
        
        scroll.addView(gridContainer)
        addView(scroll)
    }

    private fun buildGifLayout() {
        val gifOptions = listOf(
            "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
            "https://media.giphy.com/media/13CoXDiaCcCoyk/giphy.gif",
            "https://media.giphy.com/media/l3vR4CdLInXOhr3O0/giphy.gif",
            "https://media.giphy.com/media/26AHONQ79FdWZhAI0/giphy.gif",
            "https://media.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy.gif",
            "https://media.giphy.com/media/xT0xeJpnrWC4XWblWQ/giphy.gif"
        )

        val mainContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 5f)
        }

        val scroll = android.widget.ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(10, 10, 10, 10)
        }

        val rows = gifOptions.chunked(2)
        for (rowItems in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 350).apply {
                    setMargins(0, 0, 0, 10)
                }
            }
            
            for (gif in rowItems) {
                val frame = android.widget.FrameLayout(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                        setMargins(10, 0, 10, 0)
                    }
                    
                    val imageView = android.widget.ImageView(context).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        
                        // Use Glide to load GIF efficiently and reliably
                        com.bumptech.glide.Glide.with(context)
                            .asGif()
                            .load(gif)
                            .into(this)
                    }
                    
                    val overlay = android.view.View(context).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                        isClickable = true
                        
                        setOnTouchListener { _, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    setBackgroundColor(Color.parseColor("#44FFFFFF"))
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    setBackgroundColor(Color.TRANSPARENT)
                                    if (event.action == MotionEvent.ACTION_UP) {
                                        android.widget.Toast.makeText(context, "Sending GIF...", android.widget.Toast.LENGTH_SHORT).show()
                                        (context as KeyboardService).sendGif(gif)
                                    }
                                }
                            }
                            true
                        }
                    }
                    
                    addView(imageView)
                    addView(overlay)
                }
                rowLayout.addView(frame)
            }
            
            // Fill empty space if row has only 1 item
            if (rowItems.size == 1) {
                val empty = android.view.View(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                }
                rowLayout.addView(empty)
            }
            
            container.addView(rowLayout)
        }

        scroll.addView(container)
        mainContainer.addView(scroll)

        // Add bottom bar with Back Button
        val bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 10, 0, 20)
            }
        }
        
        val backBtn = Button(context).apply {
            text = "⇦ Back to Keyboard"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = createKeyBackground(currentKeyBgColor, currentBorderColor, false)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setPadding(60, 20, 60, 20)
            }
            
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.background = createKeyBackground(currentKeyBgColor, currentGlowColor, true)
                        if (isHapticEnabled) {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        playKeySound(v)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.background = createKeyBackground(currentKeyBgColor, currentBorderColor, false)
                        if (event.action == MotionEvent.ACTION_UP) {
                            isGifMode = false
                            buildLayout()
                        }
                    }
                }
                true
            }
        }
        bottomBar.addView(backBtn)
        mainContainer.addView(bottomBar)

        addView(mainContainer)
    }

    private fun handleInternalKey(key: String) {
        if (key == "!#1" || key == "abc") {
            isSymbolMode = !isSymbolMode
            buildLayout()
            return
        }
        if (key == "⇧" || key == "SHIFT") {
            isCaps = !isCaps
            updateCapsUI()
            return
        }
        
        // Auto-revert caps after typing one character (if not in symbol mode)
        if (isCaps && key.length == 1 && !isSymbolMode) {
            keyPressListener(key) // Send the typed capital letter
            isCaps = false
            updateCapsUI() // Revert to small letters
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

    private fun playKeySound(v: android.view.View) {
        if (!isSoundEnabled || currentSound == "none") return
        
        val soundId = soundMap[currentSound] ?: soundMap["default"]
        if (soundId != null) {
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    private fun showKeyPreview(btn: Button) {
        val text = btn.text.toString()
        if (text.length > 1 || text == "⇧" || text == "⌫" || text == "🌐" || text == "🔍" || text == "﹀") return 

        if (keyPreviewPopup == null) {
            keyPreviewText = android.widget.TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 36f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
            keyPreviewPopup = android.widget.PopupWindow(keyPreviewText, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                isTouchable = false
                isClippingEnabled = false
                elevation = 15f
            }
        }

        keyPreviewText?.text = text
        val popupBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor(currentKeyBgColor))
            setStroke(2, Color.parseColor(currentBorderColor))
        }
        keyPreviewText?.background = popupBg
        
        val popupWidth = (btn.width * 1.5).toInt()
        val popupHeight = (btn.height * 1.8).toInt()
        
        keyPreviewPopup?.width = popupWidth
        keyPreviewPopup?.height = popupHeight

        val loc = IntArray(2)
        btn.getLocationInWindow(loc)
        
        val x = loc[0] - (popupWidth - btn.width) / 2
        val y = loc[1] - popupHeight + (btn.height / 3)

        if (keyPreviewPopup?.isShowing == true) {
            keyPreviewPopup?.update(x, y, popupWidth, popupHeight)
            // If it was animating to dismiss, cancel it and restore
            keyPreviewText?.animate()?.cancel()
            keyPreviewText?.alpha = 1f
            keyPreviewText?.scaleX = 1f
            keyPreviewText?.scaleY = 1f
        } else {
            keyPreviewText?.alpha = 0f
            keyPreviewText?.scaleX = 0.5f
            keyPreviewText?.scaleY = 0.5f
            keyPreviewPopup?.showAtLocation(btn, Gravity.NO_GRAVITY, x, y)
            
            keyPreviewText?.animate()?.cancel()
            keyPreviewText?.animate()
                ?.alpha(1f)
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(100)
                ?.setInterpolator(android.view.animation.OvershootInterpolator())
                ?.withEndAction(null)
                ?.start()
        }
    }

    private fun hideKeyPreview() {
        if (keyPreviewPopup?.isShowing == true) {
            keyPreviewText?.animate()?.cancel()
            keyPreviewText?.animate()
                ?.alpha(0f)
                ?.scaleX(0.8f)
                ?.scaleY(0.8f)
                ?.setDuration(100)
                ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                ?.withEndAction {
                    keyPreviewPopup?.dismiss()
                }
                ?.start()
        }
    }

    fun updateTheme() {
        val sharedPref = context.getSharedPreferences("KeyboardSettings", Context.MODE_PRIVATE)
        val themeName = sharedPref.getString("theme", "neon_cyan") ?: "neon_cyan"
        currentSound = sharedPref.getString("sound", "default") ?: "default"
        isSoundEnabled = sharedPref.getBoolean("soundEnabled", true)
        isHapticEnabled = sharedPref.getBoolean("hapticEnabled", true)
        
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
