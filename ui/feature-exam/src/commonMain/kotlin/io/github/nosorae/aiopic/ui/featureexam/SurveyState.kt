package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.question.model.Survey
import io.github.nosorae.aiopic.engine.question.model.SurveyAnswers
import io.github.nosorae.aiopic.engine.question.model.SurveyQuestion
import io.github.nosorae.aiopic.engine.question.model.TopicGroup

/**
 * 서베이를 한 화면에 질문 하나씩 넘기는 상태. 모의고사의 서베이 단계와 설정의 서베이가 같이 쓴다(F2.2).
 * 화면 순서는 지금 보여야 하는 질문(고른 답에 따라 하위 질문이 끼어든다) 다음에 주제 묶음 하나씩이다.
 * 하나만 고르는 질문은 고르는 즉시 다음 화면으로 가고, 묶음 화면은 그 묶음의 최소 개수를 채워야 다음으로 간다.
 */
data class SurveyState(
    val survey: Survey,
    /** 질문 id 에서 고른 보기 id. 앞으로 돌아가 다른 보기를 고르면 안 보이게 된 하위 질문의 답은 남아 있어도 [answers] 에서 빠진다. */
    val choices: Map<String, String> = emptyMap(),
    val topics: Set<String> = emptySet(),
    /** 지금 화면의 위치. 0 부터 센다. */
    val screen: Int = 0,
) {
    val questions: List<SurveyQuestion> get() = survey.visibleQuestions(choices)

    val screenCount: Int get() = questions.size + survey.groups.size

    /** 지금 화면이 하나만 고르는 질문이면 그 질문, 묶음 화면이면 null 이다. */
    val question: SurveyQuestion? get() = questions.getOrNull(screen)

    val group: TopicGroup? get() = survey.groups.getOrNull(screen - questions.size)?.takeIf { screen >= questions.size }

    val isLast: Boolean get() = screen == screenCount - 1

    /** 맨 위 진행 막대의 값. 하위 질문이 끼어들면 전체 화면 수가 늘어 조금 줄 수 있다. */
    val progress: Float get() = (screen + 1f) / screenCount

    /** 지금 묶음 화면에서 다음으로 갈 수 있는지. 마지막 화면이면 모든 조건([canSubmit])을 본다. */
    val canNext: Boolean get() = if (isLast) canSubmit else group?.let { selectedIn(it) >= it.min } ?: (question?.id in choices)

    val canSubmit: Boolean get() = questions.all { it.id in choices } && survey.accepts(topics)

    fun selectedIn(group: TopicGroup): Int = group.topics.count { it.id in topics }

    fun choose(option: String): SurveyState = question?.let { copy(choices = choices + (it.id to option), screen = screen + 1) } ?: this

    fun toggle(topic: String): SurveyState = copy(topics = if (topic in topics) topics - topic else topics + topic)

    fun next(): SurveyState = if (canNext && !isLast) copy(screen = screen + 1) else this

    fun back(): SurveyState = copy(screen = (screen - 1).coerceAtLeast(0))

    /** 저장할 답. 지금 보이는 질문의 답만 남기고 주제는 서베이 순서대로 둔다. */
    fun answers(): SurveyAnswers = SurveyAnswers(
        choices = questions.mapNotNull { question -> choices[question.id]?.let { question.id to it } }.toMap(),
        topics = survey.groups.flatMap { group -> group.topics.map { it.id } }.filter { it in topics },
    )

    companion object {
        /** 저장한 답으로 미리 채운다. 서베이에 없어진 주제는 뺀다. 첫 화면부터 다시 넘기며 고른 답을 확인한다. */
        fun prefilled(survey: Survey, saved: SurveyAnswers?): SurveyState {
            val ids = survey.groups.flatMap { group -> group.topics.map { it.id } }.toSet()
            return SurveyState(survey, saved?.choices.orEmpty(), saved?.topics.orEmpty().filter { it in ids }.toSet())
        }
    }
}
