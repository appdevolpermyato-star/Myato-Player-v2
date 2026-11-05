package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object FloatingPlayerPrefs {
    private const val PREFS_NAME = "myato_floating_player_prefs"
    private const val KEY_ENABLED = "key_floating_enabled"
    private const val KEY_AUTO_START = "key_auto_start"
    private const val KEY_BUBBLE_SIZE = "key_bubble_size_dp"
    private const val KEY_PANEL_SIZE_PERCENT = "key_panel_size_percent"
    private const val KEY_PANEL_OPACITY_PERCENT = "key_panel_opacity_percent"
    private const val KEY_POS_X = "key_pos_x"
    private const val KEY_POS_Y = "key_pos_y"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isAutoStart(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_START, false)
    }

    fun setAutoStart(context: Context, autoStart: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_START, autoStart)
            .apply()
    }

    fun getBubbleSize(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BUBBLE_SIZE, 56)
    }

    fun setBubbleSize(context: Context, sizeDp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BUBBLE_SIZE, sizeDp)
            .apply()
    }

    /**
     * Tamanho do Mini Menu em porcentagem:
     * 70 (Compacto), 80 (Pequeno), 90 (Médio), 100 (Padrão), 110 (Grande), 120 (Extra grande)
     */
    fun getPanelSizePercent(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PANEL_SIZE_PERCENT, 100)
    }

    fun setPanelSizePercent(context: Context, percent: Int) {
        val clamped = percent.coerceIn(70, 120)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PANEL_SIZE_PERCENT, clamped)
            .apply()
    }

    /**
     * Transparência do fundo do Mini Menu em porcentagem de opacidade:
     * 30 (Muito transparente) a 100 (Totalmente opaco)
     */
    fun getPanelOpacityPercent(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PANEL_OPACITY_PERCENT, 100)
    }

    fun setPanelOpacityPercent(context: Context, percent: Int) {
        val clamped = percent.coerceIn(30, 100)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PANEL_OPACITY_PERCENT, clamped)
            .apply()
    }

    fun restoreDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PANEL_SIZE_PERCENT, 100)
            .putInt(KEY_PANEL_OPACITY_PERCENT, 100)
            .apply()
    }

    fun getPanelAlpha(context: Context): Float {
        return getPanelOpacityPercent(context) / 100f
    }

    fun setPanelAlpha(context: Context, alpha: Float) {
        setPanelOpacityPercent(context, (alpha * 100).toInt())
    }

    fun getSavedPosition(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val x = prefs.getInt(KEY_POS_X, 24)
        val y = prefs.getInt(KEY_POS_Y, 200)
        return Pair(x, y)
    }

    fun savePosition(context: Context, x: Int, y: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_POS_X, x)
            .putInt(KEY_POS_Y, y)
            .apply()
    }

    fun resetPosition(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_POS_X, 24)
            .putInt(KEY_POS_Y, 200)
            .apply()
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
