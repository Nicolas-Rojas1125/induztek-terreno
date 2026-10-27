package cl.duocuc.induztek.nativo

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class TipoVibracion { EXITO, ADVERTENCIA, ERROR }

class RetroalimentacionHaptica(context: Context) {
    private val vibrador: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun vibrar(tipo: TipoVibracion) {
        val v = vibrador ?: return
        if (!v.hasVibrator()) return
        val efecto = when (tipo) {
            TipoVibracion.EXITO -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            TipoVibracion.ADVERTENCIA -> VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1)
            TipoVibracion.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 200, 90, 200), -1)
        }
        try {
            v.vibrate(efecto)
        } catch (_: SecurityException) {
        }
    }
}
