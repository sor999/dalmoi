package com.sor999.dalmoi.presentation.course

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.ContextThemeWrapper
import androidx.appcompat.R as AppCompatR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapOptions
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PolylineOverlay
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.R
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.presentation.common.theme.Palette
import kotlinx.coroutines.delay

@Composable
internal fun CourseMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    onPickPoint: ((GeoPoint) -> Unit)? = null,
    recenterRequest: Int = 0,
    nightMode: Boolean = false,
    referencePoints: List<GeoPoint> = emptyList(),
    detailLoop: Boolean? = null,
) {
    if (!BuildConfig.NAVER_MAP_CONFIGURED) {
        Box(modifier.background(Palette.Soft), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.api_course_map_unavailable), Modifier.padding(12.dp))
        }
        return
    }

    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) {
        CourseMapView(
            points,
            modifier,
            onPickPoint,
            recenterRequest,
            nightMode,
            referencePoints,
            detailLoop,
            onRetry = { attempt++ },
        )
    }
}

@Composable
private fun CourseMapView(
    points: List<GeoPoint>,
    modifier: Modifier,
    onPickPoint: ((GeoPoint) -> Unit)?,
    recenterRequest: Int,
    nightMode: Boolean,
    referencePoints: List<GeoPoint>,
    detailLoop: Boolean?,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val boundsPadding = with(LocalDensity.current) { 24.dp.roundToPx() }
    val detailPadding = with(LocalDensity.current) { 36.dp.roundToPx() }
    val detailTopPadding = with(LocalDensity.current) { 48.dp.roundToPx() }
    val routeWidth = with(LocalDensity.current) { 5.dp.roundToPx() }
    val loopMarkerSize = with(LocalDensity.current) { 24.dp.roundToPx() }
    val loopMarker = remember(context, detailLoop, loopMarkerSize) {
        if (detailLoop == true) loopMarker(context, loopMarkerSize) else null
    }
    val editing = onPickPoint != null
    var lastRecenterRequest by remember { mutableIntStateOf(recenterRequest) }
    val currentPick by rememberUpdatedState(onPickPoint)
    val currentPoints by rememberUpdatedState(points + referencePoints)
    var map by remember { mutableStateOf<NaverMap?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var authFailed by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    val mapView = remember(context, lifecycle) {
        val center = currentPoints.firstOrNull()?.toLatLng() ?: initialCourseCenter
        val options = NaverMapOptions().useTextureView(true).camera(CameraPosition(center, 15.0))
        val mapContext = ContextThemeWrapper(context, AppCompatR.style.Theme_AppCompat_Light_NoActionBar)
        MapView(mapContext, options)
    }

    DisposableEffect(mapView, lifecycle) {
        val sdk = NaverMapSdk.getInstance(context.applicationContext)
        var disposed = false
        val authListener = NaverMapSdk.OnAuthFailedListener {
            mapView.post { if (!disposed) authFailed = true }
        }
        sdk.onAuthFailedListener = authListener

        val loadListener = NaverMap.OnLoadListener { loaded = true }
        mapView.onCreate(null)
        mapView.getMapAsync { readyMap ->
            if (!disposed) {
                map = readyMap
                readyMap.isNightModeEnabled = nightMode
                readyMap.uiSettings.isZoomControlEnabled = false
                readyMap.uiSettings.isCompassEnabled = false
                readyMap.uiSettings.isScaleBarEnabled = false
                readyMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false)
                loaded = readyMap.isLoaded
                if (!loaded) readyMap.addOnLoadListener(loadListener)
                if (editing) {
                    readyMap.setOnMapClickListener { _, coordinate ->
                        if (loaded &&
                            !authFailed
                        ) {
                            currentPick?.invoke(GeoPoint(coordinate.latitude, coordinate.longitude))
                        }
                    }
                } else {
                    if (detailLoop == null) {
                        readyMap.showRoute(currentPoints, boundsPadding)
                    } else {
                        readyMap.showRoute(currentPoints, detailPadding, detailTopPadding)
                    }
                }
            }
        }

        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    started = true
                }

                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    resumed = true
                }

                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    resumed = false
                }

                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    started = false
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        val application = context.applicationContext
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = mapView.onLowMemory()
            override fun onTrimMemory(level: Int) = Unit
        }
        application.registerComponentCallbacks(memoryCallbacks)

        onDispose {
            disposed = true
            application.unregisterComponentCallbacks(memoryCallbacks)
            lifecycle.removeObserver(observer)
            if (sdk.onAuthFailedListener === authListener) sdk.onAuthFailedListener = null
            map?.removeOnLoadListener(loadListener)
            map?.setOnMapClickListener(null)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(loaded, authFailed) {
        if (!loaded && !authFailed) {
            delay(20_000)
            timedOut = true
        }
    }

    LaunchedEffect(recenterRequest, loaded, map) {
        val activeMap = map
        if (activeMap != null && loaded && lastRecenterRequest != recenterRequest) {
            activeMap.showRoute(currentPoints, boundsPadding)
            lastRecenterRequest = recenterRequest
        }
    }

    // Each point update replaces the overlays as a group, keeping undo and reset in sync.
    DisposableEffect(map, points, referencePoints, routeWidth, detailLoop, loopMarker) {
        val activeMap = map
        if (activeMap == null) {
            onDispose {}
        } else {
            val coordinates = points.map { it.toLatLng() }
            val reference = activeMap.routeLine(
                referencePoints.map { it.toLatLng() },
                if (nightMode) Palette.Muted.toArgb() else Palette.Faint.toArgb(),
                routeWidth,
                dashed = true,
            )
            val routeColor = if (nightMode) Palette.Lime else Palette.Ink
            val line = activeMap.routeLine(coordinates, routeColor.toArgb(), routeWidth)
            val markers = coordinates.mapIndexedNotNull { index, coordinate ->
                val markerIcon = when {
                    index == 0 && loopMarker != null -> loopMarker
                    index == 0 -> OverlayImage.fromResource(R.drawable.ic_course_start)
                    detailLoop == true -> null
                    index == coordinates.lastIndex -> OverlayImage.fromResource(R.drawable.ic_course_end)
                    detailLoop == null -> OverlayImage.fromResource(R.drawable.ic_course_point)
                    else -> null
                }
                if (markerIcon == null) return@mapIndexedNotNull null
                Marker().apply {
                    position = coordinate
                    icon = markerIcon
                    anchor = PointF(0.5f, 0.5f)
                    this.map = activeMap
                }
            }
            onDispose {
                reference?.map = null
                line?.map = null
                markers.forEach { it.map = null }
            }
        }
    }

    val description = stringResource(R.string.api_course_map_label)
    val ready = loaded && !authFailed
    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
                .testTag(if (ready) "course-map-ready" else "course-map")
                .semantics { contentDescription = description },
        )
        if (!ready) {
            Surface(Modifier.align(Alignment.Center), color = Palette.White) {
                if (authFailed || timedOut) {
                    TextButton(onRetry) { Text(stringResource(R.string.api_course_map_error_retry)) }
                } else {
                    Text(stringResource(R.string.api_course_map_loading), Modifier.padding(12.dp))
                }
            }
        }
    }
}

