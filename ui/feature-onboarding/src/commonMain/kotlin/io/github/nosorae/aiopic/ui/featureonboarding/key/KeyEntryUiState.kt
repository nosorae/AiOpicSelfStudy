package io.github.nosorae.aiopic.ui.featureonboarding.key

import io.github.nosorae.aiopic.ui.featureonboarding.DomainError

data class KeyEntryUiState(
    val providers: List<KeyProvider> = KeyProvider.entries,
    val selectedProvider: KeyProvider? = null,
    val key: String = "",
    val checking: Boolean = false,
    /** 마지막 확인이 실패한 이유. 키를 고치면 지운다. */
    val error: DomainError? = null,
    /** 키가 확인돼 저장됐다. 다음 화면으로 넘어가기 전까지 저장 버튼 대신 확인 체크를 보인다. */
    val saved: Boolean = false,
    val navigation: KeyEntryDestination? = null,
) {
    /** 키를 저장할 제공사. 제공사가 하나뿐이면 고르지 않아도 그 제공사다(첫 버전은 Gemini 하나). */
    val provider: KeyProvider? get() = selectedProvider ?: providers.singleOrNull()

    val canSubmit: Boolean get() = provider != null && key.isNotBlank() && !checking && !saved

    /** 키 받기 버튼이 열 제공사. 고르기 전에는 목록 첫 제공사다. */
    val keyPageProvider: KeyProvider get() = provider ?: providers.first()
}

/** 키 확인이 끝나야 가는 이동만 상태로 둔다(ADR 0009). 어디로 갈지는 화면을 등록한 쪽이 정한다. */
enum class KeyEntryDestination { KeySaved, }

sealed interface KeyEntryIntent {
    data class SelectProvider(val provider: KeyProvider) : KeyEntryIntent

    data class ChangeKey(val key: String) : KeyEntryIntent

    data object SubmitKey : KeyEntryIntent

    data object NavigationHandled : KeyEntryIntent
}
