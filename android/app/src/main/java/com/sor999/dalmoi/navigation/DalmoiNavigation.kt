package com.sor999.dalmoi.navigation

import androidx.activity.compose.LocalActivity
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiDemoViewModel
import com.sor999.dalmoi.presentation.auth.AuthScreen
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.BottomNavigation
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.NavigationTab
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.course.CourseDetailScreen
import com.sor999.dalmoi.presentation.course.CourseEditorScreen
import com.sor999.dalmoi.presentation.course.CourseListScreen
import com.sor999.dalmoi.presentation.crew.CrewDetailScreen
import com.sor999.dalmoi.presentation.crew.CrewListScreen
import com.sor999.dalmoi.presentation.home.HomeScreen
import com.sor999.dalmoi.presentation.record.RecordScreen
import com.sor999.dalmoi.presentation.session.CreateSessionScreen
import com.sor999.dalmoi.presentation.session.RunningScreen
import com.sor999.dalmoi.presentation.session.WaitingRoomScreen
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class AppScreen {
    Login,
    Home,
    Courses,
    Editor,
    CourseDetail,
    Crews,
    CrewDetail,
    CreateSession,
    Waiting,
    Running,
    Record,
}

@Serializable
data class Destination(val screen: AppScreen)

@Composable
fun DalmoiApp(
    modifier: Modifier = Modifier,
    initialScreen: AppScreen = AppScreen.Login,
    viewModel: DalmoiDemoViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    val screen = entry?.toRoute<Destination>()?.screen ?: initialScreen
    val dark = screen == AppScreen.Running && !state.mvp
    val view = LocalView.current
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    var nameSheet by rememberSaveable { mutableStateOf(false) }
    val open: (AppScreen) -> Unit = { next ->
        val currentScreen = controller.currentBackStackEntry?.toRoute<Destination>()?.screen
        if (currentScreen != next) controller.navigate(Destination(next))
    }
    val back: () -> Unit = { if (!controller.popBackStack()) open(AppScreen.Home) }
    Surface(modifier.fillMaxSize(), color = if (dark) Palette.Running else Palette.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            NavHost(
                controller,
                startDestination = Destination(initialScreen),
                modifier = Modifier.weight(1f),
            ) {
                composable<Destination> { destination ->
                    when (destination.toRoute<Destination>().screen) {
                        AppScreen.Login -> AuthScreen(
                            state,
                            viewModel::selectSignup,
                            viewModel::changeEmail,
                            viewModel::changePassword,
                            viewModel::changeNickname,
                            { if (viewModel.enterDemo()) open(AppScreen.Home) },
                        )

                        AppScreen.Home -> HomeScreen(
                            state,
                            { open(AppScreen.Editor) },
                            { open(AppScreen.Crews) },

                            { open(AppScreen.CrewDetail) },
                            { open(AppScreen.Waiting) },
                            { id ->
                                viewModel.selectCourse(id)
                                open(AppScreen.CourseDetail)
                            },
                        )

                        AppScreen.Courses -> CourseListScreen(
                            state,
                            { open(AppScreen.Editor) },
                            { id ->
                                viewModel.selectCourse(id)
                                open(AppScreen.CourseDetail)
                            },
                        )

                        AppScreen.Editor -> CourseEditorScreen(
                            state,
                            viewModel::addPoint,
                            viewModel::undoPoint,

                            viewModel::resetDraft,
                            viewModel::changeDraftName,
                            {
                                if (viewModel.validateDraft()) {
                                    if (state.mvp && state.draftName.isNotBlank()) {
                                        if (viewModel.saveDemoCourse()) open(AppScreen.Running)
                                    } else {
                                        nameSheet = true
                                    }
                                }
                            },
                            back,
                        )

                        AppScreen.CourseDetail -> CourseDetailScreen(
                            state.course,
                            back,
                            { open(AppScreen.CreateSession) },
                        )

                        AppScreen.Crews -> CrewListScreen(
                            state,
                            viewModel::changeInviteCode,
                            { if (viewModel.joinDemoCrew()) open(AppScreen.CrewDetail) },

                            { open(AppScreen.CrewDetail) },
                            { name ->
                                viewModel.nameCrew(name)
                                open(AppScreen.CrewDetail)
                            },
                        )

                        AppScreen.CrewDetail -> CrewDetailScreen(
                            state,
                            back,
                            { open(AppScreen.CreateSession) },
                            { open(AppScreen.Waiting) },
                            { open(AppScreen.Record) },
                        )

                        AppScreen.CreateSession -> CreateSessionScreen(
                            state,
                            viewModel::selectCourse,
                            back,
                            { name ->
                                viewModel.nameRun(name)
                                open(AppScreen.Waiting)
                            },
                        )

                        AppScreen.Waiting -> WaitingRoomScreen(state, back, { open(AppScreen.Running) })

                        AppScreen.Running -> RunningScreen(state, { open(AppScreen.Record) })

                        AppScreen.Record -> RecordScreen(state, { open(AppScreen.Home) })
                    }
                }
            }
            if (screen in listOf(AppScreen.Home, AppScreen.Courses, AppScreen.Crews)) {
                BottomNavigation(
                    listOf(
                        NavigationTab(AppScreen.Home, R.string.nav_home),
                        NavigationTab(AppScreen.Courses, R.string.nav_courses),
                        NavigationTab(AppScreen.Crews, R.string.nav_crews),
                        NavigationTab(AppScreen.Record, R.string.nav_records),
                    ),
                    screen,
                    open,
                )
            }
        }
    }
    if (nameSheet) {
        AlertDialog(
            onDismissRequest = { nameSheet = false },
            title = { Body(label(R.string.course_save), size = 20.sp, weight = FontWeight.Bold) },
            text = { LabeledInput(label(R.string.course_name), state.draftName, viewModel::changeDraftName) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.saveDemoCourse()) {
                            nameSheet = false
                            open(AppScreen.CourseDetail)
                        }
                    },
                    enabled = state.draftName.isNotBlank(),
                ) { Body(label(R.string.course_save)) }
            },

            dismissButton = { TextButton(onClick = { nameSheet = false }) { Body(label(R.string.course_cancel)) } },

        )
    }
}
