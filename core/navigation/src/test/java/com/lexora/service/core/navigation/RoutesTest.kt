package com.lexora.service.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun bottomNavigationIsLimitedToHomeAndMenu() {
        assertTrue(Routes.usesBottomNavigation(Routes.Home))
        assertTrue(Routes.usesBottomNavigation(Routes.Menu))
        assertFalse(Routes.usesBottomNavigation(Routes.ProfileSettings))
        assertFalse(Routes.usesBottomNavigation(Routes.Requests))
        assertFalse(Routes.usesBottomNavigation(null))
    }
}
