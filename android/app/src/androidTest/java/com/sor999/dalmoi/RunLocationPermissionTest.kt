package com.sor999.dalmoi

import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.presentation.common.theme.DalmoiTheme
import com.sor999.dalmoi.presentation.session.RunLocationStartButton
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class RunLocationPermissionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun deniedPermissionKeepsRunStoppedAndPreciseGrantStartsRun() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("dalmoi.runLocationPermissionSmoke") == "true")
        assumeTrue(
            instrumentation.targetContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED,
        )
        val starts = mutableIntStateOf(0)
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                DalmoiTheme {
                    RunLocationStartButton(enabled = true) { starts.intValue++ }
                }
            }
        }

        fun text(id: Int) = instrumentation.targetContext.getString(id)
        compose.onNodeWithText(text(R.string.api_start_me)).performClick()
        clickSystemPermissionButton("permission_deny_button")
        compose.onNodeWithText(text(R.string.run_location_permission_title)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, starts.intValue) }

        compose.onNodeWithText(text(R.string.api_dismiss)).performClick()
        compose.onNodeWithText(text(R.string.api_start_me)).performClick()
        clickSystemPermissionButton("permission_allow_foreground_only_button")
        compose.waitUntil(5_000) { starts.intValue == 1 }
    }

    private fun clickSystemPermissionButton(id: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        compose.waitUntil(10_000) {
            automation.rootInActiveWindow?.findPermissionButton(id)?.performAction(
                AccessibilityNodeInfo.ACTION_CLICK,
            ) == true
        }
    }

    private fun AccessibilityNodeInfo.findPermissionButton(id: String): AccessibilityNodeInfo? {
        if (viewIdResourceName?.endsWith(":id/$id") == true) return this
        for (index in 0 until childCount) {
            getChild(index)?.findPermissionButton(id)?.let { return it }
        }
        return null
    }
}
