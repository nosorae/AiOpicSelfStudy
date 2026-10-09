package io.github.nosorae.aiopic.engine.answer.port

import kotlin.time.Duration

/** 분석에 보낼 녹음. 답변 하나의 조각을 기기에서 이어 붙인 파일 내용이다. */
class Recording(
    val bytes: ByteArray,
    val mimeType: String,
    /** 이어 녹음을 시작한 위치. 이 구간을 말 멈춤으로 세지 않게 요청에 표시한다(F3.4). */
    val resumedAt: List<Duration>,
)

/** 시험 팩이 주는 프롬프트 하나. [version] 은 결과마다 promptVersion 으로 남는다(F5.7). */
data class Prompt(
    val version: String,
    val text: String,
    /** 응답 JSON 스키마. */
    val responseSchema: String,
)
