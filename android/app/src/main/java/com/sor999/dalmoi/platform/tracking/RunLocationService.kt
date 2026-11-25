package com.sor999.dalmoi.platform.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.DalmoiApplication
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.domain.model.GeoPoint
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal data class RunTrackingState(
    val sessionId: Long? = null,
    val positions: List<RunnerPosition> = emptyList(),
    val sharing: Boolean = false,
    val myLocation: GeoPoint? = null,
)

class RunLocationService :
    Service(),
    LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fixes = Channel<Location>(Channel.CONFLATED)
    private val manager by lazy { getSystemService(LocationManager::class.java) }
    private val api by lazy { (application as DalmoiApplication).container.runLocationApi }
    private var activeId: Long? = null
    private var lastLocalFixNanos = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(EXTRA_SESSION, 0L) ?: 0L
        if (id <= 0 || !api.signedIn.value) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (activeId != null) return START_NOT_STICKY
        val locationManager = manager
        if (locationManager == null || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                GPS_INTERVAL,
                0f,
                this,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            stopSelf()
            return START_NOT_STICKY
        } catch (_: IllegalArgumentException) {
            stopSelf()
            return START_NOT_STICKY
        }
        activeId = id
        mutableTracking.value = RunTrackingState(sessionId = id)
        runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
            ?.takeIf { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..RECENT_FIX_NANOS }
            ?.let(::showLocalFix)
        scope.launch {
            api.signedIn.first { !it }
            stopSelf()
        }
        scope.launch { uploadFixes(id) }
        watchLocations(id)
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        if (activeId == null || !location.hasAccuracy() || !location.accuracy.isFinite()) return
        showLocalFix(location)
        fixes.trySend(location)
    }

    private fun showLocalFix(location: Location) {
        if (!location.hasAccuracy() || location.accuracy !in 0f..50f ||
            location.elapsedRealtimeNanos < lastLocalFixNanos
        ) {
            return
        }
        lastLocalFixNanos = location.elapsedRealtimeNanos
        val point = GeoPoint(location.latitude, location.longitude)
        mutableTracking.update { it.copy(myLocation = point) }
    }

    override fun onProviderDisabled(provider: String) {
        mutableTracking.update { it.copy(sharing = false, myLocation = null) }
    }

    override fun onProviderEnabled(provider: String) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    private suspend fun uploadFixes(id: Long) {
        var lastSequence = -1L
        for (fix in fixes) {
            val timestamp = fix.time
            if (timestamp <= lastSequence) continue
            lastSequence = timestamp
            val sample = GpsSample(
                UUID.randomUUID().toString(),
                timestamp,
                fix.latitude,
                fix.longitude,
                fix.accuracy.toDouble(),
                Instant.ofEpochMilli(timestamp).toString(),
            )
            try {
                val valid = api.submit(id, sample)
                mutableTracking.update { it.copy(sharing = valid) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableTracking.update { it.copy(sharing = false) }
                if (failure.endsTracking()) {
                    stopSelf()
                    return
                }
            }
        }
    }

    private fun watchLocations(id: Long) {
        val updates = Channel<Unit>(Channel.CONFLATED)
        scope.launch {
            while (isActive) {
                if (SystemClock.elapsedRealtimeNanos() - lastLocalFixNanos >= STALE_FIX_NANOS) {
                    mutableTracking.update { it.copy(myLocation = null) }
                }
                updates.trySend(Unit)
                delay(GPS_INTERVAL)
            }
        }
        scope.launch {
            while (isActive) {
                try {
                    api.changes(id).collect { updates.trySend(Unit) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A fresh REST snapshot remains available while the socket reconnects.
                }
                delay(GPS_INTERVAL)
            }
        }
        scope.launch {
            for (update in updates) {
                try {
                    val positions = api.latest(id)
                    mutableTracking.update { it.copy(positions = positions) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutableTracking.update {
                        it.copy(sharing = false, positions = it.positions.map { item -> item.copy(isStale = true) })
                    }
                    if (failure.endsTracking()) {
                        stopSelf()
                        return@launch
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        fixes.close()
        if (activeId != null) manager?.removeUpdates(this)
        mutableTracking.value = RunTrackingState()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.run_tracking_notification),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.run_tracking_notification))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    private fun Exception.endsTracking(): Boolean {
        if (this !is ApiFailure) return false
        return when (kind) {
            ApiFailureKind.Unauthorized, ApiFailureKind.Forbidden, ApiFailureKind.NotFound -> true
            else -> code == "PARTICIPANT_NOT_RUNNING" || code == "SESSION_NOT_RUNNING"
        }
    }

    companion object {
        private const val EXTRA_SESSION = "sessionId"
        private const val GPS_INTERVAL = 5_000L
        private const val RECENT_FIX_NANOS = 15_000_000_000L
        private const val STALE_FIX_NANOS = 30_000_000_000L
        private const val CHANNEL = "run_location"
        private const val NOTIFICATION_ID = 410
        private val mutableTracking = MutableStateFlow(RunTrackingState())
        internal val tracking = mutableTracking.asStateFlow()

        internal fun start(context: Context, sessionId: Long) {
            if (tracking.value.sessionId != null) return
            try {
                val intent = Intent(context, RunLocationService::class.java).putExtra(EXTRA_SESSION, sessionId)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: IllegalStateException) {
                // A lifecycle transition can make this request ineligible until the screen is resumed again.
            } catch (_: SecurityException) {
                // The screen continues to show that location sharing is unavailable.
            }
        }

        internal fun stop(context: Context) {
            context.stopService(Intent(context, RunLocationService::class.java))
        }
    }
}
