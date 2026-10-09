package io.github.nosorae.aiopic.ui.featuresettings.key

import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider

/** 설정의 제공사별 키 관리 한 줄(F1.2). 키 값은 화면에 들고 오지 않고 저장 여부만 본다. */
data class KeyStatus(val provider: KeyProvider, val saved: Boolean)
