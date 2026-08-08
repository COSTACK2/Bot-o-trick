package com.example.dpitrick

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Funções utilitárias para ler e alterar o DPI (densidade de tela) do sistema.
 *
 * IMPORTANTE: alterar o DPI do sistema é uma operação protegida pelo Android.
 * Este utilitário tenta dois caminhos, nesta ordem:
 *
 *   1) Settings.Global "display_density_forced" — funciona apenas se o app
 *      tiver a permissão WRITE_SECURE_SETTINGS concedida via ADB
 *      (não é possível conceder isso só pela tela de Configurações).
 *
 *   2) Comando de shell "wm density" — funciona apenas em dispositivos
 *      com acesso root (binário su disponível).
 *
 * Se nenhum dos dois estiver disponível, a alteração falha e o app avisa
 * o usuário (veja OverlayService).
 */
object DpiUtils {

    private const val TAG = "DpiUtils"
    private const val CHAVE_DENSIDADE = "display_density_forced"

    /** Retorna o DPI atualmente aplicado (forçado, se houver, ou o de fábrica). */
    fun getCurrentDensity(context: Context): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, CHAVE_DENSIDADE)
        } catch (e: Settings.SettingNotFoundException) {
            context.resources.displayMetrics.densityDpi
        }
    }

    /** Tenta aplicar um novo valor de DPI. Retorna true se algum método funcionou. */
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

    /** Remove o DPI forçado, voltando ao valor de fábrica do aparelho. */
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
        // Primeiro tenta via root (su)
        try {
            val processo = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            if (processo.waitFor() == 0) return true
        } catch (e: Exception) {
            Log.w(TAG, "Root indisponível para: $command")
        }

        // Depois tenta sem root (só funciona se o app já rodar com permissões de shell)
        return try {
            val processo = Runtime.getRuntime().exec(command)
            processo.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao executar comando: $command", e)
            false
        }
    }
}
