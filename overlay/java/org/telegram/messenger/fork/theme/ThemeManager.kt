package org.telegram.messenger.fork.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * Nawmagram Theme Manager
 * Deep Space dark color system with Material You support.
 */
object ThemeManager {

    private const val PREFS_NAME = "nawmagram_theme"
    private const val KEY_THEME = "current_theme"

    val DEEP_SPACE = ThemeConfig(
        name = "Deep Space",
        background = Color.parseColor("#0A0E1A"),
        surface = Color.parseColor("#111827"),
        elevated = Color.parseColor("#1C2436"),
        accentPrimary = Color.parseColor("#6C63FF"),
        accentSecondary = Color.parseColor("#FF6B9D"),
        textPrimary = Color.parseColor("#F0F4FF"),
        textSecondary = Color.parseColor("#8892A4"),
        bubbleOutStart = Color.parseColor("#6C63FF"),
        bubbleOutEnd = Color.parseColor("#9333EA"),
        bubbleIn = Color.parseColor("#1C2436"),
        onlineIndicator = Color.parseColor("#06D6A0")
    )

    val MIDNIGHT = ThemeConfig(
        name = "Midnight",
        background = Color.parseColor("#050A15"),
        surface = Color.parseColor("#0D1626"),
        elevated = Color.parseColor("#142035"),
        accentPrimary = Color.parseColor("#3B82F6"),
        accentSecondary = Color.parseColor("#60A5FA"),
        textPrimary = Color.parseColor("#E8F0FF"),
        textSecondary = Color.parseColor("#6B7A99"),
        bubbleOutStart = Color.parseColor("#1D4ED8"),
        bubbleOutEnd = Color.parseColor("#3B82F6"),
        bubbleIn = Color.parseColor("#142035"),
        onlineIndicator = Color.parseColor("#34D399")
    )

    val NEON_PURPLE = ThemeConfig(
        name = "Neon Purple",
        background = Color.parseColor("#0D0A1A"),
        surface = Color.parseColor("#160F2E"),
        elevated = Color.parseColor("#1E1442"),
        accentPrimary = Color.parseColor("#A855F7"),
        accentSecondary = Color.parseColor("#EC4899"),
        textPrimary = Color.parseColor("#F5F0FF"),
        textSecondary = Color.parseColor("#9277B0"),
        bubbleOutStart = Color.parseColor("#7C3AED"),
        bubbleOutEnd = Color.parseColor("#A855F7"),
        bubbleIn = Color.parseColor("#1E1442"),
        onlineIndicator = Color.parseColor("#A3E635")
    )

    val ALL_THEMES = listOf(DEEP_SPACE, MIDNIGHT, NEON_PURPLE)
    private var currentTheme = DEEP_SPACE
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, DEEP_SPACE.name)
        currentTheme = ALL_THEMES.find { it.name == saved } ?: DEEP_SPACE
    }

    fun get(): ThemeConfig = currentTheme

    fun set(theme: ThemeConfig) {
        currentTheme = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun tint(base: Int, delta: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        hsv[2] = (hsv[2] + delta).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }
}

data class ThemeConfig(
    val name: String,
    val background: Int,
    val surface: Int,
    val elevated: Int,
    val accentPrimary: Int,
    val accentSecondary: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val bubbleOutStart: Int,
    val bubbleOutEnd: Int,
    val bubbleIn: Int,
    val onlineIndicator: Int
)
