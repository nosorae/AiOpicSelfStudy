package io.github.nosorae.aiopic.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayerRulesTest {

    private fun violations(from: String, vararg to: String) = layerViolations(mapOf(from to to.toList()))

    @Test
    fun libsCannotDependOnAnyProjectModule() {
        assertEquals(1, violations(":libs:tts", ":libs:timer").size)
        assertEquals(1, violations(":libs:tts", ":engine:session").size)
    }

    @Test
    fun engineMayDependOnLibsAndOtherEngines() {
        assertTrue(violations(":engine:session", ":libs:timer", ":engine:answer").isEmpty())
        assertEquals(1, violations(":engine:session", ":packs:opic-en").size)
        assertEquals(1, violations(":engine:session", ":ui:design").size)
    }

    @Test
    fun packsMayDependOnEngineAndLibsOnly() {
        assertTrue(violations(":packs:opic-en", ":engine:question", ":libs:tts").isEmpty())
        assertEquals(1, violations(":packs:opic-en", ":ui:design").size)
    }

    @Test
    fun uiMayNotDependOnPacks() {
        assertTrue(violations(":ui:feature-exam", ":ui:design", ":engine:session", ":libs:recorder").isEmpty())
        assertEquals(1, violations(":ui:feature-exam", ":packs:opic-en").size)
        assertEquals(1, violations(":ui:feature-exam", ":apps:opic:shared").size)
    }

    @Test
    fun appsMayDependOnEverything() {
        assertTrue(
            violations(":apps:opic:shared", ":ui:design", ":packs:opic-en", ":engine:session", ":libs:tts", ":apps:opic:other")
                .isEmpty(),
        )
    }

    @Test
    fun toolsMayDependOnEnginePacksLibs() {
        assertTrue(violations(":tools:eval", ":engine:answer", ":packs:opic-en", ":libs:llm-gemini").isEmpty())
        assertEquals(1, violations(":tools:eval", ":ui:design").size)
    }

    @Test
    fun unknownLayerIsAViolation() {
        assertEquals(1, violations(":misc:thing").size)
    }

    @Test
    fun messageNamesBothModulesAndTheFix() {
        val message = violations(":ui:feature-exam", ":packs:opic-en").single()
        assertTrue(":ui:feature-exam" in message, message)
        assertTrue(":packs:opic-en" in message, message)
        assertTrue("고칠 방법" in message, message)
    }

    @Test
    fun namespaceComesFromModulePath() {
        assertEquals("io.github.nosorae.aiopic.ui.featureonboarding", namespaceOf(":ui:feature-onboarding"))
        assertEquals("io.github.nosorae.aiopic.apps.opic.shared", namespaceOf(":apps:opic:shared"))
        assertEquals("io.github.nosorae.aiopic.packs.opicen", namespaceOf(":packs:opic-en"))
        assertEquals("io.github.nosorae.aiopic.apps.opic.androidapp", namespaceOf(":apps:opic:androidApp"))
    }
}
