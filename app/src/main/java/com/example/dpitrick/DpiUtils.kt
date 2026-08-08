package com.example.dpitrick

import android.content.Context
import android.provider.Settings
import android.util.Log

object DpiUtils {

    private const val TAG = "DpiUtils"
    private const val CHAVE_DENSIDADE = "display_density_forced"

    fun getCurrentDensity(context: Context): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, CHAVE_DENSIDADE)
        } catch (e: Settings.SettingNotFoundException) {
            context.resources.displayMetrics.densityDpi
        }
    }

    fun setDensity(context: Context, dpi: Int): Boolean {
        val viaSettings = try {
            Settings.Global.putString(context.contentResolver, CHAVE_DENSIDADE, dpi.toString())
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem permissão WRITE_SECURE_SETTINGS: ${e.message}")
            false
        }
        if (viaSettings) return true

        return runShellCommand("wm density $dpi")
    }

    fun resetDensity(context: Context): Boolean {
        val viaSettings = try {
            Settings.Global.putString(context.contentResolver, CHAVE_DENSIDADE, null)
            true
        } catch (e: SecurityException) {
            false
        }
        if (viaSettings) return true

        return runShellCommand("wm density reset")
    }

    private fun runShellCommand(command: String): Boolean {
        try {
            val processo = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            if (processo.waitFor() == 0) return true
        } catch (e: Exception) {
            Log.w(TAG, "Root indisponível para: $command")
        }

        return try {
            val processo = Runtime.getRuntime().exec(command)
            processo.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao executar comando: $command", e)
            false
        }
    }
}
