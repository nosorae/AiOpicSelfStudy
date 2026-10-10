package io.github.nosorae.aiopic.apps.opic.shared

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import io.github.nosorae.aiopic.apps.opic.shared.data.StoreExamRepository
import io.github.nosorae.aiopic.apps.opic.shared.data.StorePracticeRepository
import io.github.nosorae.aiopic.apps.opic.shared.data.StoreReads
import io.github.nosorae.aiopic.engine.aigemini.GeminiAnalyzer
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisRequest
import io.github.nosorae.aiopic.engine.answer.analysis.AnswerAnalysisHandler
import io.github.nosorae.aiopic.engine.answer.analysis.GradeRequest
import io.github.nosorae.aiopic.engine.answer.analysis.SessionGradeHandler
import io.github.nosorae.aiopic.engine.answer.check.choose
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.TextAnalysis
import io.github.nosorae.aiopic.engine.answer.port.Transcriber
import io.github.nosorae.aiopic.engine.answer.port.Transcript
import io.github.nosorae.aiopic.engine.answer.port.TranscriptAnalyzer
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.pick.parseQuestions
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmRequest
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import io.github.nosorae.aiopic.libs.recorder.api.Recorder
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import io.github.nosorae.aiopic.libs.timer.deviceMonotonicTimer
import io.github.nosorae.aiopic.libs.tts.api.Tts
import io.github.nosorae.aiopic.libs.workqueue.api.WorkHandler
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import io.github.nosorae.aiopic.packs.opicen.OpicPrompt
import io.github.nosorae.aiopic.packs.opicen.OpicQuestionType
import io.github.nosorae.aiopic.ui.featureexam.ExamContent
import io.github.nosorae.aiopic.ui.featureexam.ExamRepository
import io.github.nosorae.aiopic.ui.featureexam.ExamViewModel
import io.github.nosorae.aiopic.ui.featurehistory.HistoryStore
import io.github.nosorae.aiopic.ui.featurehistory.HistoryViewModel
import io.github.nosorae.aiopic.ui.featurehome.ExamSessionSource
import io.github.nosorae.aiopic.ui.featurehome.HomeViewModel
import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import io.github.nosorae.aiopic.ui.featureonboarding.consent.ConsentViewModel
import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyChecker
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryViewModel
import io.github.nosorae.aiopic.ui.featureonboarding.start.DecideStartUseCase
import io.github.nosorae.aiopic.ui.featureonboarding.start.StartViewModel
import io.github.nosorae.aiopic.ui.featurepractice.PracticeRepository
import io.github.nosorae.aiopic.ui.featurepractice.PracticeViewModel
import io.github.nosorae.aiopic.ui.featurereport.AnswerReportViewModel
import io.github.nosorae.aiopic.ui.featurereport.ReportStore
import io.github.nosorae.aiopic.ui.featurereport.SessionReportViewModel
import io.github.nosorae.aiopic.ui.featurereport.flag.FlagStore
import io.github.nosorae.aiopic.ui.featuresettings.SettingsViewModel
import io.github.nosorae.aiopic.ui.featuresettings.export.FlagExportStore
import io.github.nosorae.aiopic.ui.featuresettings.export.FlagExportViewModel
import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.practice.AnswerTimerStore
import io.github.nosorae.aiopic.ui.featuresettings.practice.QuestionFileStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.MissingResourceException
import kotlin.time.DurationUnit

/**
 * 앱 하나의 DI 그래프. 화면은 여기서 ViewModel 이나 assisted 팩토리를 꺼낸다(ADR 0004, 0007).
 * 기기에 묶인 것(키 보관소, DB, 녹음기, 음성, 대기열)은 플랫폼 진입점이 만들어 [Factory] 인자로 넘긴다.
 * 그래서 공통 테스트가 메모리 가짜로 같은 그래프를 만든다.
 */
@DependencyGraph(AppScope::class)
interface AppGraph {
    val startViewModel: StartViewModel

    /** 첫 실행 화면(S1, S2)을 마친 뒤 다음 화면을 다시 정한다(F1.0). */
    val decideStartUseCase: DecideStartUseCase

    /** 홈의 모의시험 버튼이 이어 볼 회차를 찾는다(F3). */
    val examSessionSource: ExamSessionSource

    val keyEntryViewModelFactory: KeyEntryViewModel.Factory

    val consentViewModel: ConsentViewModel

    val homeViewModel: HomeViewModel

    val examViewModelFactory: ExamViewModel.Factory

    val practiceViewModelFactory: PracticeViewModel.Factory

    val sessionReportViewModelFactory: SessionReportViewModel.Factory

    val answerReportViewModelFactory: AnswerReportViewModel.Factory

    val historyViewModel: HistoryViewModel

    val settingsViewModel: SettingsViewModel

