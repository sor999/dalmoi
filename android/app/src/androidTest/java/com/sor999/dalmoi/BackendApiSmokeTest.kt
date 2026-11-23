package com.sor999.dalmoi

import android.Manifest
import android.os.Build
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
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.data.local.EncryptedTokenStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/** Explicit opt-in: creates an isolated account and data on the configured development server. */
class BackendApiSmokeTest {
    val compose = createAndroidComposeRule<MainActivity>()
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
                test.evaluate()
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
        click(R.string.crew_add_run)
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
        click(R.string.api_start_group)
        waitForText(text(R.string.api_start_me))
        click(R.string.api_start_me)
        waitForText(text(R.string.run_hold_end))
        compose.onNodeWithText(text(R.string.run_hold_end)).performTouchInput { longClick() }
        compose.onNodeWithText(text(R.string.run_end_confirm)).performClick()
        waitForText(text(R.string.api_complete_group))
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
        logoutFromHome()
        waitForText(text(R.string.auth_email))
        compose.onNodeWithTag("auth-submit").assertIsDisplayed()
    }
}
