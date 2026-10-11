package io.github.nosorae.aiopic.ui.featurepractice

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question

/** 한 문항 연습이 쓰는 저장과 문항 풀. 앱 층이 답변 저장소, 내장 문항 풀, 녹음 폴더 위치를 묶어 구현하고, 테스트는 메모리로 구현한다. */
interface PracticeRepository {
    /** 낼 수 있는 질문 전부. 내장 풀과 사용자가 불러온 파일(F6.3)을 합친 것이다. */
    suspend fun pool(): List<Question>

    /** 지금까지 남긴 답변 전부. 모의고사과 한 문항 연습 답변을 함께 본다. */
    suspend fun answers(): List<Answer>

    /** 사용자가 "틀림"을 누른 교정(F8.1). 그 답변은 약점 점수에서 빠진다. */
    suspend fun flaggedCorrections(): Set<CorrectionFlag>

    /** 답변 하나의 녹음 조각을 쓰는 폴더. */
    fun recordingDirectory(answerId: String): String

    /** 답변을 저장하고 분석 대기열에 넣는다(F5.1). */
    suspend fun save(answer: Answer)
}
