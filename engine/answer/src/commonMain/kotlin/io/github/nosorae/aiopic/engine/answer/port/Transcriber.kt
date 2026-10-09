package io.github.nosorae.aiopic.engine.answer.port

import kotlin.time.Duration

/**
 * 두 단계 경로의 첫 단계. 녹음에서 전문과 단어마다 시각을 뽑는다(F5.9, ADR 0019).
 * 음성을 못 듣는 제공사를 쓸 때 기기 음성 인식이나 받아쓰기 전용 모델이 맡는다.
 */
interface Transcriber {
    suspend fun transcribe(recording: Recording): AnalysisResult<Transcript>
}

data class Transcript(val text: String, val words: List<TimedWord>)

/** 녹음 시작부터 잰 단어 하나의 시작과 끝. 두 단계 경로에서 멈춤과 말 속도를 앱이 계산하는 데 쓴다. */
data class TimedWord(val text: String, val start: Duration, val end: Duration)
