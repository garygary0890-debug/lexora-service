package com.lexora.service.feature.planning

import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningEntryDraftTest {
    @Test
    fun draft_maps_to_planned_request_command() {
        val command = PlanningEntryDraft(
            title = "  Замена масла  ",
            description = "  Плановый визит ",
            startAtEpochMs = 1000L,
            endAtEpochMs = 2000L,
            employeeId = "employee-1",
            branchId = "branch-1",
            teamName = "  Мобильная бригада "
        ).toSaveRequestCommand()

        assertEquals("Замена масла", command.title)
        assertEquals("Плановый визит", command.description)
        assertEquals(1000L, command.plannedAtEpochMs)
        assertEquals(2000L, command.plannedEndEpochMs)
        assertEquals("employee-1", command.assigneeEmployeeId)
        assertEquals("branch-1", command.branchId)
        assertEquals("Мобильная бригада", command.assigneeTeamName)
    }
}
