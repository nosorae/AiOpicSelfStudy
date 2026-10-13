package io.github.nosorae.aiopic.buildlogic

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseSigningTest {

    private val dir = createTempDirectory().toFile()

    private fun propsFile(vararg lines: String) = File(dir, "signing.properties").apply { writeText(lines.joinToString("\n")) }

    @Test
    fun noPathMeansUnsignedWithReason() {
        val reason = readReleaseSigning(null).exceptionOrNull()?.message.orEmpty()
        assertTrue(RELEASE_SIGNING_KEY in reason, reason)
    }

    @Test
    fun missingFileMeansUnsignedWithReason() {
        val reason = readReleaseSigning(File(dir, "none.properties").path).exceptionOrNull()?.message.orEmpty()
        assertTrue("none.properties" in reason, reason)
    }

    @Test
    fun missingFieldsAreNamed() {
        val file = propsFile("storeFile=upload.jks", "keyAlias=upload")
        val reason = readReleaseSigning(file.path).exceptionOrNull()?.message.orEmpty()
        assertTrue("storePassword" in reason && "keyPassword" in reason, reason)
    }

    @Test
    fun relativeStoreFileIsNextToPropertiesFile() {
        File(dir, "upload.jks").writeText("x")
        val file = propsFile("storeFile=upload.jks", "storePassword=a", "keyAlias=upload", "keyPassword=b")
        val signing = readReleaseSigning(file.path).getOrThrow()
        assertEquals(File(dir, "upload.jks").canonicalFile, signing.storeFile.canonicalFile)
        assertEquals("upload", signing.keyAlias)
        assertEquals("a", signing.storePassword)
        assertEquals("b", signing.keyPassword)
    }

    @Test
    fun missingKeystoreIsReported() {
        val file = propsFile("storeFile=gone.jks", "storePassword=a", "keyAlias=upload", "keyPassword=b")
        val reason = readReleaseSigning(file.path).exceptionOrNull()?.message.orEmpty()
        assertTrue("gone.jks" in reason, reason)
    }
}
