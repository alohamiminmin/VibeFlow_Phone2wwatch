package com.example.myvibrationproject

import android.content.Context
import android.content.SharedPreferences

object AppVibeSettings {

    private const val PREF_NAME = "app_vibe_settings"
    private const val PREF_CUSTOM_PATTERN = "custom_pattern_"
    private const val PREF_CUSTOM_AMPLITUDE = "custom_amplitude_"
    private const val PREF_CANDIDATES = "candidates"

    fun setPattern(context: Context, packageName: String, pattern: VibePattern) {
        getPrefs(context).edit().putString(packageName, pattern.name).apply()
    }

    fun getPattern(context: Context, packageName: String): VibePattern? {
        val name = getPrefs(context).getString(packageName, null) ?: return null
        return runCatching { VibePattern.valueOf(name) }.getOrNull()
    }

    fun getAllSettings(context: Context): Map<String, VibePattern> {
        return getPrefs(context).all
            .filter { (key, _) ->
                !key.startsWith(PREF_CUSTOM_PATTERN) &&
                        !key.startsWith(PREF_CUSTOM_AMPLITUDE) &&
                        key != PREF_CANDIDATES
            }
            .mapNotNull { (pkg, value) ->
                if (value !is String) return@mapNotNull null
                val pattern = runCatching {
                    VibePattern.valueOf(value)
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

    fun setCustomPattern(context: Context, packageName: String, pattern: String) {
        getPrefs(context).edit()
            .putString(PREF_CUSTOM_PATTERN + packageName, pattern).apply()
    }

    fun getCustomPattern(context: Context, packageName: String): String? {
        return getPrefs(context).getString(PREF_CUSTOM_PATTERN + packageName, null)
    }

    fun setCustomAmplitude(context: Context, packageName: String, amplitude: String) {
        getPrefs(context).edit()
            .putString(PREF_CUSTOM_AMPLITUDE + packageName, amplitude).apply()
    }

    fun getCustomAmplitude(context: Context, packageName: String): String? {
        return getPrefs(context).getString(PREF_CUSTOM_AMPLITUDE + packageName, null)
    }

    fun addCandidate(context: Context, packageName: String) {
        val current = getCandidates(context).toMutableSet()
        current.add(packageName)
        getPrefs(context).edit()
            .putStringSet(PREF_CANDIDATES, current)
            .apply()
    }

    fun getCandidates(context: Context): Set<String> {
        return getPrefs(context).getStringSet(PREF_CANDIDATES, emptySet()) ?: emptySet()
    }

    fun removeCandidate(context: Context, packageName: String) {
        val current = getCandidates(context).toMutableSet()
        current.remove(packageName)
        getPrefs(context).edit()
            .putStringSet(PREF_CANDIDATES, current)
            .apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}