package com.sor999.dalmoi.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import java.net.URI

/** Local development servers need Nearby devices permission on Android 17. */
@Composable
fun LocalServerAccess(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiresPermission = BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37 && isLocalServer(BuildConfig.API_BASE_URL)
    var granted by remember {
        mutableStateOf(
            !requiresPermission || context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = !requiresPermission || context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Body(stringResource(R.string.api_local_access_help))
            ActionButton(stringResource(R.string.api_local_access_allow), {
                if (Build.VERSION.SDK_INT >= 37) launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            })
            ActionButton(stringResource(R.string.api_local_settings), {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            })
        }
    }
}

internal fun isLocalServer(url: String): Boolean {
    val host = try {
        URI(url).host?.lowercase() ?: return false
    } catch (_: Exception) {
        return false
    }
    val octets = host.split('.').mapNotNull { it.toIntOrNull() }
    if (host in setOf("localhost", "::1", "[::1]")) return true
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false
    return when (octets[0]) {
        10, 127 -> true
        192 -> octets[1] == 168
        172 -> octets[1] in 16..31
        else -> false
    }
}
