package io.github.nosorae.aiopic.libs.debug

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidPlatformTest {
    private val platform = AndroidPlatform(ApplicationProvider.getApplicationContext())

    @Test
    fun storesSwitchesAndCrashFiles() {
        assertNull(platform.readSwitch("slow"))
        platform.writeSwitch("slow", true)
        assertEquals(true, platform.readSwitch("slow"))

        platform.writeCrash("crash-1.txt", "boom")
        assertEquals(listOf("crash-1.txt"), platform.crashNames())
        assertEquals("boom", platform.readCrash("crash-1.txt"))
        platform.deleteCrash("crash-1.txt")
        assertEquals(emptyList(), platform.crashNames())

        assertTrue(platform.info().map { it.first }.containsAll(listOf("app", "os", "device", "locale", "free storage")))
    }
}