private fun NaverMap.routeLine(
    coordinates: List<LatLng>,
    routeColor: Int,
    routeWidth: Int,
    dashed: Boolean = false,
): PolylineOverlay? {
    if (coordinates.size < 2) return null
    return PolylineOverlay().apply {
        coords = coordinates
        color = routeColor
        width = routeWidth
        capType = PolylineOverlay.LineCap.Round
        joinType = PolylineOverlay.LineJoin.Round
        if (dashed) setPattern(routeWidth * 2, routeWidth)
        map = this@routeLine
    }
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private fun loopMarker(context: Context, size: Int): OverlayImage {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(size / 24f, size / 24f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Palette.Ink.toArgb()
    canvas.drawCircle(12f, 12f, 12f, paint)
    paint.color = Palette.Lime.toArgb()
    canvas.drawCircle(12f, 12f, 9.75f, paint)
    paint.color = Palette.Ink.toArgb()
    paint.typeface = ResourcesCompat.getFont(context, R.font.pretendard_bold)
    paint.textSize = 11f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("S/E", 12f, 12f - (paint.ascent() + paint.descent()) / 2, paint)
    return OverlayImage.fromBitmap(bitmap)
}

private val initialCourseCenter = LatLng(37.52, 126.93)

private fun NaverMap.showRoute(points: List<GeoPoint>, padding: Int, topPadding: Int = padding) {
    val coordinates = points.distinct().map { it.toLatLng() }
    val camera = if (coordinates.size > 1) {
        CameraUpdate.fitBounds(LatLngBounds.from(coordinates), padding, topPadding, padding, padding)
    } else {
        CameraUpdate.scrollAndZoomTo(coordinates.firstOrNull() ?: initialCourseCenter, 15.0)
    }
    moveCamera(camera)
}
