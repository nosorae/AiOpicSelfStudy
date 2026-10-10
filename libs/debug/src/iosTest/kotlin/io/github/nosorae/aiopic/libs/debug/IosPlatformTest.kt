package io.github.nosorae.aiopic.libs.debug

import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosPlatformTest {
    private val platform = IosPlatform(NSTemporaryDirectory() + "debug-test-" + NSUUID().UUIDString)

    @Test
    fun storesSwitchesAndCrashFiles() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("debug_tools.slow")
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
