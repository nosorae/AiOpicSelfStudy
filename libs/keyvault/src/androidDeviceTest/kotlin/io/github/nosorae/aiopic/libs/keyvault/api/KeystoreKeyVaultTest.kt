package io.github.nosorae.aiopic.libs.keyvault.api

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class KeystoreKeyVaultTest : KeyVaultContractTest() {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    override fun createVault(): KeyVault = KeystoreKeyVault(context)

    @Test
    fun `F1_2_저장_파일에_평문_키가_남지_않는다`() = runTest {
        val vault = createVault()
        vault.put("gemini", "plain-test-key")

        val stored = context.preferencesDataStoreFile(KeystoreKeyVault.FILE_NAME).readBytes().decodeToString()

        assertFalse("plain-test-key" in stored)
        vault.delete("gemini")
    }
}
