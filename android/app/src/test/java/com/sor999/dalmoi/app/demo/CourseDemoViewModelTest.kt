package com.sor999.dalmoi.app.demo

import com.sor999.dalmoi.presentation.common.map.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseDemoViewModelTest {
    @Test
    fun validationKeepsInputsAndRequiresTwoPoints() {
        val model = DalmoiDemoViewModel()
        model.changeDraftName("한강 새벽")
        model.resetDraft()
        model.addPoint(MapPoint(20f, 100f))
        assertFalse(model.saveDemoCourse())
        assertEquals(DraftError.TooFewPoints, model.uiState.value.draftError)
        assertEquals("한강 새벽", model.uiState.value.draftName)
        model.addPoint(MapPoint(100f, 100f))
        assertTrue(model.saveDemoCourse())
        assertEquals("한강 새벽", model.uiState.value.course.name)
        assertEquals(2, model.uiState.value.course.points.size)
    }

    @Test
    fun reachingPointLimitPreservesPathAndUndoAllowsEditing() {
        val model = DalmoiDemoViewModel()
        model.resetDraft()
        repeat(50) { model.addPoint(MapPoint(it.toFloat(), 200f)) }
        val original = model.uiState.value.draftPoints
        model.addPoint(MapPoint(60f, 200f))
        assertEquals(original, model.uiState.value.draftPoints)
        assertEquals(DraftError.PointLimit, model.uiState.value.draftError)
        model.undoPoint()
        model.addPoint(MapPoint(60f, 200f))
        assertEquals(50, model.uiState.value.draftPoints.size)
        assertEquals(null, model.uiState.value.draftError)
    }

    @Test
    fun invalidCoordinatesCannotCorruptDisplayedDistance() {
        val model = DalmoiDemoViewModel()
        model.addPoint(MapPoint(Float.NaN, 1f))
        model.addPoint(MapPoint(2f, Float.POSITIVE_INFINITY))
        assertEquals(5, model.uiState.value.draftPoints.size)
        assertEquals(2860, model.uiState.value.draftDistanceMeters)
    }
}
