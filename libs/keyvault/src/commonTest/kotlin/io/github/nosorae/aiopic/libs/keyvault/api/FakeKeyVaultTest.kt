package io.github.nosorae.aiopic.libs.keyvault.api

class FakeKeyVaultTest : KeyVaultContractTest() {
    override fun createVault(): KeyVault = FakeKeyVault()
}
