package com.sor999.dalmoi.app.demo

import com.sor999.dalmoi.presentation.common.map.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrewDemoViewModelTest {
    @Test
    fun invalidInviteKeepsCodeAndValidInviteClearsError() {
        val model = DalmoiDemoViewModel()
        model.changeInviteCode("wrong")
        assertFalse(model.joinDemoCrew())
        assertEquals("wrong", model.uiState.value.inviteCode)
        model.changeInviteCode("rs-7k2q")
        assertTrue(model.joinDemoCrew())
        assertFalse(model.uiState.value.inviteError)
    }
}
