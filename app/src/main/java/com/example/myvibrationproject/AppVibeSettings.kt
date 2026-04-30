package com.example.myvibrationproject

import android.content.Context
import android.content.SharedPreferences

object AppVibeSettings {

    private const val PREF_NAME = "app_vibe_settings"
    private const val PREF_CUSTOM_PATTERN = "custom_pattern_"
    private const val PREF_CUSTOM_AMPLITUDE = "custom_amplitude_"

    fun setPattern(context: Context, packageName: String, pattern: VibePattern) {
        getPrefs(context).edit().putString(packageName, pattern.name).apply()
    }

    fun getPattern(context: Context, packageName: String): VibePattern? {
        val name = getPrefs(context).getString(packageName, null) ?: return null
        return runCatching { VibePattern.valueOf(name) }.getOrNull()
    }

    fun getAllSettings(context: Context): Map<String, VibePattern> {
        return getPrefs(context).all
            .filter { !it.key.startsWith(PREF_CUSTOM_PATTERN)
                    && !it.key.startsWith(PREF_CUSTOM_AMPLITUDE) }
            .mapNotNull { (pkg, value) ->
                val pattern = runCatching {
                    VibePattern.valueOf(value as String)
                }.getOrNull() ?: return@mapNotNull null
                pkg to pattern
            }.toMap()
    }

    fun removeApp(context: Context, packageName: String) {
        getPrefs(context).edit()
            .remove(packageName)
            .remove(PREF_CUSTOM_PATTERN + packageName)
            .remove(PREF_CUSTOM_AMPLITUDE + packageName)
            .apply()
    }

    // カスタムパターン（ms配列を文字列で保存）
    fun setCustomPattern(context: Context, packageName: String, pattern: String) {
        getPrefs(context).edit()
            .putString(PREF_CUSTOM_PATTERN + packageName, pattern).apply()
    }

    fun getCustomPattern(context: Context, packageName: String): String? {
        return getPrefs(context).getString(PREF_CUSTOM_PATTERN + packageName, null)
    }

    // カスタム強度（0〜255の配列を文字列で保存）
    fun setCustomAmplitude(context: Context, packageName: String, amplitude: String) {
        getPrefs(context).edit()
            .putString(PREF_CUSTOM_AMPLITUDE + packageName, amplitude).apply()
    }

    fun getCustomAmplitude(context: Context, packageName: String): String? {
        return getPrefs(context).getString(PREF_CUSTOM_AMPLITUDE + packageName, null)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}