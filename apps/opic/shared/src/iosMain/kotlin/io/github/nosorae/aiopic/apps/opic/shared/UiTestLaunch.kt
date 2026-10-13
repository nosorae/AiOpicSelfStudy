package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.apps.opic.shared.data.deleteAppData
import io.github.nosorae.aiopic.engine.aigemini.GeminiModels
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/**
 * XCUITest(iosAppUITests)가 앱을 띄울 때 주는 실행 인자. 사용자는 iOS 앱에 실행 인자를 줄 수 없어서 테스트에서만 켜진다.
 * 값은 Swift 의 `UITestLaunch` 와 같다. 가짜 Gemini, 느린 응답, 녹음 실패는 디버깅 스위치([DebugSwitches])를 실행 인자로 켠다.
 */
internal object UiTestLaunch {
    /** 저장된 키, 설정, DB, 녹음을 지우고 첫 실행처럼 시작한다(F1.0). 시뮬레이터는 앱을 다시 설치해도 이것들이 남는다. */
    private val reset = "ui-test-reset" in NSProcessInfo.processInfo.arguments

    /** 스토어 스크린샷용 예시 기록([seedStoreDemo])을 넣는다(#236). reset 과 함께 줘서 빈 DB 에 넣는다. */
    val storeDemo = "ui-test-store-demo" in NSProcessInfo.processInfo.arguments

    /** 그래프가 DB 와 녹음 폴더를 만들기 전에 부른다. */
    fun resetIfAsked(keyVault: KeyVault) {
        if (!reset) return
        NSBundle.mainBundle.bundleIdentifier?.let(NSUserDefaults.standardUserDefaults::removePersistentDomainForName)
        deleteAppData()
        // Keychain 삭제는 기다릴 일이 없는 짧은 호출이라 앱 시작에서 막고 끝낸다.
        runBlocking { keyVault.delete(GeminiModels.PROVIDER) }
    }
}
