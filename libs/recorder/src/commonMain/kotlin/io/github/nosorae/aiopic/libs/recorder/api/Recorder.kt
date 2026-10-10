package io.github.nosorae.aiopic.libs.recorder.api

import kotlin.time.Duration

/**
 * 답변 하나를 몇 초 단위 조각 파일로 녹음한다(F3.2, F3.3). 답하는 도중 앱이 꺼져도 쓰던 조각 하나만 잃는다.
 * 녹음기는 답변 하나 동안 멈추지 않고 쓰는 파일만 바꾼다. 멈췄다 다시 열면 그 사이 약 0.07초가 빠져 음절이 잘린다.
 * 전화 같은 끼어듦이 끝나면 새 조각부터 이어 쓴다.
 */
public interface Recorder {
    /**
     * [directory] 에 조각을 녹음한 순서대로 쓰기 시작한다. 이미 조각이 있으면 그 뒤 번호부터 이어 쓴다.
     * 시작했으면 null, 못 했으면 그 이유다.
     */
    public suspend fun start(directory: String): RecordingFailure?

    /**
     * 녹음을 멈추고 지금 폴더의 조각 경로를 녹음한 순서대로 돌려준다.
     * [start] 뒤로 조각을 쓰지 못했거나 끼어듦 뒤 마이크를 다시 열지 못한 적이 있으면 [StoppedRecording.failure] 에 담는다.
     * 그 사이 소리는 조각에 없다.
     */
    public suspend fun stop(): StoppedRecording

    /** [directory] 에 남아 있는 조각 경로를 녹음한 순서대로 돌려준다. 앱이 죽었다 켜진 뒤 조각 목록을 되찾을 때 쓴다. */
    public suspend fun chunks(directory: String): List<String>

    /**
     * 조각을 순서대로 이어 붙여 [output] 파일 하나로 만든다. 분석 요청에는 이 파일 하나를 보낸다.
     * 돌려주는 값은 이어 녹음을 시작한 위치(다시 [start] 했거나 끼어듦 뒤 새로 쓴 조각의 시작)를 [output] 기준으로 잰 것이다.
     * 분석이 이 위치를 말 멈춤으로 세지 않게 요청에 넣는다(F3.4). 조각 길이와 끊긴 곳은 녹음기만 알아서 여기서 계산한다.
     */
    public suspend fun merge(chunks: List<String>, output: String): List<Duration>
}

public data class StoppedRecording(val chunks: List<String>, val failure: RecordingFailure? = null)

public enum class RecordingFailure {
    /** 마이크 권한이 없거나 다른 앱이 마이크를 쓰고 있어 열지 못했다. */
    MicUnavailable,

    /** 조각 파일을 만들거나 쓰지 못했다. 저장 공간이 모자랄 때 생긴다. */
    WriteFailed,
}
