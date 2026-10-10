package io.github.nosorae.aiopic.engine.question.pick

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * 한 회차 질문 가운데 [part] 위치의 몫을 고른다. 먼저 [generate] 로 만든 질문(F6.1)을 쓰고, 생성이 실패(null)했거나
 * [checkQuestions] 를 통과 못하면 회차 전체를 [pool] 에서 서베이 주제로 고른 목록의 같은 위치를 쓴다(F6.2).
 * 풀에서 고를 때 늘 회차 전체를 같은 [random] 씨앗으로 고르므로, 몫마다 따로 불러도 몫끼리 같은 문항이 겹치지 않는다.
 * 풀에도 채울 문항이 없는 유형이 있으면 null 이다.
 *
 * [order] 는 이번 회차에 낼 문항 유형 순서 전체다. 시험 팩의 단계와 고른 수준으로 부르는 쪽이 만든다.
 * [generate] 는 부르는 쪽이 주제, 수준, 재조정, 약점 유형을 프롬프트에 채워 [part] 몫만 모델에 받는 함수다.
 * 엔진은 시험 이름도 제공사도 모르고 결과 모양만 본다.
 */
suspend fun pickQuestions(
    order: List<QuestionType>,
    topics: List<String>,
    pool: List<Question>,
    maxOutsideTopics: Int,
    random: Random = Random,
    part: IntRange = order.indices,
    generate: suspend () -> List<Question>?,
): List<Question>? = generate()?.takeIf { checkQuestions(it, order.slice(part), topics, maxOutsideTopics).isEmpty() }
    ?: pickFromPool(pool, order, topics, random)?.slice(part)

/**
 * 생성한 질문이 [order] 와 같은 유형 순서인지, 빈 질문이 없는지, 서베이 밖 주제가 [maxOutsideTopics] 개 이하인지 본다.
 * 서베이 밖 주제를 0개로 막지 않는 것은 자기소개, 롤플레이 상황처럼 서베이와 상관없는 주제가 시험마다 몇 개 있어서다.
 * 그 수는 시험 팩이 정한다. 문제가 없으면 빈 목록이다.
 */
fun checkQuestions(questions: List<Question>, order: List<QuestionType>, topics: List<String>, maxOutsideTopics: Int): List<String> {
    if (questions.map { it.type } != order) return listOf("유형 순서가 다르다: ${questions.map { it.type.id }}")
    val blank = questions.filter { it.text.isBlank() || it.topic.isBlank() }.map { "빈 질문: ${it.id}" }
    val outside = questions.map { it.topic }.toSet() - topics.toSet()
    return blank + listOfNotNull("서베이 밖 주제가 ${outside.size}개다: $outside".takeIf { outside.size > maxOutsideTopics })
}

/**
 * 풀에서 [order] 의 유형마다 아직 안 낸 문항 하나를 고른다(F6.2). 바로 앞 문항과 같은 주제를 먼저 고르고,
 * 없으면 서베이 주제를 섞은 순서대로, 그래도 없으면 서베이 밖 주제에서 고른다.
 * 앞 문항 주제를 먼저 보니 묘사, 습관, 경험 같은 3문항 세트와 롤플레이 셋이 한 주제로 묶인다.
 */
fun pickFromPool(pool: List<Question>, order: List<QuestionType>, topics: List<String>, random: Random): List<Question>? {
    val rank = topics.shuffled(random).withIndex().associate { (index, topic) -> topic to index }
    val used = mutableSetOf<String>()
    var last: String? = null
    return order.map { type ->
        pool.filter { it.type == type && it.id !in used }
            .shuffled(random)
            .minByOrNull { if (it.topic == last) -1 else rank[it.topic] ?: rank.size }
            ?.also {
                used += it.id
                last = it.topic
            }
            ?: return null
    }
}

/**
 * 문항 파일(F6.3)을 읽는다. 내장 풀과 사용자가 불러온 파일이 같은 모양, [Question] 의 JSON 목록이다.
 * 빈 목록, 빈 칸, 겹친 id, 깨진 JSON 은 [IllegalArgumentException] 으로 거절한다. 화면은 그 메시지를 보여 준다.
 */
fun parseQuestions(json: String): List<Question> {
    val questions = Json.decodeFromString<List<Question>>(json)
    require(questions.isNotEmpty()) { "문항이 없다" }
    questions.forEach { require(listOf(it.id, it.text, it.type.id, it.topic).none(String::isBlank)) { "빈 칸이 있다: $it" } }
    val duplicated = questions.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
    require(duplicated.isEmpty()) { "id 가 겹친다: $duplicated" }
    return questions
}