    val flagExportViewModel: FlagExportViewModel

    /** 앱 틀이 구독해서 화면 모드를 고르는 즉시 테마를 바꾼다(F9.1). */
    val themeModeStore: ThemeModeStore

    /** 플랫폼 대기열이 답변 id 나 회차 등급 요청을 꺼낼 때마다 부른다(F5.1, F5.3). 대기열은 그래프보다 먼저 만들어져서 이 값을 나중에 이어 준다. */
    val analysisHandler: WorkHandler

    @Provides fun voiceConsent(settings: AppSettings): VoiceConsent = settings

    @Provides fun provideThemeModeStore(settings: AppSettings): ThemeModeStore = settings

    @Provides fun provideModelSelectionStore(settings: AppSettings): ModelSelectionStore = settings

    @Provides fun answerTimerStore(settings: AppSettings): AnswerTimerStore = settings

    @Provides fun questionFileStore(settings: AppSettings): QuestionFileStore = settings

    // ponytail: 첫 버전은 제공사가 Gemini 하나라 목록도 하나다.
    @Provides fun modelCatalog(): ModelCatalog = GeminiModels

    @Provides
    fun keyChecker(client: GeminiClient): KeyChecker = KeyChecker { _, key ->
        // ponytail: 키 확인 전용 API 가 없어 기본 모델에 짧은 요청을 보낸다. 무료 키의 하루 한도에서 한 번을 쓴다.
        geminiKeyCheck(client.generate(LlmRequest(apiKey = key, model = GeminiModels.defaultModel.id, prompt = "Reply with OK.")))
    }

    @SingleIn(AppScope::class)
    @Provides
    fun opicStore(database: OpicDatabase): OpicStore = OpicStore(database)

    @SingleIn(AppScope::class)
    @Provides
    fun storeReads(store: OpicStore): StoreReads = StoreReads(store)

    @Provides fun examSessionSource(reads: StoreReads): ExamSessionSource = reads

    @Provides fun reportStore(reads: StoreReads): ReportStore = reads

    @Provides fun historyStore(reads: StoreReads): HistoryStore = reads

    @Provides fun flagStore(reads: StoreReads): FlagStore = reads

    @Provides fun flagExportStore(reads: StoreReads): FlagExportStore = reads

    @Provides fun examSpec(): ExamSpec = OpicEn.examSpec

    /** 기록과 리포트에 보일 문항 유형 이름. 시험 팩의 유형 id 를 한국어로 옮긴다. */
    @Provides
    fun questionTypeNames(): Map<QuestionType, String> = mapOf(
        OpicQuestionType.SelfIntro to "자기소개",
        OpicQuestionType.Description to "묘사",
        OpicQuestionType.Routine to "습관",
        OpicQuestionType.PastExperience to "과거 경험",
        OpicQuestionType.RoleplayAsk to "롤플레이 질문",
        OpicQuestionType.RoleplaySolve to "롤플레이 해결",
        OpicQuestionType.RoleplayExperience to "롤플레이 경험",
        OpicQuestionType.Comparison to "비교",
        OpicQuestionType.SocialIssue to "사회 이슈",
    ).mapKeys { it.key.type }

    // ponytail: 시험 팩에 서베이 Part 1~3 질문이 아직 없어 주제 고르기만 보인다. 팩이 Part 질문을 내면 여기서 넘긴다

    /** 시험 화면을 열 때마다 만들어서 설정의 답변 타이머(F2.7)를 그때 값으로 읽는다. */
    @Provides
    fun examContent(spec: ExamSpec, settings: AppSettings): ExamContent = ExamContent(spec, OpicEn.surveyTopics, surveyParts = emptyList(), answerTimer = settings.answerTimer)

    @Provides
    fun examRepository(store: OpicStore, queue: WorkQueue, spec: ExamSpec, @Named(RECORDINGS) recordingsRoot: String): ExamRepository = StoreExamRepository(store, queue, spec, OpicEn::questionPool, recordingsRoot)

    @Provides
    fun practiceRepository(store: OpicStore, queue: WorkQueue, settings: AppSettings, @Named(RECORDINGS) recordingsRoot: String): PracticeRepository = StorePracticeRepository(store, queue, { practicePool(settings) }, recordingsRoot)

    @Provides
    fun analysisHandler(store: OpicStore, queue: WorkQueue, client: GeminiClient, keyVault: KeyVault, settings: AppSettings, recorder: Recorder): WorkHandler = analysisQueueHandler(
        store = store,
        queue = queue,
        client = client,
        keyVault = keyVault,
        settings = settings,
        answerRequest = { answer, model -> analysisRequest(answer, model, recorder) },
        gradeRequest = { answers, model -> gradeRequest(answers, model, recorder) },
    )

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides settings: AppSettings,
            @Provides keyVault: KeyVault,
            @Provides database: OpicDatabase,
            @Provides @Named(RECORDINGS) recordingsRoot: String,
            @Provides recorder: Recorder,
            @Provides tts: Tts,
            @Provides workQueue: WorkQueue,
            @Provides gemini: GeminiClient,
            @Provides timer: MonotonicTimer,
        ): AppGraph
    }
}

