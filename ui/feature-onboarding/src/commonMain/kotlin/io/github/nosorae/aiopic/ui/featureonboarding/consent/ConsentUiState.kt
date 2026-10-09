package io.github.nosorae.aiopic.ui.featureonboarding.consent

data class ConsentUiState(val navigation: ConsentDestination? = null)

/** 동의를 저장해야 가는 이동이라 상태로 둔다(ADR 0009). 어디로 갈지는 화면을 등록한 쪽이 정한다. */
enum class ConsentDestination { ConsentGiven, }

sealed interface ConsentIntent {
    data object GiveConsent : ConsentIntent

    data object NavigationHandled : ConsentIntent
}
