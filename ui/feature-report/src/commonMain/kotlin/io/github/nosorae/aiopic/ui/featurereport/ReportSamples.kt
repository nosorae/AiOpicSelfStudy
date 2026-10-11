package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.answer.model.Upgrade
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import kotlin.time.Duration.Companion.seconds

/** 미리보기와 스크린샷 테스트가 같이 쓰는 고정 상태. */
internal object ReportSamples {
    private val flash = AnalyzedBy("gemini", "gemini-3.8-flash", "v1")
    private val lite = AnalyzedBy("gemini", "gemini-3.5-flash-lite", "v1")
    private val types = listOf("self-intro", "description", "routine", "past-experience", "roleplay-ask")

    /** 앱 층이 시험 팩과 모델 목록에서 넣는 이름. 받아쓰기 경로 모델은 목록에 없어 id 로 보인다. */
    private val names = ReportNames(
        types = mapOf("self-intro" to "자기소개", "description" to "묘사", "routine" to "습관", "past-experience" to "과거 경험", "roleplay-ask" to "롤플레이 질문하기")
            .mapKeys { QuestionType(it.key) },
        models = listOf(
            ModelInfo(flash.provider, flash.modelId, "Gemini 3.8 Flash", hearsAudio = true, stability = Stability.Stable),
            ModelInfo(lite.provider, lite.modelId, "Gemini 3.5 Flash Lite", hearsAudio = true, stability = Stability.Stable),
        ),
        grades = listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"),
    )

    private val feedback = Feedback(
        transcript = "Last weekend I go to the park near my house. I walked my dog and we play with a ball for about an hour.",
        corrections = listOf(
            Correction(original = "I go to the park", corrected = "I went to the park"),
            Correction(original = "we play with a ball", corrected = "we played with a ball"),
        ),
        upgrades = listOf(Upgrade(original = "about an hour", better = "for the better part of an hour")),
        tenseConsistent = false,
        sentenceCount = 2,
        wordsPerMinute = 92.4,
        pauses = listOf(Pause(start = 4.seconds, length = 2.seconds), Pause(start = 11.seconds, length = 3.seconds)),
        roleplayQuestionCount = null,
        pronunciation = Pronunciation.Evaluated("Clear overall. The ending of \"walked\" was dropped twice."),
        score = 64,
    )

    private fun answer(index: Int, analysis: AnswerAnalysis) = Answer(
        id = "a$index",
        question = Question("q$index", "Tell me about a park you often visit. What did you do there last time?", QuestionType(types[index % types.size]), "park"),
        examSessionId = "s1",
        chunkPaths = emptyList(),
        analysis = analysis,
    )

    private val grade = SessionGrade(
        checks = emptyList(),
        low = "IM2",
        high = "IH",
        reasons = listOf(
            "묘사와 경험 문항에서 문장을 이어 말했지만 과거 시제가 자주 흔들렸습니다.",
            "롤플레이에서 질문을 세 개 이상 만들었습니다.",
        ),
        brokeAt = listOf(3),
    )

    private fun report(
        answers: List<Answer>,
        status: ExamStatus = ExamStatus.Finished,
        grade: SessionGrade? = this.grade,
        gradeError: AnalysisError? = null,
    ) = SessionReportUiState(
        loading = false,
        report = SessionReport(
            session = ExamSession(id = "s1", level = 4, answerIds = answers.map { it.id }, status = status, grade = grade, gradeError = gradeError),
            answers = answers,
            questionCount = 7,
            names = names,
        ),
    )

    /** 등급 범위, 근거, 무너진 문항, 한 회차 안에서 모델이 섞인 표시. */
    val mixedModels = report(
        List(7) { answer(it, AnswerAnalysis.Done(if (it == 6) lite else flash, feedback.copy(score = 60 + it))) },
    )

    /** 시간이 다 돼 5번까지 답했고, 하나는 평가할 수 없고, 하나는 아직 분석 대기다. */
    val timeUp = report(
        listOf(
            answer(0, AnswerAnalysis.Done(flash, feedback)),
            answer(1, AnswerAnalysis.Done(flash, feedback.copy(score = 71))),
            answer(2, AnswerAnalysis.Unscorable(flash, UnscorableReason.TooShort)),
            answer(3, AnswerAnalysis.Done(flash, feedback.copy(score = 58))),
            answer(4, AnswerAnalysis.Pending),
        ),
        status = ExamStatus.TimeUp,
        grade = null,
    )

    /** 답변 7개를 모두 분석했고 회차 등급만 기다린다(등급 요청이 대기열에 있거나 모델이 내는 중). */
    val grading = report(List(7) { answer(it, AnswerAnalysis.Done(flash, feedback)) }, grade = null)

    /** 첫 교정에 "틀림"을 누른 상태(F8.1). */
    val answerEvaluated = AnswerReportUiState(loading = false, answer = answer(1, AnswerAnalysis.Done(flash, feedback)), wrongCorrections = setOf(0), names = names)

    /** 받아쓴 글만 분석한 경로(F5.9). 롤플레이라 한 질문 수도 보인다. */
    val pronunciationNotEvaluated = AnswerReportUiState(
        loading = false,
        answer = answer(
            4,
            AnswerAnalysis.Done(
                AnalyzedBy("text-only", "transcript-model", "v1"),
                feedback.copy(tenseConsistent = true, roleplayQuestionCount = 3, pronunciation = Pronunciation.NotEvaluated),
            ),
        ),
        names = names,
    )

    val answerUnscorable = AnswerReportUiState(loading = false, answer = answer(2, AnswerAnalysis.Unscorable(flash, UnscorableReason.WrongLanguage)), names = names)

    val answerPending = AnswerReportUiState(loading = false, answer = answer(3, AnswerAnalysis.Pending), names = names)

    /** 연결이 끊겨 결과를 받지 못했다(#203). */
    val answerFailed = AnswerReportUiState(loading = false, answer = answer(3, AnswerAnalysis.Failed(AnalysisError.Retryable)), names = names)

    /** 답변 7개 중 하나를 분석하지 못해 등급을 요청하지 않았다(#203). */
    val analysisFailed = report(
        List(7) { answer(it, if (it == 6) AnswerAnalysis.Failed(AnalysisError.Rejected) else AnswerAnalysis.Done(flash, feedback)) },
        grade = null,
    )

    /** 답변은 다 분석했지만 키가 틀려 등급을 받지 못했다(#203). */
    val gradeFailed = report(List(7) { answer(it, AnswerAnalysis.Done(flash, feedback)) }, grade = null, gradeError = AnalysisError.InvalidKey)
}