private const val RECORDINGS = "recordingsRoot"

/**
 * 한 문항 연습의 풀. 시험 팩의 내장 문항에 사용자가 설정에서 불러온 문항(F6.3)을 더한다. 모의시험 출제에는 넣지 않는다.
 * 시험 팩의 문항 파일을 못 읽으면 불러온 문항만 쓰고, 둘 다 없으면 연습 화면이 죽지 않고 "낼 질문이 없습니다" 오류 화면을 보인다.
 */
internal suspend fun practicePool(settings: AppSettings, builtIn: suspend () -> List<Question> = OpicEn::questionPool): List<Question> {
    val pack = try {
        builtIn()
    } catch (_: MissingResourceException) {
        emptyList()
    }
    // 저장할 때 검사를 마친 파일이다.
    return pack + settings.questionFile()?.let(::parseQuestions).orEmpty()
}

/**
 * 플랫폼 진입점이 Metro 컴파일러 플러그인 없이 그래프를 만들 수 있게 여기서 만든다. 인자는 [AppGraph.Factory] 와 같다.
 * [gemini] 와 [timer] 는 앱이 기본값을 쓰고, 끝까지 도는 시나리오 테스트가 저장해 둔 Gemini 응답과 손으로 넘기는 시계로 바꾼다.
 */
fun createAppGraph(
    settings: AppSettings,
    keyVault: KeyVault,
    database: OpicDatabase,
    recordingsRoot: String,
    recorder: Recorder,
    tts: Tts,
    workQueue: WorkQueue,
    gemini: GeminiClient = GeminiClient(),
    timer: MonotonicTimer = deviceMonotonicTimer(),
): AppGraph = createGraphFactory<AppGraph.Factory>().create(settings, keyVault, database, recordingsRoot, recorder, tts, workQueue, gemini, timer)

/** 키 확인 요청 결과를 키 입력 화면의 결과로 옮긴다(F1.1). 모델이 없다는 답도 키는 통과했다는 뜻이다. */
internal fun geminiKeyCheck(result: LlmResult): DomainResult<Unit> = when (result) {
    is LlmResult.Success, LlmResult.ModelNotFound -> DomainResult.Success(Unit)
    LlmResult.InvalidKey -> DomainResult.Failure(DomainError.InvalidKey)
    is LlmResult.Retryable, is LlmResult.Rejected -> DomainResult.Failure(DomainError.Unavailable)
}

/**
 * 분석 대기열의 처리기. 테스트가 가짜 Gemini 클라이언트를 넣을 수 있게 그래프 밖에 둔다.
 * [request] 를 따로 받는 것은 호스트 JVM 테스트에서 시험 팩의 프롬프트 파일(Compose 리소스)을 못 읽어서다.
 */
internal fun answerAnalysisHandler(
    store: OpicStore,
    client: GeminiClient,
    keyVault: KeyVault,
    settings: AppSettings,
    request: suspend (Answer, ModelInfo) -> AnalysisRequest,
): WorkHandler {
    val gemini = FallBackToDefaultModel(GeminiAnalyzer(client) { keyVault.get(GeminiModels.PROVIDER) }, settings)
    return AnswerAnalysisHandler(
        store = store,
        router = AnswerRouter(gemini, NoTwoStepPath, NoTwoStepPath),
        consented = settings::isGiven,
        request = { answer -> request(answer, GeminiModels.choose(settings.selectedModelId()).model) },
    )
}

/**
 * 분석 대기열 하나의 처리기. 답변 분석을 처리하고, 회차 답변이 모두 분석되면 등급 요청을 같은 대기열에 넣어 처리한다(F5.3).
 * 등급도 설정에서 고른 모델로 보낸다. 요청을 따로 받는 이유는 [answerAnalysisHandler] 와 같다.
 */
// ponytail: 등급 요청은 고른 모델이 내려가도 기본 모델로 돌리지 않고 버린다. 다음 답변 분석이 기본 모델로 바꿔 두므로, 등급을 다시 요청하는 길이 생기면 그때 돌린다.
internal fun analysisQueueHandler(
    store: OpicStore,
    queue: WorkQueue,
    client: GeminiClient,
    keyVault: KeyVault,
    settings: AppSettings,
    answerRequest: suspend (Answer, ModelInfo) -> AnalysisRequest,
    gradeRequest: suspend (List<Answer>, ModelInfo) -> GradeRequest,
): WorkHandler = SessionGradeHandler(
    store = store,
    queue = queue,
    answers = answerAnalysisHandler(store, client, keyVault, settings, answerRequest),
    grader = GeminiAnalyzer(client) { keyVault.get(GeminiModels.PROVIDER) },
    consented = settings::isGiven,
    request = { answers -> gradeRequest(answers, GeminiModels.choose(settings.selectedModelId()).model) },
)

