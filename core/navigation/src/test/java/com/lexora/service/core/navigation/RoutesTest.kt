package com.lexora.service.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun bottomNavigationIsLimitedToHomeAndMenuByDefault() {
        assertTrue(Routes.usesBottomNavigation(Routes.Home))
        assertTrue(Routes.usesBottomNavigation(Routes.Menu))
        assertFalse(Routes.usesBottomNavigation(Routes.ProfileSettings))
        assertFalse(Routes.usesBottomNavigation(Routes.Requests))
        assertFalse(Routes.usesBottomNavigation(null))
    }

    @Test
    fun configuredSectionsAppearInBottomNavigation() {
        assertTrue(Routes.usesBottomNavigation(Routes.Clients, listOf(Routes.Clients, Routes.Reports)))
        assertFalse(Routes.usesBottomNavigation(Routes.Requests, listOf(Routes.Clients, Routes.Reports)))
    }

    @Test
    fun bottomMenuSelectionPreservesOrderAndLimitsToThreeAvailableRoutes() {
        assertEquals(
            listOf(Routes.Reports, Routes.Clients, Routes.Planning),
            Routes.normalizeBottomDestinations(
                listOf(Routes.Reports, Routes.Clients, Routes.Planning, Routes.Requests),
                listOf(Routes.Reports, Routes.Clients, Routes.Planning, Routes.Requests),
            ),
        )
    }

    @Test
    fun bottomMenuSelectionDropsDuplicatesAndUnavailableRoutes() {
        assertEquals(
            listOf(Routes.Clients, Routes.Reports),
            Routes.normalizeBottomDestinations(
                listOf(Routes.Clients, Routes.ProfileSettings, Routes.Clients, Routes.Reports),
                listOf(Routes.Clients, Routes.Reports),
            ),
        )
    }
}
