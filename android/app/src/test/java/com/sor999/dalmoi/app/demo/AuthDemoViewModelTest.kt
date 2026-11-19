package com.sor999.dalmoi.app.demo

import com.sor999.dalmoi.presentation.common.map.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthDemoViewModelTest {
    @Test
    fun loginValidationKeepsEmailAndClearsPasswordOnContinue() {
        val model = DalmoiDemoViewModel()
        assertFalse(model.enterDemo())
        model.changePassword("preview-password")
        assertTrue(model.enterDemo())
        assertEquals("minjun@dalmoi.app", model.uiState.value.email)
        assertEquals("", model.uiState.value.password)
    }
}