/**
 * 고른 모델이 내려가 분석이 [AnalysisError.ModelUnavailable] 로 끝나면 기본 모델을 저장하고 다시 보내게 한다(F5.8).
 * 다시 보낼 때 [answerAnalysisHandler] 가 저장된 모델을 읽어서 기본 모델로 간다. 설정 화면은 [AppSettings.takeFellBackFrom] 으로 알린다.
 * 기본 모델마저 없으면 돌릴 곳이 없어 그대로 실패로 둔다.
 */
private class FallBackToDefaultModel(private val analyzer: AudioAnswerAnalyzer, private val settings: AppSettings) : AudioAnswerAnalyzer {
    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> {
        val result = analyzer.analyze(model, prompt, question, recording)
        val default = GeminiModels.choose(model.id, unavailable = true).model
        if (result != AnalysisResult.Failed(AnalysisError.ModelUnavailable) || model == default) return result
        settings.fallBack(from = model.id, to = default.id)
        return AnalysisResult.Failed(AnalysisError.Retryable)
    }
}

/**
 * 답변 하나의 분석 요청을 만든다. 조각을 같은 폴더의 `answer.m4a` 하나로 이어 붙이고(조각 이름 규칙과 겹치지 않는다)
 * 시험 팩의 답변 프롬프트에 문항과 이어 녹음 위치를 채운다(F3.4, F5.7).
 */
private suspend fun analysisRequest(answer: Answer, model: ModelInfo, recorder: Recorder): AnalysisRequest {
    val recording = recording(answer, recorder)
    val prompt = OpicPrompt.Answer.load()
    val text = prompt.fill(
        mapOf(
            "question" to answer.question.text,
            "questionType" to answer.question.type.id,
            "resumedAt" to recording.resumedAt.joinToString { it.toDouble(DurationUnit.SECONDS).toString() }.ifEmpty { "none" },
        ),
    )
    return AnalysisRequest(model, Prompt(prompt.version, text, OpicPrompt.Answer.responseSchema.toString()), recording)
}

/**
 * 회차 등급 요청을 만든다(F5.3). 시험 팩의 등급 프롬프트에 답한 순서의 답변 분석 결과(평가할 수 없음 포함)를 JSON 으로 채우고
 * 같은 순서의 녹음을 붙인다. 녹음은 답변 분석 때와 같은 방법으로 이어 붙인다.
 */
private suspend fun gradeRequest(answers: List<Answer>, model: ModelInfo, recorder: Recorder): GradeRequest {
    val prompt = OpicPrompt.SessionGrade.load()
    val text = prompt.fill(mapOf("answers" to Json.encodeToString(answers.map { it.analysis })))
    return GradeRequest(model, Prompt(prompt.version, text, OpicPrompt.SessionGrade.responseSchema.toString()), OpicEn.gradeScale, answers.map { recording(it, recorder) })
}

/** 답변의 조각을 같은 폴더의 `answer.m4a` 하나로 이어 붙여 읽는다. */
private suspend fun recording(answer: Answer, recorder: Recorder): Recording {
    // 조각이 없는 답변(마이크를 못 연 경우)은 빈 녹음으로 보낸다. 여기서 예외가 나면 대기열 사슬 전체가 멈춘다.
    val output = answer.chunkPaths.firstOrNull()?.substringBeforeLast('/')?.plus("/answer.m4a")
    val resumedAt = output?.let { recorder.merge(answer.chunkPaths, it) }.orEmpty()
    return Recording(output?.let(::readFile) ?: ByteArray(0), "audio/mp4", resumedAt)
}

/** 녹음 파일 하나를 통째로 읽는다. 분석 요청 한 번에 답변 하나(길어야 몇 MB)만 싣는다. */
internal expect fun readFile(path: String): ByteArray

// ponytail: Gemini 목록은 모두 음성을 직접 들어서 두 단계 경로(받아쓰기, 글 분석)를 쓰지 않는다. 음성을 못 듣는 모델을 더할 때 실제 구현을 넣는다.
private object NoTwoStepPath : Transcriber, TranscriptAnalyzer {
    override suspend fun transcribe(recording: Recording): AnalysisResult<Transcript> = AnalysisResult.Failed(AnalysisError.Rejected)

    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, transcript: Transcript): AnalysisResult<TextAnalysis> = AnalysisResult.Failed(AnalysisError.Rejected)
}
