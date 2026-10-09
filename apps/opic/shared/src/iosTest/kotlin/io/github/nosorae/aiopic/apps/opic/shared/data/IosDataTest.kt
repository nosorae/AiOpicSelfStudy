package io.github.nosorae.aiopic.apps.opic.shared.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosDataTest {
    @Test
    fun `녹음 폴더는 앱 지원 폴더의 recordings 이고 백업에서 빠진다`() {
        val root = recordingsRoot()

        assertTrue(root.endsWith("/Application Support/recordings"), root)
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(root))
        val excluded = memScoped {
            val value = alloc<ObjCObjectVar<Any?>>()
            NSURL.fileURLWithPath(root).getResourceValue(value.ptr, NSURLIsExcludedFromBackupKey, null)
            (value.value as NSNumber).boolValue
        }
        assertEquals(true, excluded)
    }
}
