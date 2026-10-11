package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.ContentFix
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
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
import kotlin.time.Instant

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
        jobs: List<AnalysisJob> = emptyList(),
    ) = SessionReportUiState(
        loading = false,
        report = SessionReport(
            session = ExamSession(id = "s1", level = 4, answerIds = answers.map { it.id }, status = status, grade = grade, gradeError = gradeError),
            answers = answers,
            questionCount = 7,
            names = names,
            jobs = jobs,
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

    /** 새로 시작해 끝낸 미완료 회차(#223). 3번까지 답해 분석이 끝났고 등급을 내는 중이다. */
    val abandoned = report(List(3) { answer(it, AnswerAnalysis.Done(flash, feedback)) }, status = ExamStatus.Abandoned, grade = null)

    /** 서베이만 내고 답한 문항 없이 끝낸 미완료 회차(#223). 분석할 답이 없어 등급도 없다. */
    val abandonedEmpty = report(emptyList(), status = ExamStatus.Abandoned, grade = null)

    /** 답변 7개를 모두 분석했고 회차 등급만 기다린다(등급 요청이 대기열에 있거나 모델이 내는 중). */
    val grading = report(List(7) { answer(it, AnswerAnalysis.Done(flash, feedback)) }, grade = null)

    /** 첫 교정에 "틀림"을 누른 상태(F8.1). */
    val answerEvaluated = AnswerReportUiState(loading = false, answer = answer(1, AnswerAnalysis.Done(flash, feedback)), wrongCorrections = setOf(0), names = names)

    /** 프롬프트 v3 결과(#217). 내용 교정 하나와 최종 스크립트가 있고, 스크립트를 복사한 뒤다. */
    val answerScripted = answerEvaluated.copy(
        wrongCorrections = emptySet(),
        answer = answer(
            1,
            AnswerAnalysis.Done(
                flash,
                feedback.copy(
                    contentFixes = listOf(
                        ContentFix(
                            original = "I walked my dog and we play with a ball for about an hour.",
                            problem = "공원에서 무엇을 했는지만 말하고 그때 어땠는지가 없어 이야기가 끝나지 않는다.",
                            corrected = "I walked my dog, and we played with a ball for about an hour. It was the most relaxing hour of my week.",
                        ),
                    ),
                    finalScript = "Last weekend, I went to the park near my house. It's a small park with a walking trail and a big open lawn. " +
                        "I walked my dog there, and we played with a ball for about an hour. She kept bringing the ball back, so I couldn't stop laughing. " +
                        "Honestly, it was the most relaxing hour of my week, and I'm planning to go again this Saturday.",
                ),
            ),
        ),
        scriptCopied = true,
    )

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

    private fun job(state: JobState, id: String, kind: JobKind = JobKind.Answer) = AnalysisJob(kind, id, state, updatedAt = Instant.fromEpochMilliseconds(0))

    /** 연결이 없어 분석 작업이 연결을 기다린다(#205). */
    val answerWaitingNetwork = answerPending.copy(job = job(JobState.WaitingNetwork, "a3"))

    /** 답변 다섯 개 중 하나는 보내는 중, 하나는 연결을 기다린다(#205). */
    val sessionWaiting = report(
        List(5) { answer(it, if (it < 3) AnswerAnalysis.Done(flash, feedback) else AnswerAnalysis.Pending) },
        grade = null,
        jobs = listOf(job(JobState.Running, "a3"), job(JobState.WaitingNetwork, "a4")),
    )

    /** 답변은 다 분석했고 등급 작업이 연결을 기다린다(#205). */
    val gradeWaitingNetwork = report(List(7) { answer(it, AnswerAnalysis.Done(flash, feedback)) }, grade = null, jobs = listOf(job(JobState.WaitingNetwork, "s1", JobKind.Grade)))
}
