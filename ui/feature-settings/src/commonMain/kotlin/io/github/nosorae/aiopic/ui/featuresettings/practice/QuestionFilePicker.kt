package io.github.nosorae.aiopic.ui.featuresettings.practice

import androidx.compose.runtime.Composable

/**
 * 문항 파일(F6.3)을 고르는 시스템 창을 여는 함수를 돌려준다. 고르면 [onPick] 에 파일 내용을 넘기고, 읽지 못했으면 null 을 넘긴다.
 * 고르기를 취소하면 부르지 않는다. Android 는 문서 고르기, iOS 는 문서 선택기를 연다.
 */
@Composable
internal expect fun rememberQuestionFilePicker(onPick: (String?) -> Unit): () -> Unit
