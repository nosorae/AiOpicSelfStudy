package io.github.nosorae.aiopic.libs.workqueue.api

import kotlin.time.Duration.Companion.seconds

/** [WorkResult.Retry] 를 받으면 처음 한 번 뒤로 이만큼 더 보낸다(F5.6). */
internal const val MAX_RETRIES = 3

/** 첫 재시도 전 대기. 다음부터 두 배씩 늘린다. 무료 키의 분당 한도가 풀리기를 기다리는 시간이다. */
internal val FIRST_BACKOFF = 30.seconds
