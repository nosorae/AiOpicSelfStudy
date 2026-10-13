package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.ContentFix
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Upgrade
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 스토어 스크린샷용 예시 기록(#236). 등급이 오르는 끝난 회차 셋과 한 문항 연습 둘이다. 사용자 데이터가 아니라 지어낸 예시라서
 * iOS 는 XCUITest 실행 인자(`ui-test-store-demo`), Android 는 스크린샷 테스트에서만 부른다.
 * [pool] 은 내장 문항 풀이다. 호스트 JVM 테스트는 팩 리소스를 못 읽어 파일에서 읽어 넘긴다.
 */
internal suspend fun OpicStore.seedStoreDemo(pool: List<Question>) {
    val types = OpicEn.examSpec.steps.filterIsInstance<ExamStep.Ask>().map { it.questionType }
    DEMO_SESSIONS.forEachIndexed { index, demo ->
        val byType = pool.groupBy { it.type }
        var sets = 0
        val questions = types.mapIndexed { order, type ->
            val sameType = byType.getValue(type)
            if (type.id !in SET_TYPES) return@mapIndexed sameType[(index + order) % sameType.size]
            val topic = demo.topics[sets++ / 3]
            sameType.first { it.topic == topic }
        }
        val answers = questions.mapIndexed { order, question ->
            val score = demo.baseScore + SCORE_SPREAD[order % SCORE_SPREAD.size]
            Answer(
                id = "demo-${index + 1}-${order + 1}",
                question = question,
                examSessionId = "demo-${index + 1}",
                chunkPaths = emptyList(),
                analysis = AnswerAnalysis.Done(FLASH, if (question.id == SHOWCASE_QUESTION) showcase else plain.copy(score = score)),
            )
        }
        answers.forEach { save(it) }
        save(
            ExamSession(
                id = "demo-${index + 1}",
                stepIndex = OpicEn.examSpec.steps.lastIndex,
                surveyTopics = demo.topics,
                level = 4,
                answerIds = answers.map { it.id },
                remaining = demo.remainingMinutes.minutes,
                status = ExamStatus.Finished,
                grade = SessionGrade(emptyList(), demo.low, demo.high, demo.reasons, demo.brokeAt),
            ),
        )
    }
    pool.filter { it.id in PRACTICE_QUESTIONS }.forEachIndexed { index, question ->
        save(Answer("demo-practice-${index + 1}", question, null, emptyList(), analysis = AnswerAnalysis.Done(FLASH, plain.copy(score = 74 + index * 6))))
    }
}

private class DemoSession(
    val topics: List<String>,
    val low: String,
    val high: String,
    val baseScore: Int,
    val remainingMinutes: Int,
    val reasons: List<String>,
    val brokeAt: List<Int> = emptyList(),
)

private val DEMO_SESSIONS = listOf(
    DemoSession(
        topics = listOf("movies", "music", "walking"),
        low = "IM1",
        high = "IM2",
        baseScore = 52,
        remainingMinutes = 3,
        reasons = listOf("묘사 문항은 문장 서너 개로 이어 말했지만 과거 경험에서 현재 시제로 자주 돌아갔습니다.", "롤플레이에서 질문을 두 개만 했습니다."),
        brokeAt = listOf(12),
    ),
    DemoSession(
        topics = listOf("cafes", "cooking", "jogging"),
        low = "IM2",
        high = "IM3",
        baseScore = 61,
        remainingMinutes = 6,
        reasons = listOf("경험 문항에서 언제, 누구와, 무엇을 했는지 순서대로 말했습니다.", "비교 문항에서 이유를 하나만 들고 끝냈습니다."),
        brokeAt = listOf(14),
    ),
    DemoSession(
        topics = listOf("parks", "music", "domestic-travel"),
        low = "IM3",
        high = "IH",
        baseScore = 70,
        remainingMinutes = 8,
        reasons = listOf(
            "묘사, 습관, 경험 세트 셋에서 문단 길이로 답했고 과거 시제를 끝까지 유지했습니다.",
            "롤플레이에서 상황에 맞는 질문을 네 개 만들었습니다.",
            "사회 이슈 문항에서 예시 없이 의견만 말해 IH 위로는 근거가 부족합니다.",
        ),
    ),
)

private val FLASH = AnalyzedBy(GeminiModels.PROVIDER, GeminiModels.defaultModel.id, "v3")

/** 세트로 나오는 유형. 회차의 서베이 주제 순서대로 묘사, 습관, 경험을 같은 주제로 묶는다. */
private val SET_TYPES = setOf("description", "routine", "past-experience")

private val SCORE_SPREAD = listOf(6, 3, 4, 8, -2, 5, 1, 7, 0, -4, 2, -6, 4, -3, -8)

private const val SHOWCASE_QUESTION = "parks-description-1"
private val PRACTICE_QUESTIONS = setOf("cafes-routine-1", "beach-past-experience-1")

private val plain = Feedback(
    transcript = "I usually go there on weekends with my friends. We talk a lot and stay for a few hours.",
    corrections = emptyList(),
    upgrades = emptyList(),
    tenseConsistent = true,
    sentenceCount = 6,
    wordsPerMinute = 112.0,
    pauses = emptyList(),
    roleplayQuestionCount = null,
    pronunciation = Pronunciation.Evaluated("Clear overall."),
    score = 70,
)

/** 답변 리포트 스크린샷에 나오는 답. 문법 교정, 한 단계 위 표현, 내용 교정, 최종 스크립트가 다 있다. */
private val showcase = Feedback(
    transcript = "There is a big park near my house. It have a lake in the middle and a long walking trail around it. " +
        "In the morning many people jogging there, and in the evening families come with their kids. " +
        "I like the bench next to the lake. It is very peaceful, so I go there when I feel stressed.",
    corrections = listOf(
        Correction(original = "It have a lake", corrected = "It has a lake", deducted = 3),
        Correction(original = "many people jogging there", corrected = "many people jog there", deducted = 3),
    ),
    upgrades = listOf(
        Upgrade(original = "It is very peaceful", better = "It's so peaceful that I lose track of time"),
        Upgrade(original = "when I feel stressed", better = "whenever I need to clear my head"),
    ),
    tenseConsistent = true,
    sentenceCount = 5,
    wordsPerMinute = 118.0,
    pauses = listOf(Pause(start = 9.seconds, length = 2.seconds)),
    roleplayQuestionCount = null,
    pronunciation = Pronunciation.Evaluated("Clear overall. Stress on \"peaceful\" fell on the second syllable once."),
    score = 78,
    contentFixes = listOf(
        ContentFix(
            original = "I like the bench next to the lake.",
            problem = "어떤 의자인지 말하지 않아 왜 좋아하는지가 그려지지 않는다.",
            corrected = "My favorite spot is an old wooden bench right next to the lake, where you can watch the ducks.",
        ),
    ),
    finalScript = "There's a big park about ten minutes from my house. It has a lake in the middle and a long walking trail around it. " +
        "In the morning, many people jog there, and in the evening, families come out with their kids. " +
        "My favorite spot is an old wooden bench right next to the lake, where you can watch the ducks. " +
        "It's so peaceful that I lose track of time, so I go there whenever I need to clear my head.",
)
