package io.github.nosorae.aiopic.ui.featurereport

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus

fun session(level: Int, answers: Int, status: ExamStatus = ExamStatus.Finished) = ExamSession(id = "s1", level = level, answerIds = List(answers) { "a$it" }, status = status)

fun done(model: String = "model-a", score: Int = 70) = AnswerAnalysis.Done(
    analyzedBy = AnalyzedBy("gemini", model, "v1"),
    feedback = Feedback(
        transcript = "I go to the park.",
        corrections = emptyList(),
        upgrades = emptyList(),
        tenseConsistent = true,
        sentenceCount = 1,
        wordsPerMinute = 100.0,
        pauses = emptyList(),
        roleplayQuestionCount = null,
        pronunciation = Pronunciation.NotEvaluated,
        score = score,
    ),
)

fun answers(examSessionId: String?, count: Int) = List(count) {
    Answer(
        id = "a$it",
        question = Question("q$it", "Tell me about your park.", QuestionType("t"), "park"),
        examSessionId = examSessionId,
        chunkPaths = emptyList(),
        analysis = done(),
    )
}
