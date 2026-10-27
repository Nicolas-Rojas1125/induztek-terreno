package cl.duocuc.induztek.nativo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

data class Coordenadas(val latitud: Double, val longitud: Double, val precisionMetros: Float?)

class ServicioUbicacion(private val context: Context) {
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun gpsActivo(): Boolean =
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    @SuppressLint("MissingPermission")
    suspend fun obtener(timeoutMs: Long = 15_000): Coordenadas? {
        if (!tienePermiso()) return null
        val proveedor = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        val actual = withTimeoutOrNull(timeoutMs) { ubicacionActual(proveedor) }
        val ubicacion = actual ?: ultimaConocida()
        return ubicacion?.let {
            Coordenadas(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun ultimaConocida(): Location? =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    private suspend fun ubicacionActual(proveedor: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val cancelacion = android.os.CancellationSignal()
                cont.invokeOnCancellation { cancelacion.cancel() }
                lm.getCurrentLocation(proveedor, cancelacion, Executors.newSingleThreadExecutor()) { loc ->
                    if (cont.isActive) cont.resume(loc)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Requerido en API < 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
                lm.requestLocationUpdates(proveedor, 0L, 0f, listener, Looper.getMainLooper())
            }
        }
}
