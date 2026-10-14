package com.lexora.service.feature.planning

internal enum class PlanningAddActionPlacement {
    EMPTY_STATE,
    FAB,
    NONE,
}

internal fun planningAddActionPlacement(selectedTab: Int, eventCount: Int): PlanningAddActionPlacement =
    when {
        selectedTab == 0 && eventCount == 0 -> PlanningAddActionPlacement.EMPTY_STATE
        selectedTab == 0 || selectedTab == 1 -> PlanningAddActionPlacement.FAB
        else -> PlanningAddActionPlacement.NONE
    }
