package com.lexora.service.feature.assets

import kotlin.test.Test
import kotlin.test.assertEquals

class AssetsScreenLayoutTest {
    @Test
    fun contentPaddingRemovesOnlyTheTopGap() {
        assertEquals(
            AssetsContentPaddingDp(horizontal = 20, top = 0, bottom = 20),
            assetsContentPaddingDp(),
        )
    }
}
