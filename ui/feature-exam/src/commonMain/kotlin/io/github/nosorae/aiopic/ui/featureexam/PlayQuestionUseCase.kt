package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.tts.api.Tts
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 질문을 기기 음성으로 끝까지 읽고 녹음을 시작한다(F2.3). 이 단계에 녹음 조각이 이미 있으면 이어 녹음이다.
 * 이어 녹음인데 다시 듣기를 이미 썼으면 질문을 읽지 않고 바로 녹음한다(F3.3). 다시 듣기도 이어 녹음과 같은 길로 온다.
 */
@Inject
class PlayQuestionUseCase(
    private val repository: ExamRepository,
    private val tts: Tts,
    private val recorder: Recorder,
) {
    /**
     * 질문을 받는 데 [LoadingNoticeDelay] 넘게 걸리면 [onNotice] 로 [making] 을, [SlowNoticeDelay] 넘게 걸리면 오래 걸린다고 알린다.
     * 질문을 받으면 안내를 지우고, 내장 문항으로 바꿨으면 그렇게 알린다.
     */
    suspend operator fun invoke(session: ExamSession, making: QuestionNotice.Making? = null, onNotice: (QuestionNotice?) -> Unit = {}): PlayResult {
        val picked = coroutineScope {
            val waiting = launch {
                delay(LoadingNoticeDelay)
                making?.let(onNotice)
                delay(SlowNoticeDelay - LoadingNoticeDelay)
                onNotice(QuestionNotice.Slow)
            }
            repository.question(session).also { waiting.cancel() }
        }
        onNotice(if (picked?.fellBack == true) QuestionNotice.BuiltIn else null)
        val question = picked?.question ?: return PlayResult.Failed(ExamError.QuestionUnavailable)
        val directory = repository.recordingDirectory(session)
        val resumed = recorder.chunks(directory).isNotEmpty()
        if (!(resumed && session.replayUsed) && !tts.speak(question.text)) return PlayResult.Failed(ExamError.SpeechFailed)
        recorder.start(directory)?.let { return PlayResult.Failed(it.toExamError()) }
        return PlayResult.Recording(resumed)
    }
}

sealed interface PlayResult {
    /** 녹음이 시작됐다. [resumed] 면 이 단계의 녹음을 이어 쓴다. */
    data class Recording(val resumed: Boolean) : PlayResult

    data class Failed(val error: ExamError) : PlayResult
}

/** 질문이 바로 오면 안내가 깜빡이지 않게 이만큼 기다린 뒤에 불러오는 중을 보인다. */
val LoadingNoticeDelay = 500.milliseconds

/** Play 뒤 이만큼 지나도 질문이 안 오면 연결 상태를 보라고 안내를 바꾼다. */
val SlowNoticeDelay = 10.seconds
