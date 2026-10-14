package com.lexora.service.feature.planning

import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningAddActionTest {
    @Test
    fun empty_calendar_shows_centered_add_button() {
        assertEquals(
            PlanningAddActionPlacement.EMPTY_STATE,
            planningAddActionPlacement(selectedTab = 0, eventCount = 0),
        )
    }

    @Test
    fun populated_calendar_shows_fab() {
        assertEquals(
            PlanningAddActionPlacement.FAB,
            planningAddActionPlacement(selectedTab = 0, eventCount = 1),
        )
    }

    @Test
    fun workload_keeps_fab_for_empty_and_populated_lists() {
        assertEquals(
            PlanningAddActionPlacement.FAB,
            planningAddActionPlacement(selectedTab = 1, eventCount = 0),
        )
        assertEquals(
            PlanningAddActionPlacement.FAB,
            planningAddActionPlacement(selectedTab = 1, eventCount = 1),
        )
    }
}
