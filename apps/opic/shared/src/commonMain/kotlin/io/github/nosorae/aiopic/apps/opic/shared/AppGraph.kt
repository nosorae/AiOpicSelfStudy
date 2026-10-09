package io.github.nosorae.aiopic.apps.opic.shared

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featurehome.ExamSessionSource
import io.github.nosorae.aiopic.ui.featurehome.HomeViewModel
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import io.github.nosorae.aiopic.ui.featureonboarding.consent.ConsentViewModel
import io.github.nosorae.aiopic.ui.featureonboarding.consent.VoiceConsent
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyChecker
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryViewModel
import io.github.nosorae.aiopic.ui.featuresettings.SettingsViewModel
import io.github.nosorae.aiopic.ui.featuresettings.model.ModelSelectionStore
import io.github.nosorae.aiopic.ui.featuresettings.theme.ThemeModeStore
import kotlinx.coroutines.flow.flowOf

/** 앱 하나의 DI 그래프. 화면은 여기서 assisted 팩토리를 꺼내 ViewModel 을 만든다(ADR 0004, 0007). */
@DependencyGraph(AppScope::class)
interface AppGraph {
    val keyEntryViewModelFactory: KeyEntryViewModel.Factory

    val homeViewModel: HomeViewModel

    val settingsViewModel: SettingsViewModel

    val consentViewModel: ConsentViewModel

    /** 앱 틀이 구독해서 화면 모드를 고르는 즉시 테마를 바꾼다(F9.1). */
    val themeModeStore: ThemeModeStore

    // ponytail: 실제 키 보관(n-key)과 Gemini 키 확인(n-gemini)을 아직 그래프에 넣지 않아 메모리 보관소와 늘 통과하는 확인기를 둔다.
    // 플랫폼마다 Keystore, Keychain 구현을 팩토리 인자로 넘길 때 바꾼다. 키 입력과 설정이 같은 보관소를 보도록 하나만 만든다.
    @SingleIn(AppScope::class)
    @Provides
    fun keyVault(): KeyVault = object : KeyVault {
        private val keys = mutableMapOf<String, String>()

        override suspend fun get(provider: String): String? = keys[provider]

        override suspend fun put(provider: String, key: String) {
            keys[provider] = key
        }

        override suspend fun delete(provider: String) {
            keys.remove(provider)
        }
    }

    @Provides fun keyChecker(): KeyChecker = KeyChecker { _, _ -> DomainResult.Success(Unit) }

    // ponytail: 동의도 아직 플랫폼 저장소에 넣지 않아 메모리에 둔다. 설정의 무료 등급 안내가 동의 화면을 열 수 있게만 한다.
    @SingleIn(AppScope::class)
    @Provides
    fun voiceConsent(): VoiceConsent = object : VoiceConsent {
        private var given = false

        override suspend fun isGiven(): Boolean = given

        override suspend fun give() {
            given = true
        }
    }

    // ponytail: 회차를 저장하는 시험 화면(u-exam)이 아직 없어 빈 목록을 준다. 시험 화면이 Room 저장소(OpicStore)를
    // 그래프에 넣을 때 store::sessions 로 바꾼다.
    @Provides fun examSessionSource(): ExamSessionSource = ExamSessionSource { flowOf(emptyList()) }

    // ponytail: 첫 버전은 제공사가 Gemini 하나라 목록도 하나다.
    @Provides fun modelCatalog(): ModelCatalog = GeminiModels

    @Provides fun provideThemeModeStore(settings: AppSettings): ThemeModeStore = settings

    @Provides fun provideModelSelectionStore(settings: AppSettings): ModelSelectionStore = settings

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides settings: AppSettings): AppGraph
    }
}

/** 플랫폼 진입점이 Metro 컴파일러 플러그인 없이 그래프를 만들 수 있게 여기서 만든다. [settings] 는 플랫폼 설정 저장소다. */
fun createAppGraph(settings: AppSettings = AppSettings.inMemory()): AppGraph = createGraphFactory<AppGraph.Factory>().create(settings)
