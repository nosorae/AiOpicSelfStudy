package io.github.nosorae.aiopic.ui.featureonboarding.consent

class FakeVoiceConsent(var given: Boolean = false) : VoiceConsent {
    override suspend fun isGiven(): Boolean = given

    override suspend fun give() {
        given = true
    }
}
