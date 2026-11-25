package com.sor999.dalmoi.presentation.common.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.Palette

@Composable
internal fun rememberPreciseLocationAction(onAllowed: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var notice by rememberSaveable { mutableStateOf<LocationNotice?>(null) }

    fun startWithLocationEnabled() {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager != null && LocationManagerCompat.isLocationEnabled(manager)) {
            onAllowed()
        } else {
            notice = LocationNotice.DeviceDisabled
        }
    }

    val requestLocation =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startWithLocationEnabled()
            } else {
                notice = LocationNotice.PrecisePermissionRequired
            }
        }
    notice?.let { current ->
        AlertDialog(
            onDismissRequest = { notice = null },
            shape = RoundedCornerShape(Corners.Dialog),
            containerColor = Palette.White,
            title = { Body(label(current.title)) },
            text = { Body(label(current.message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        notice = null
                        context.startActivity(current.settingsIntent(context))
                    },
                ) { Body(label(current.action)) }
            },
            dismissButton = {
                TextButton(onClick = { notice = null }) { Body(label(R.string.api_dismiss)) }
            },
        )
    }

    return {
        if (context.hasPreciseLocation()) {
            startWithLocationEnabled()
        } else {
            requestLocation.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            )
        }
    }
}

private fun Context.hasPreciseLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private enum class LocationNotice(val title: Int, val message: Int, val action: Int) {
    PrecisePermissionRequired(
        R.string.run_location_permission_title,
        R.string.run_location_permission_message,
        R.string.run_location_permission_settings,
    ),
    DeviceDisabled(
        R.string.run_location_disabled_title,
        R.string.run_location_disabled_message,
        R.string.run_location_device_settings,
    ),
    ;

    fun settingsIntent(context: Context): Intent = when (this) {
        PrecisePermissionRequired -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )

        DeviceDisabled -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }
}
