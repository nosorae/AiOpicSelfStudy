package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.runtime.Composable

/** 시스템에서 동작 줄이기(iOS)나 애니메이션 제거(Android 애니메이션 배율 0)를 켰는지. 켜져 있으면 반복 애니메이션을 멈춘다. */
@Composable
internal expect fun rememberReduceMotion(): Boolean
