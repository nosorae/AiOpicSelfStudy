package io.github.nosorae.aiopic.ui.featuresettings.practice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.question.pick.parseQuestions

/** 문항 파일을 불러온 결과. 화면이 경우마다 문구를 고른다. */
sealed interface QuestionImportResult {
    data class Imported(val count: Int) : QuestionImportResult

    /** 고른 파일을 읽지 못했다. */
    data object Unreadable : QuestionImportResult

    /** 문항 파일 모양이 아니다. [reason] 은 검사가 돌려준 이유 그대로다. */
    data class Invalid(val reason: String) : QuestionImportResult
}

/**
 * 고른 파일을 내장 문항 풀과 같은 규칙(`parseQuestions`)으로 검사하고, 맞으면 저장한다(F6.3). 틀리면 저장된 파일을 건드리지 않는다.
 * [json] 이 null 이면 파일을 읽지 못한 것이다.
 */
// ponytail: 파일은 하나만 둔다. 새로 불러오면 앞 파일을 바꾼다. 여러 파일을 쌓아 두고 고르게 하려면 목록 저장소로 바꾼다.
@Inject
class ImportQuestionsUseCase(private val questionFileStore: QuestionFileStore) {
    suspend operator fun invoke(json: String?): QuestionImportResult {
        if (json == null) return QuestionImportResult.Unreadable
        val questions = try {
            parseQuestions(json)
        } catch (e: IllegalArgumentException) {
            return QuestionImportResult.Invalid(e.message.orEmpty())
        }
        questionFileStore.saveQuestionFile(json)
        return QuestionImportResult.Imported(questions.size)
    }
}
