package io.github.nosorae.aiopic.libs.keyvault.api

class KeychainKeyVaultTest : KeyVaultContractTest() {
    override fun createVault(): KeyVault = KeychainKeyVault()
}
