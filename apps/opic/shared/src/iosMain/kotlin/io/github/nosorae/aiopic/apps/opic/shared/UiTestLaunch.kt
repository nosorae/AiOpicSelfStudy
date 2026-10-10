package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.apps.opic.shared.data.deleteAppData
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/**
 * XCUITest(iosAppUITests)가 앱을 띄울 때 주는 실행 인자. 사용자는 iOS 앱에 실행 인자를 줄 수 없어서 테스트에서만 켜진다.
 * 값은 Swift 의 `UITestLaunch` 와 같다.
 */
internal object UiTestLaunch {
    private val arguments = NSProcessInfo.processInfo.arguments

    /** Gemini 대신 늘 "OK" 를 돌려준다. 가짜 키가 키 확인을 넘고(F1.1), 출제와 분석은 응답을 못 읽어 내장 문항과 분석 실패로 간다. */
    val fakeGemini = "ui-test-fake-gemini" in arguments

    /** 저장된 키, 설정, DB, 녹음을 지우고 첫 실행처럼 시작한다(F1.0). 시뮬레이터는 앱을 다시 설치해도 이것들이 남는다. */
    val reset = "ui-test-reset" in arguments

    // ponytail: 테스트용 가짜 엔진이 앱 바이너리에 실린다. 실행 인자로만 켜지고 크기가 작아서 빌드 구성을 나누지 않는다.
    fun geminiClient(): GeminiClient = if (fakeGemini) {
        GeminiClient(HttpClient(MockEngine { respond(OK_RESPONSE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }))
    } else {
        GeminiClient()
    }

    /** 그래프가 DB 와 녹음 폴더를 만들기 전에 부른다. */
    fun resetIfAsked(keyVault: KeyVault) {
        if (!reset) return
        NSBundle.mainBundle.bundleIdentifier?.let(NSUserDefaults.standardUserDefaults::removePersistentDomainForName)
        deleteAppData()
        // Keychain 삭제는 기다릴 일이 없는 짧은 호출이라 앱 시작에서 막고 끝낸다.
        runBlocking { keyVault.delete(GeminiModels.PROVIDER) }
    }

    private const val OK_RESPONSE = """{"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}"""
}
