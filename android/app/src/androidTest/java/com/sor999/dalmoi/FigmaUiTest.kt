package com.sor999.dalmoi

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.sor999.dalmoi.app.MainActivity
import com.sor999.dalmoi.app.demo.DalmoiDemoViewModel
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.DraftError
import com.sor999.dalmoi.navigation.AppScreen
import com.sor999.dalmoi.navigation.DalmoiApp
import com.sor999.dalmoi.presentation.common.theme.DalmoiTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class FigmaUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun show(screen: AppScreen, state: DalmoiUiState = DalmoiUiState()) {
        val model = DalmoiDemoViewModel(state)
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                key(screen, state) { DalmoiTheme { DalmoiApp(initialScreen = screen, viewModel = model) } }
            }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(250, 5000)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT < 29) {
            File(
                context.getExternalFilesDir(null),
                "$name.png",
            ).outputStream().use {
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    it,
                )
            }
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DalmoiFigmaMcp")
        }
        val uri = checkNotNull(
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            ),
        )
        checkNotNull(context.contentResolver.openOutputStream(uri)).use {
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                it,
            )
        }
    }

    @Test
    fun referenceScreensAndErrorStatesRenderWithLocalSvgAssets() {
        compose.onNodeWithText("디자인 미리보기").assertDoesNotExist()
        listOf(
            AppScreen.Login, AppScreen.Home, AppScreen.Editor, AppScreen.CourseDetail, AppScreen.Crews,
            AppScreen.CrewDetail,
            AppScreen.CreateSession,
            AppScreen.Waiting,
            AppScreen.Running,
            AppScreen.Record,
        ).forEach { screen ->
            show(screen)
            screenshot(screen.name)
        }
        show(AppScreen.Login, DalmoiUiState(authError = true))
        screenshot("LoginError")
        show(AppScreen.Editor, DalmoiUiState(draftError = DraftError.SaveFailed))
        screenshot("EditorError")
        show(AppScreen.Crews, DalmoiUiState(inviteError = true))
        screenshot("CrewError")
        show(AppScreen.Running, DalmoiUiState(disconnected = true))
        compose.onNodeWithText("네트워크 연결을 확인해 주세요.").assertIsDisplayed()
        screenshot("RunningError")
        show(
            AppScreen.Editor,
            DalmoiUiState(
                mvp = true,
                draftPoints = emptyList(),
                draftDistanceMeters = 5000,

                draftName = "여의도 한 바퀴",
                draftError = DraftError.TooFewPoints,
            ),
        )
        screenshot("MvpEditor")
        show(AppScreen.Running, DalmoiUiState(mvp = true))
        compose.onNodeWithText("러닝 종료").performClick()
        screenshot("MvpEndDialog")
        show(AppScreen.Record, DalmoiUiState(mvp = true))
        compose.onNodeWithText("러닝 완료").assertIsDisplayed()
        compose.onNodeWithText("러닝을 종료할까요?").assertDoesNotExist()
        screenshot("MvpRecord")
    }

    @Test
    fun courseResetDrawingAndSaveKeepTheNewRouteAndName() {
        show(AppScreen.Editor)
        compose.onNodeWithText("전체 초기화").performClick()
        compose.onNode(hasText("전체 초기화") and hasAnyAncestor(isDialog())).performClick()
        val map = compose.onNodeWithContentDescription("예시 코스 지도")
        map.performTouchInput { click(Offset(width * .2f, height * .3f)) }
        compose.mainClock.advanceTimeBy(500)
        map.performTouchInput { click(Offset(width * .6f, height * .4f)) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("코스 저장").performClick()
        compose.onNodeWithContentDescription("코스 이름").performTextInput("새 Figma 코스")
        compose.onAllNodesWithText("코스 저장").onLast().performClick()
        compose.onNodeWithText("코스 상세").assertIsDisplayed()
        compose.onNodeWithText("새 Figma 코스").assertIsDisplayed()
        screenshot("SavedCourse")
    }

    @Test
    fun runningCanContinueAfterCancelThenFinishAfterLongPress() {
        show(AppScreen.Running)
        compose.runOnIdle {
            val bars = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            assertFalse(bars.isAppearanceLightStatusBars)
            assertFalse(bars.isAppearanceLightNavigationBars)
        }
        compose.onNodeWithText("길게 눌러 종료").performTouchInput { longClick() }
        compose.onNodeWithText("계속 달리기").performClick()
        compose.onNodeWithText("길게 눌러 종료").assertIsDisplayed()
        compose.onNodeWithText("길게 눌러 종료").performTouchInput { longClick() }
        compose.onNode(hasText("종료") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("실제 이동 거리").assertIsDisplayed()
    }

    @Test
    fun loginInputsNavigateToTheHomeWithoutAnyPreviewToolbar() {
        show(AppScreen.Login)
        compose.onNodeWithContentDescription("비밀번호").performTextInput("ui-preview")
        compose.onAllNodesWithText("로그인").onLast().performClick()
        compose.onNodeWithText("안녕하세요, 민준님").assertIsDisplayed()
        compose.onNodeWithText("디자인 미리보기").assertDoesNotExist()
    }
}
