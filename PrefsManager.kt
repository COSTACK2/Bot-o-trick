package com.example.dpitrick

import android.content.Context

/**
 * Guarda as preferências do usuário (valores de DPI e se o modo Trick
 * está ativo) usando SharedPreferences, para que fiquem salvas mesmo
 * depois de fechar e reabrir o app.
 */
object PrefsManager {

    private const val PREFS_NAME = "dpi_trick_prefs"
    private const val KEY_DPI_ALTO = "dpi_alto"
    private const val KEY_DPI_NORMAL = "dpi_normal"
    private const val KEY_TRICK_ATIVO = "trick_ativo"

    fun getDpiAlto(context: Context, default: Int = 480): Int =
        prefs(context).getInt(KEY_DPI_ALTO, default)

    fun setDpiAlto(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_DPI_ALTO, value).apply()
    }

    fun getDpiNormal(context: Context, default: Int): Int =
        prefs(context).getInt(KEY_DPI_NORMAL, default)

    fun setDpiNormal(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_DPI_NORMAL, value).apply()
    }

    fun isTrickAtivo(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRICK_ATIVO, false)

    fun setTrickAtivo(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_TRICK_ATIVO, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
