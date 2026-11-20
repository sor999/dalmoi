package com.sor999.dalmoi

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.sor999.dalmoi.app.DalmoiApplication
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.data.local.EncryptedTokenStore
import com.sor999.dalmoi.domain.model.GeoPoint
import kotlinx.coroutines.runBlocking
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
    private val localNetworkPermission = TestRule { test, _ ->
        object : Statement() {
            override fun evaluate() {
                if (Build.VERSION.SDK_INT >= 37 && isBackendSmokeEnabled()) {
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    instrumentation.uiAutomation.grantRuntimePermission(
                        instrumentation.targetContext.packageName,
                        Manifest.permission.ACCESS_LOCAL_NETWORK,
                    )
                }
                test.evaluate()
            }
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(localNetworkPermission).around(compose)
    private fun text(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
    private fun waitForText(value: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(id: Int) {
        compose.onNodeWithText(text(id)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
    }
    private fun tab(name: String) {
        compose.onNodeWithTag("api-tab-$name").performClick()
        compose.waitForIdle()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("api-loading").fetchSemanticsNodes().isEmpty() }
    }

    private fun isBackendSmokeEnabled() =
        InstrumentationRegistry.getArguments().getString("dalmoi.runBackendSmoke") == "true"

    private fun signUp(accountId: String) {
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
        waitForText("AndroidRunner님, 반가워요")
    }

    @Test
    fun realBackendSignupCourseCrewSessionAndRecordLifecycle() {
        assumeTrue(isBackendSmokeEnabled())
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
        tab("Crews")
        compose.onNodeWithText(text(R.string.api_crew_name)).performTextInput(crew)
        closeSoftKeyboard()
        click(R.string.crew_create)
        waitForText(crew)
        click(R.string.api_show_invite)
        compose.onNode(
            SemanticsMatcher("16-character invitation") { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any {
                    Regex("^[A-Z0-9]{16}$").matches(it.text)
                } == true
            },
        ).assertIsDisplayed()
        val application = context.applicationContext as DalmoiApplication
        runBlocking {
            application.container.repository.createCourse(
                course,
                listOf(GeoPoint(37.52, 126.93), GeoPoint(37.522, 126.932)),
            ).getOrThrow()
        }
        tab("Courses")
        waitForText(course)
        compose.onNodeWithText(course).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.api_choose_crew_for_run)).assertIsDisplayed()
        tab("Crews")
        waitForText("$crew · 1/10명")
        compose.onNodeWithText("$crew · 1/10명").performScrollTo().performClick()
        waitForText(crew)
        click(R.string.course_create_run)
        compose.onNodeWithText(text(R.string.api_session_name)).performTextInput(session)
        closeSoftKeyboard()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(course, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(course, substring = true).performScrollTo().performClick()
        click(R.string.api_create_session)
        waitForText(session)
        click(R.string.api_start_group)
        waitForText(text(R.string.api_start_me))
        click(R.string.api_start_me)
        waitForText(text(R.string.api_finish_me))
        click(R.string.api_finish_me)
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
        waitForText(text(R.string.api_actual_points, 0))
        compose.onNodeWithText(text(R.string.api_actual_points, 0)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.api_logout)).performClick()
        waitForText(text(R.string.auth_email))
        compose.onNodeWithTag("auth-submit").assertIsDisplayed()
    }
}
