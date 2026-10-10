package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.session.model.ExamSession

/**
 * 시험 화면이 쓰는 저장과 출제. 앱 층이 회차 저장소, 출제, 녹음 폴더 위치를 묶어 구현하고, 테스트는 메모리로 구현한다.
 * 회차 상태는 바뀔 때마다 통째로 저장하고 앱을 켜면 여기서 다시 읽는다(F3.1).
 */
interface ExamRepository {
    suspend fun session(id: String): ExamSession?

    suspend fun save(session: ExamSession)

    /**
     * 회차의 지금 단계(연습 문항 또는 채점 문항)에 낼 질문. 같은 단계에는 언제 불러도 같은 질문을 돌려줘야 한다.
     * 앱을 다시 켜고 이어 볼 때 다른 질문이 나오면 안 된다. 낼 질문이 없으면 null 이다.
     * 모델 출제를 기다리느라 오래 걸릴 수 있다. 그동안 화면은 불러오는 중 안내를 보인다.
     */
    suspend fun question(session: ExamSession): ExamQuestion?

    /** 회차의 지금 단계 녹음 조각을 쓰는 폴더. 같은 단계에는 같은 폴더라서 다시 켜면 이어 녹음한다(F3.3). */
    fun recordingDirectory(session: ExamSession): String

    /** 답변을 저장하고 분석 대기열에 넣는다(F5.1). */
    suspend fun save(answer: Answer)
}

/** 낼 질문. [fellBack] 이면 모델 출제가 실패해서 이번에 내장 문항으로 바꿨다(F6.2). 화면이 짧게 알린다. */
data class ExamQuestion(val question: Question, val fellBack: Boolean = false)
