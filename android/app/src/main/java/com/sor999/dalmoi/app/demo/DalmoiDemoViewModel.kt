package com.sor999.dalmoi.app.demo

import androidx.lifecycle.ViewModel
import com.sor999.dalmoi.presentation.common.map.MapArtwork
import com.sor999.dalmoi.presentation.common.map.MapPoint
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CourseCardData(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val thumbnail: MapArtwork = MapArtwork.ThumbLoop,
    val createdOn: String = "2026.11.08",
    val points: List<MapPoint> = emptyList(),
)

enum class DraftError { TooFewPoints, NameRequired, PointLimit, SaveFailed }

object FigmaFixtures {
    val editorPoints = listOf(
        MapPoint(64f, 420f),
        MapPoint(98f, 262f),
        MapPoint(196f, 206f),
        MapPoint(286f, 268f),
        MapPoint(254f, 408f),
    )
    val courses = listOf(
        CourseCardData("banpo", "반포 한강 루프", 5240),
        CourseCardData("seorae", "서래섬 왕복", 3100, MapArtwork.ThumbReturn),
        CourseCardData("jamwon", "잠원 인터벌", 2040, MapArtwork.ThumbIntervals),
    )
}

/** Design fixtures are isolated here; no values in this state are server or GPS responses. */
data class DalmoiUiState(
    val courses: List<CourseCardData> = FigmaFixtures.courses,
    val selectedCourseId: String = "banpo",
    val crewName: String = "새벽 한강 크루",
    val runName: String = "목요일 새벽 5K",
    val draftPoints: List<MapPoint> = FigmaFixtures.editorPoints,
    val draftName: String = "",
    val draftDistanceMeters: Int = 2860,
    val draftEdited: Boolean = false,
    val draftError: DraftError? = null,
    val email: String = "minjun@dalmoi.app",
    val password: String = "",
    val nickname: String = "",
    val signingUp: Boolean = false,
    val authError: Boolean = false,
    val inviteCode: String = "RS-7K2Q",
    val inviteError: Boolean = false,
    val disconnected: Boolean = false,
    val processingRecord: Boolean = false,
    val mvp: Boolean = false,
) {
    val course: CourseCardData get() = courses.first { it.id == selectedCourseId }
}

class DalmoiDemoViewModel(initialState: DalmoiUiState = DalmoiUiState()) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    fun changeEmail(value: String) {
        _uiState.update { it.copy(email = value, authError = false) }
    }
    fun changePassword(value: String) {
        _uiState.update { it.copy(password = value, authError = false) }
    }
    fun changeNickname(value: String) {
        _uiState.update { it.copy(nickname = value) }
    }
    fun selectSignup(value: Boolean) {
        _uiState.update { it.copy(signingUp = value, authError = false) }
    }
    fun enterDemo(): Boolean {
        val state = uiState.value
        val valid =
            state.email.contains('@') && state.password.isNotBlank() &&
                (!state.signingUp || state.nickname.isNotBlank())
        _uiState.update { it.copy(authError = !valid, password = if (valid) "" else it.password) }
        return valid
    }
}

fun kilometers(meters: Int): String = String.format(Locale.ROOT, "%.2f", meters / 1000.0)
