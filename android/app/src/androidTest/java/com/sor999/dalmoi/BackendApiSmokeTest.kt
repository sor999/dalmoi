package com.sor999.dalmoi

import android.Manifest
import android.annotation.SuppressLint
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.descendants
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.sor999.dalmoi.app.DalmoiApplication
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.local.EncryptedTokenStore
import com.sor999.dalmoi.data.local.TokenStore
import com.sor999.dalmoi.data.local.Tokens
import com.sor999.dalmoi.data.remote.ApiClient
import com.sor999.dalmoi.data.remote.createHttpClient
import com.sor999.dalmoi.data.repository.HttpBackendRepository
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.platform.tracking.GpsSample
import com.sor999.dalmoi.platform.tracking.RunLocationApi
import com.sor999.dalmoi.platform.tracking.RunLocationService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/** Explicit opt-in: creates an isolated account and data on the configured development server. */
class BackendApiSmokeTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val deviceFixes = ConcurrentHashMap.newKeySet<GeoPoint>()
    private var stopLocationObservation: (() -> Unit)? = null
    private val runtimePermissions = TestRule { test, _ ->
        object : Statement() {
            override fun evaluate() {
                if (isBackendSmokeEnabled()) {
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val packageName = instrumentation.targetContext.packageName
                    val automation = instrumentation.uiAutomation
                    if (Build.VERSION.SDK_INT >= 37) {
                        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_LOCAL_NETWORK)
                    }
                    automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
                    automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
                }
                try {
                    test.evaluate()
                } finally {
                    stopLocationObservation?.invoke()
                }
            }
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(runtimePermissions).around(compose)
    private fun text(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
    private fun waitForText(value: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun waitUntilEnabled(id: Int) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(text(id)) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun click(id: Int) {
        val node = compose.onNodeWithText(text(id))
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
    }
    private fun tab(name: String) {
        val tag = "api-tab-$name"
        for (attempt in 0 until 3) {
            if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) break
            compose.onNodeWithContentDescription(text(R.string.course_back)).performClick()
            compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
        }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
    }

    private fun logoutFromHome() {
        tab("Home")
        compose.onNodeWithContentDescription(text(R.string.home_profile)).performClick()
        compose.onNodeWithText(text(R.string.api_logout)).performClick()
    }

    private fun isBackendSmokeEnabled() =
        InstrumentationRegistry.getArguments().getString("dalmoi.runBackendSmoke") == "true"

    private fun signUp(accountId: String) {
        if (compose.onAllNodesWithContentDescription(text(R.string.home_profile)).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription(text(R.string.home_profile)).performClick()
        }
        if (compose.onAllNodesWithText(text(R.string.api_logout)).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText(text(R.string.api_logout)).performClick()
        }
        waitForText(text(R.string.auth_email))
        compose.onAllNodesWithText(text(R.string.auth_signup)).onFirst().performClick()
        compose.onNodeWithContentDescription(
            text(R.string.auth_email),
        ).performTextInput("android-$accountId@example.com")
        compose.onNodeWithContentDescription(text(R.string.auth_password)).performTextInput("SmokePassword123!")
        compose.onNodeWithContentDescription(text(R.string.auth_nickname)).performTextInput("AndroidRunner")
        closeSoftKeyboard()
        compose.onNodeWithTag("auth-submit").performClick()
        waitForText(text(R.string.home_greeting_format, "AndroidRunner"))
    }

    @Test
    fun courseEditorShowsUnavailableMapWhenNaverKeyIsMissing() {
        assumeTrue(isBackendSmokeEnabled())
        assumeTrue(!BuildConfig.NAVER_MAP_CONFIGURED)
        signUp(System.currentTimeMillis().toString())
        tab("Courses")
        click(R.string.course_title)
        compose.onNodeWithText(text(R.string.api_course_map_unavailable)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.course_save)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(text(R.string.course_back)).performClick()
        logoutFromHome()
        waitForText(text(R.string.auth_email))
    }

    @Test
    fun realBackendSignupCourseCrewSessionAndRecordLifecycle() {
        assumeTrue(isBackendSmokeEnabled())
        check(BuildConfig.NAVER_MAP_CONFIGURED) { "Set the Naver Maps key ID before running the live map test." }
        val unique = System.currentTimeMillis().toString()
        val crew = "Android-$unique"
        val course = "Course-$unique"
        val session = "Run-$unique"
        signUp(unique)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val restored = EncryptedTokenStore(context).current
        assertNotNull(restored)
        val persisted = context.getSharedPreferences("auth_session", 0).all.values.joinToString()
        assertFalse(persisted.contains(requireNotNull(restored).accessToken))
        assertFalse(persisted.contains(restored.refreshToken))
        click(R.string.crew_join)
        compose.onNodeWithContentDescription(text(R.string.crew_code)).assertIsDisplayed()
        tab("Crews")
        click(R.string.crew_create)
        compose.onNodeWithContentDescription(text(R.string.crew_name)).performTextInput(crew)
        closeSoftKeyboard()
        compose.onNodeWithText(text(R.string.crew_create_action)).performClick()
        waitForText(crew)
        waitUntilEnabled(R.string.api_show_invite)
        click(R.string.api_show_invite)
        val inviteCode = SemanticsMatcher("16-character invitation") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any {
                Regex("^[A-Z0-9]{16}$").matches(it.text)
            } == true
        }
        compose.waitUntil(30_000) { compose.onAllNodes(inviteCode).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(inviteCode).performScrollTo().assertIsDisplayed()
        tab("Home")
        click(R.string.course_title)
        compose.onNodeWithTag("api-tab-Courses").assertDoesNotExist()
        compose.onNodeWithContentDescription(text(R.string.course_name)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.course_distance)).assertIsDisplayed()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("course-map-ready").fetchSemanticsNodes().isNotEmpty()
        }
        val map = compose.onNodeWithTag("course-map-ready")
        compose.onNodeWithText(text(R.string.course_save)).assertIsNotEnabled()
        map.performTouchInput { click(Offset(width * .40f, height * .42f)) }
        waitUntilEnabled(R.string.course_undo)
        compose.onNodeWithText(text(R.string.course_save)).assertIsNotEnabled()
        map.performTouchInput { click(Offset(width * .60f, height * .50f)) }
        waitUntilEnabled(R.string.course_save)
        click(R.string.course_undo)
        compose.onNodeWithText(text(R.string.course_save)).assertIsNotEnabled()
        map.performTouchInput { click(Offset(width * .70f, height * .52f)) }
        waitUntilEnabled(R.string.course_save)
        click(R.string.course_save)
        compose.onNodeWithContentDescription(text(R.string.course_name)).performTextInput(course)
        closeSoftKeyboard()
        compose.onNodeWithTag("course-name-confirm").performClick()
        waitForText(course)
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("course-map-ready").fetchSemanticsNodes().isNotEmpty()
        }
        tab("Crews")
        waitForText(crew)
        compose.onNodeWithText(crew).performScrollTo().performClick()
        waitForText(crew)
        waitUntilEnabled(R.string.crew_add_run)
        click(R.string.crew_add_run)
        compose.waitUntil(30_000) {
            compose.onAllNodesWithContentDescription(text(R.string.api_session_name)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(text(R.string.api_session_name)).performTextInput(session)
        closeSoftKeyboard()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(course, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(course, substring = true).performScrollTo().performClick()
        click(R.string.api_create_session)
        waitForText(session)
        tab("Home")
        waitForText(session)
        compose.onNodeWithText(text(R.string.home_run_participants_format, 1L, 1L)).assertIsDisplayed()
        click(R.string.home_enter)
        val backgroundSmoke = InstrumentationRegistry.getArguments().getString("dalmoi.runGpsSmoke") == "true"
        val peer = if (backgroundSmoke) preparePeer(unique) else null
        click(R.string.api_start_group)
        waitForText(text(R.string.api_start_me))
        if (isCoordinateQaEnabled()) observeDeviceFixes()
        click(R.string.api_start_me)
        waitForText(text(R.string.run_hold_end))
        if (isCoordinateQaEnabled()) verifyMyLocationCamera()
        if (peer != null) {
            verifyPeerLocation(peer)
            verifyBackgroundGps()
        }
        compose.onNodeWithText(text(R.string.run_hold_end)).performTouchInput { longClick() }
        compose.onNodeWithText(text(R.string.run_end_confirm)).performClick()
        waitForText(text(R.string.api_complete_group))
        compose.waitUntil(5_000) { RunLocationService.tracking.value.sessionId == null }
        click(R.string.api_complete_group)
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(text(R.string.api_status_completed)).fetchSemanticsNodes().isNotEmpty()
        }
        tab("Records")
        // The DB worker completes asynchronously; refresh until the actual record appears.
        val deadline = System.nanoTime() + 20_000_000_000L
        while (compose.onAllNodesWithText(session).fetchSemanticsNodes().isEmpty() && System.nanoTime() < deadline) {
            compose.onNodeWithText(text(R.string.api_refresh)).performClick()
            compose.waitForIdle()
            compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
        }
        waitForText(session)
        compose.onNodeWithText(session).performScrollTo().performClick()
        waitForText(text(R.string.record_title))
        compose.onNodeWithText(text(R.string.record_actual)).assertIsDisplayed()
        if (isCoordinateQaEnabled()) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            val repository = (context as DalmoiApplication).container.repository
            runBlocking {
                val summary = repository.records(0).getOrThrow().items.single { it.title == session }
                val route = repository.record(summary.id).getOrThrow().actualRoute
                assertTrue("At least two measured points must survive settlement", route.distinct().size >= 2)
                route.forEach { assertInjectedCoordinates(it.latitude, it.longitude) }
            }
        }
        logoutFromHome()
        waitForText(text(R.string.auth_email))
        compose.onNodeWithTag("auth-submit").assertIsDisplayed()
    }

    private data class Peer(
        val repository: HttpBackendRepository,
        val api: RunLocationApi,
        val sessionId: Long,
        val nickname: String,
    )

    private fun preparePeer(unique: String): Peer = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val owner = (context as DalmoiApplication).container.repository
        val crew = owner.crews().getOrThrow().single()
        val sessionId = owner.sessions(crew.id).getOrThrow().single().id
        val auth = AuthSession(object : TokenStore {
            override var current: Tokens? = null
                private set
            override fun save(tokens: Tokens) {
                current = tokens
            }
            override fun clear() {
                current = null
            }
        })
        val api = ApiClient(createHttpClient(), BuildConfig.API_BASE_URL, auth)
        val repo = HttpBackendRepository(api, auth)
        val nickname = "GPS peer"
        repo.authenticate("peer-$unique@example.com", "password123", nickname).getOrThrow()
        repo.joinCrew(owner.inviteCode(crew.id).getOrThrow()).getOrThrow()
        repo.sessionCommand(sessionId, "participants").getOrThrow()
        Peer(repo, RunLocationApi(api, auth, BuildConfig.API_BASE_URL), sessionId, nickname)
    }

    private fun verifyPeerLocation(peer: Peer) {
        runBlocking {
            val ownPosition = withTimeout(25_000) {
                RunLocationService.tracking.first { it.sharing && it.positions.isNotEmpty() }.positions.first()
            }
            if (isCoordinateQaEnabled()) {
                assertInjectedCoordinates(ownPosition.latitude, ownPosition.longitude)
            }
            peer.repository.sessionCommand(peer.sessionId, "me/start").getOrThrow()
            val time = requireNotNull(peer.repository.session(peer.sessionId).getOrThrow().myStartedAt)
            assertTrue(
                peer.api.submit(
                    peer.sessionId,
                    GpsSample(
                        UUID.randomUUID().toString(),
                        time.toEpochMilli(),
                        ownPosition.latitude + 0.00005,
                        ownPosition.longitude,
                        8.0,
                        time.toString(),
                    ),
                ),
            )
        }
        val markerVisible = SemanticsMatcher(peer.nickname) { node ->
            node.config.getOrNull(SemanticsProperties.StateDescription)?.contains(peer.nickname) == true
        }
        compose.waitUntil(15_000) { compose.onAllNodes(markerVisible).fetchSemanticsNodes().isNotEmpty() }
        if (isCoordinateQaEnabled()) {
            val ownMarker = SemanticsMatcher("Own runner marker") { node ->
                node.config.getOrNull(SemanticsProperties.StateDescription)?.contains("AndroidRunner") == true
            }
            compose.waitUntil(15_000) { compose.onAllNodes(ownMarker).fetchSemanticsNodes().isNotEmpty() }
        }
        runBlocking { peer.repository.sessionCommand(peer.sessionId, "me/finish").getOrThrow() }
        compose.waitUntil(15_000) { compose.onAllNodes(markerVisible).fetchSemanticsNodes().isEmpty() }
    }

    // The test host injects changing emulator GPS fixes throughout this opt-in test.
    private fun verifyBackgroundGps() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .use { it.readBytes() }
        }
        val tracking = RunLocationService.tracking
        val foreground = runBlocking {
            withTimeout(25_000) { tracking.first { it.sharing && it.positions.isNotEmpty() }.positions }
        }
        shell("input keyevent KEYCODE_HOME")
        shell("input keyevent KEYCODE_SLEEP")
        try {
            runBlocking {
                withTimeout(25_000) {
                    tracking.first { it.sharing && it.positions.isNotEmpty() && it.positions != foreground }
                }
            }
        } finally {
            shell("input keyevent KEYCODE_WAKEUP")
            shell("wm dismiss-keyguard")
            shell("am start -W -n com.sor999.dalmoi/.app.MainActivity")
        }
        waitForText(text(R.string.run_tracking_sharing))
    }

    private fun isCoordinateQaEnabled() =
        InstrumentationRegistry.getArguments().getString("dalmoi.verifyInjectedCoordinates") == "true"

    private fun awaitMap(): NaverMap {
        val ready = CompletableDeferred<NaverMap>()
        compose.runOnIdle {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().single()
            val mapView = activity.findViewById<ViewGroup>(android.R.id.content)
                .descendants.filterIsInstance<MapView>().single()
            mapView.getMapAsync { ready.complete(it) }
        }
        return runBlocking { withTimeout(15_000) { ready.await() } }
    }

    private fun verifyMyLocationCamera() {
        compose.waitUntil(25_000) { RunLocationService.tracking.value.myLocation != null }
        val map = awaitMap()
        fun isNearMyLocation(): Boolean = compose.runOnIdle {
            val position = RunLocationService.tracking.value.myLocation ?: return@runOnIdle false
            val target = map.cameraPosition.target
            abs(target.latitude - position.latitude) < 0.0002 &&
                abs(target.longitude - position.longitude) < 0.0002
        }
        compose.waitUntil(15_000) { isNearMyLocation() }
        compose.runOnIdle { map.moveCamera(CameraUpdate.scrollTo(LatLng(37.40, 127.10))) }
        compose.waitUntil(5_000) { !isNearMyLocation() }
        compose.onNodeWithContentDescription(text(R.string.run_my_location)).performClick()
        compose.waitUntil(15_000) { isNearMyLocation() }
    }

    @SuppressLint("MissingPermission") // The opt-in smoke test rule grants precise location.
    private fun observeDeviceFixes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = requireNotNull(context.getSystemService(LocationManager::class.java))
        val listener = LocationListener { fix ->
            if (fix.provider == LocationManager.GPS_PROVIDER) {
                deviceFixes.add(GeoPoint(fix.latitude, fix.longitude))
            }
        }
        manager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0f, listener, Looper.getMainLooper())
        stopLocationObservation = { manager.removeUpdates(listener) }
    }

    // The QA feeder uses 37.52 north and increases the longitude by 0.000012° per fix.
    private fun assertInjectedCoordinates(latitude: Double, longitude: Double) {
        assertTrue("Server coordinates must match an Android GPS reading", GeoPoint(latitude, longitude) in deviceFixes)
        // The emulator console can shift the injected value by about 0.00000167°.
        val emulatorPrecision = 0.000002
        assertEquals(37.52, latitude, emulatorPrecision)
        val index = ((longitude - 126.93) / 0.000012).roundToInt()
        assertTrue("Longitude must come from the emulator feed", index in 0 until 120)
        assertEquals(126.93 + index * 0.000012, longitude, emulatorPrecision)
    }
}
