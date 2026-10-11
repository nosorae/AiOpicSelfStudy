package io.github.nosorae.aiopic.engine.question.model

import kotlinx.serialization.Serializable

/**
 * 시험 팩이 정하는 서베이 한 벌(F2.2). [questions] 는 하나만 고르는 질문이고 고른 보기에 따라 하위 질문이 이어진다.
 * [groups] 는 여러 개 고르는 주제 묶음이다. 묶음마다 최소 개수가 있고, 모든 묶음에서 고른 주제가 합쳐서 [minTopics] 개 이상이어야 한다.
 * 고른 주제 id 가 출제 프롬프트와 내장 문항 풀의 주제다(F6.1, F6.2).
 */
data class Survey(val questions: List<SurveyQuestion>, val groups: List<TopicGroup>, val minTopics: Int) {
    /** 고른 답([choices], 질문 id 에서 보기 id)으로 지금 보여야 하는 질문을 순서대로 낸다. 고른 보기의 하위 질문은 그 질문 바로 뒤에 온다. */
    fun visibleQuestions(choices: Map<String, String>): List<SurveyQuestion> {
        fun expand(question: SurveyQuestion): List<SurveyQuestion> = listOf(question) + question.options.firstOrNull { it.id == choices[question.id] }?.next.orEmpty().flatMap(::expand)
        return questions.flatMap(::expand)
    }

    /** 묶음마다 최소 개수를 채웠고 합쳐서 [minTopics] 개 이상이면 받는다. */
    fun accepts(topics: Collection<String>): Boolean = topics.size >= minTopics && groups.all { group -> group.topics.count { it.id in topics } >= group.min }

    /** 고른 주제와 같은 묶음에 있는 나머지 주제. 고른 주제의 내장 문항이 없을 때 서베이 밖 주제보다 먼저 쓴다(F6.2). */
    fun nearTopics(topics: Collection<String>): List<String> = groups.filter { group -> group.topics.any { it.id in topics } }.flatMap { group -> group.topics.map { it.id } }.filterNot { it in topics }
}

/** 하나만 고르는 질문. */
data class SurveyQuestion(val id: String, val text: String, val options: List<SurveyOption>)

/** 보기 하나. 이 보기를 고르면 [next] 질문이 바로 다음 화면들로 나온다. */
data class SurveyOption(val id: String, val label: String, val next: List<SurveyQuestion> = emptyList())

/** 여러 개 고르는 주제 묶음. 화면 하나에 묶음 하나를 그린다. */
data class TopicGroup(val id: String, val title: String, val min: Int, val topics: List<Topic>)

/** 고를 수 있는 주제. [id] 는 내장 문항 풀의 주제 id 와 같은 값을 쓴다. */
data class Topic(val id: String, val label: String)

/** 저장한 서베이 답. 다음 모의고사 서베이를 미리 채우고 한 문항 연습 주제에 쓴다. */
@Serializable
data class SurveyAnswers(val choices: Map<String, String> = emptyMap(), val topics: List<String> = emptyList())
