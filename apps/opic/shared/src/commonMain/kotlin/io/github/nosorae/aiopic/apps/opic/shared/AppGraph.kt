package io.github.nosorae.aiopic.apps.opic.shared

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraph
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyChecker
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryViewModel

/** 앱 하나의 DI 그래프. 화면은 여기서 assisted 팩토리를 꺼내 ViewModel 을 만든다(ADR 0004, 0007). */
@DependencyGraph
interface AppGraph {
    val keyEntryViewModelFactory: KeyEntryViewModel.Factory

    // ponytail: 실제 키 보관(n-key)과 Gemini 키 확인(n-gemini)이 아직 없어 메모리 보관소와 늘 통과하는 확인기를 둔다.
    // 두 노드가 끝나면 플랫폼마다 그래프를 나눠 Keystore, Keychain 구현과 Gemini 요청을 넣는다.
    @Provides fun keyVault(): KeyVault = object : KeyVault {
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
}

/** 플랫폼 진입점이 Metro 컴파일러 플러그인 없이 그래프를 만들 수 있게 여기서 만든다. */
fun createAppGraph(): AppGraph = createGraph()
