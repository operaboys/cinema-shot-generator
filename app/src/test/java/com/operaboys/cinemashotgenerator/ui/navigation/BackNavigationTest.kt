package com.operaboys.cinemashotgenerator.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// واحد ۱۶ — فاز ۰: تست resolveContextualBackTarget (docs/design/README.md بخش
// Interactions: «Back navigation is contextual ... همه‌جای دیگر→Home»؛ فقط قانون
// Fallback این فاز فعال است، طبق docs/adr/042-...md).

class BackNavigationTest {

    @Test
    fun `null route means no navigation history yet, so no contextual target`() {
        assertNull(resolveContextualBackTarget(null))
    }

    @Test
    fun `Home itself has no contextual target, so the default system back applies`() {
        assertNull(resolveContextualBackTarget(Home::class.qualifiedName))
    }

    @Test
    fun `Projects falls back to Home, per the default rule`() {
        assertEquals(Home, resolveContextualBackTarget(Projects::class.qualifiedName))
    }

    @Test
    fun `Assets falls back to Home, per the default rule`() {
        assertEquals(Home, resolveContextualBackTarget(Assets::class.qualifiedName))
    }

    @Test
    fun `Studio falls back to Home, per the default rule (Composer-Shots-SceneDetail rules do not exist yet)`() {
        assertEquals(Home, resolveContextualBackTarget(Studio::class.qualifiedName))
    }
}
