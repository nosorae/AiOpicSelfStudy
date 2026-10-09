package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent
import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.practice.AnswerTimerStore
import io.github.nosorae.aiopic.ui.featuresettings.practice.QuestionFileStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeMode
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 설정 값(화면 모드, 고른 분석 모델, 음성 전송 동의, 답변 타이머, 불러온 문항 파일)을 플랫폼 설정 저장소에 둔다.
 * 플랫폼은 문자열 읽기와 쓰기 두 함수만 넘긴다(Android SharedPreferences, iOS NSUserDefaults). 키가 아니라서 암호화하지 않는다.
 */
class AppSettings(private val read: (String) -> String?, private val write: (String, String) -> Unit) :
    ThemeModeStore,
    ModelSelectionStore,
    VoiceConsent,
    AnswerTimerStore,
    QuestionFileStore {
    private val mode = MutableStateFlow(ThemeMode.entries.firstOrNull { it.name == read(THEME_MODE) } ?: ThemeMode.System)
    override val themeMode: StateFlow<ThemeMode> = mode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        write(THEME_MODE, mode.name)
        this.mode.value = mode
    }

    override suspend fun selectedModelId(): String? = read(MODEL_ID)

    override suspend fun selectModel(id: String) = write(MODEL_ID, id)

    /** 고른 모델이 내려가 분석이 실패했을 때 부른다(F5.8). 기본 모델로 바꾸고, 설정 화면이 알릴 수 있게 원래 모델 id 를 남긴다. */
    suspend fun fallBack(from: String, to: String) {
        write(FELL_BACK_FROM, from)
        selectModel(to)
    }

    override suspend fun takeFellBackFrom(): String? = read(FELL_BACK_FROM)?.ifEmpty { null }?.also { write(FELL_BACK_FROM, "") }

    override suspend fun isGiven(): Boolean = read(VOICE_CONSENT) == "true"

    override suspend fun give() = write(VOICE_CONSENT, "true")

    override suspend fun revoke() = write(VOICE_CONSENT, "false")

    /** 시험 화면을 조립할 때 바로 읽는다(F2.7). 기본은 꺼짐이다. */
    val answerTimer: Boolean get() = read(ANSWER_TIMER) == "true"

    override suspend fun answerTimerOn(): Boolean = answerTimer

    override suspend fun setAnswerTimerOn(on: Boolean) = write(ANSWER_TIMER, on.toString())

    // ponytail: 문항 파일을 설정 저장소에 문자열 하나로 둔다. 몇십 KB 까지는 괜찮고, 파일이 MB 단위로 커지면 앱 폴더 파일로 옮긴다.
    override suspend fun questionFile(): String? = read(QUESTION_FILE)

    override suspend fun saveQuestionFile(json: String) = write(QUESTION_FILE, json)

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val MODEL_ID = "model_id"
        private const val FELL_BACK_FROM = "model_fell_back_from"
        private const val VOICE_CONSENT = "voice_consent"
        private const val ANSWER_TIMER = "answer_timer"
        private const val QUESTION_FILE = "question_file"

        /** 앱 틀 테스트처럼 기기에 남길 필요가 없을 때 쓴다. */
        fun inMemory(): AppSettings {
            val values = mutableMapOf<String, String>()
            return AppSettings(read = { values[it] }, write = { key, value -> values[key] = value })
        }
    }
}
