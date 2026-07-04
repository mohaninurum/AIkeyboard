package com.example.keyboard.system_keyboard

import android.content.Intent
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.media.AudioAttributes
import android.media.SoundPool

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.keyboard/settings"
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        soundPool?.let { pool ->
            soundMap["default"] = pool.load(this, R.raw.click, 1)
            soundMap["mechanical"] = pool.load(this, R.raw.mechanical, 1)
            soundMap["typewriter"] = pool.load(this, R.raw.typewriter, 1)
            soundMap["water_drop"] = pool.load(this, R.raw.water, 1)
            soundMap["wood"] = pool.load(this, R.raw.wood, 1)
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "openKeyboardSettings" -> {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    startActivity(intent)
                    result.success(true)
                }
                "showInputMethodPicker" -> {
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showInputMethodPicker()
                    result.success(true)
                }
                "setTheme" -> {
                    val themeName = call.argument<String>("themeName") ?: "neon_cyan"
                    val sharedPref = getSharedPreferences("KeyboardSettings", android.content.Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("theme", themeName)
                        apply()
                    }
                    result.success(true)
                }
                "setSound" -> {
                    val soundName = call.argument<String>("soundName") ?: "default"
                    val sharedPref = getSharedPreferences("KeyboardSettings", android.content.Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("sound", soundName)
                        apply()
                    }
                    result.success(true)
                }
                "setSoundEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: true
                    val sharedPref = getSharedPreferences("KeyboardSettings", android.content.Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putBoolean("soundEnabled", enabled)
                        apply()
                    }
                    result.success(true)
                }
                "setHapticEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: true
                    val sharedPref = getSharedPreferences("KeyboardSettings", android.content.Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putBoolean("hapticEnabled", enabled)
                        apply()
                    }
                    result.success(true)
                }
                "getSettings" -> {
                    val sharedPref = getSharedPreferences("KeyboardSettings", android.content.Context.MODE_PRIVATE)
                    val map = mapOf(
                        "theme" to (sharedPref.getString("theme", "neon_cyan") ?: "neon_cyan"),
                        "sound" to (sharedPref.getString("sound", "default") ?: "default"),
                        "soundEnabled" to sharedPref.getBoolean("soundEnabled", true),
                        "hapticEnabled" to sharedPref.getBoolean("hapticEnabled", true)
                    )
                    result.success(map)
                }
                "playDemoSound" -> {
                    val soundName = call.argument<String>("soundName") ?: "default"
                    if (soundName != "none") {
                        val soundId = soundMap[soundName] ?: soundMap["default"]
                        if (soundId != null) {
                            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        }
                    }
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
