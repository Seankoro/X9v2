@file:Suppress("DEPRECATION")

package dk.itu.moapd.x9.s25134.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import dk.itu.moapd.x9.s25134.R

/**
 * Foreground service that collects GPS location updates and broadcasts them via LocalBroadcastManager.
 */
class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        // Broadcast action that receivers listen for
        const val ACTION_LOCATION_BROADCAST = "dk.itu.moapd.x9.s25134.LOCATION_BROADCAST"
        // Key for the Location object inside the broadcast Intent
        const val EXTRA_LOCATION = "extra_location"
        private const val CHANNEL_ID = "location_channel"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL = 10000L // 10 seconds
        private const val FASTEST_INTERVAL = 5000L // 5 seconds minimum
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Called every time GPS delivers a new location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                // Wrap location in an Intent and broadcast it
                val intent = Intent(ACTION_LOCATION_BROADCAST)
                intent.putExtra(EXTRA_LOCATION, location)
                LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires a visible notification for foreground services
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.location_tracking))
            .setSmallIcon(R.drawable.ic_x9_logo)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        requestLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.location_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL
        ).setMinUpdateIntervalMillis(FASTEST_INTERVAL).build()
        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, mainLooper
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted", e)
        }
    }

    // Stop GPS updates when the service is destroyed
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Returns null because this is a started service, not a bound service
    override fun onBind(intent: Intent?): IBinder? = null
}
