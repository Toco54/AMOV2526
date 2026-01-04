package pt.isec.amov2526.projetoKotlin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstração reativa para a API Fused Location Provider da Google.
 *
 * Transforma o modelo de callbacks da API do Google num `StateFlow` moderno do Kotlin,
 * facilitando a integração com ViewModels e Serviços sem "Callback Hell".
 *
 * @param context Contexto da aplicação (deve ser ApplicationContext para evitar memory leaks).
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Fonte única de verdade para a localização atual.
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private var locationCallback: LocationCallback? = null

    /**
     * Inicia o pedido de atualizações de localização periódicas.
     * Requer permissões `ACCESS_FINE_LOCATION` ou `ACCESS_COARSE_LOCATION` garantidas previamente.
     *
     * @param intervalMs Intervalo desejado entre atualizações (Default: 5s).
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(intervalMs: Long = 5000L) {
        if (locationCallback != null) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                }
            }
        }

        Log.d("LocationHelper", "Starting location updates...")
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback!!, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error starting location updates", e)
        }
    }

    /**
     * Para as atualizações e remove o callback para poupar bateria.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d("LocationHelper", "Location updates stopped")
        }
    }

    /**
     * Tenta obter a última localização conhecida sem iniciar atualizações contínuas.
     * Útil para inicialização rápida de mapas.
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                _currentLocation.value = location
            }
        }
    }
}
