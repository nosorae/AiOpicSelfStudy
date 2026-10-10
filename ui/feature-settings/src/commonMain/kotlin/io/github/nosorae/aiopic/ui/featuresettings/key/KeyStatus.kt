package io.github.nosorae.aiopic.ui.featuresettings.key

import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider

/**
 * 설정의 제공사별 키 관리 한 줄(F1.2). 키 값은 화면에 들고 오지 않고 [maskKey] 로 가린 값만 든다.
 * 저장된 키가 없으면 [maskedKey] 가 null 이다.
 */
data class KeyStatus(val provider: KeyProvider, val maskedKey: String?) {
    val saved: Boolean get() = maskedKey != null
}

/** 어느 키를 넣었는지 알아볼 만큼만 남긴다: 앞 3자와 끝 4자. 그보다 짧으면 남김없이 가린다. */
fun maskKey(key: String): String = if (key.length <= 7) "…" else "${key.take(3)}…${key.takeLast(4)}"
