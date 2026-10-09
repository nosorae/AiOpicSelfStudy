plugins {
    alias(libs.plugins.aiopicKmpCompose)
    alias(libs.plugins.metro)
    // Android 앱 틀이 백스택을 화면 회전과 프로세스 종료 뒤에도 되살리려고 경로를 JSON 으로 저장한다.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":ui:design"))
        implementation(project(":ui:feature-onboarding"))
        implementation(project(":ui:feature-home"))
        implementation(project(":ui:feature-settings"))
        // 화면 저장소 인터페이스를 Room 저장소(OpicStore) 위에 구현한다(data/).
        implementation(project(":ui:feature-exam"))
        implementation(project(":ui:feature-practice"))
        implementation(project(":ui:feature-report"))
        implementation(project(":ui:feature-history"))
        // 설정의 모델 목록으로 앱이 들고 있는 Gemini 목록을 넣는다.
        implementation(project(":engine:answer"))
        implementation(project(":engine:ai-gemini"))
        implementation(project(":libs:keyvault"))
        // 그래프가 플랫폼 인자(녹음기, 기기 음성)를 받고 Gemini 클라이언트, 시험 팩, 시계를 만든다.
        implementation(project(":libs:recorder"))
        implementation(project(":libs:tts"))
        implementation(project(":libs:timer"))
        implementation(project(":libs:llm-gemini"))
        implementation(project(":packs:opic-en"))
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.kotlinx.serialization.json)
    }
    // Android 앱 틀의 하단 탭은 Material 3, 뒤로가기는 Navigation 3 와 Activity 의 뒤로가기 처리를 쓴다. iOS 는 SwiftUI 가 그린다.
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.navigation3.ui)
        implementation(libs.androidx.activity.compose)
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.compose.uiTestManifest)
    }
}

// 호스트 JVM 테스트에는 Android 용 SQLite 네이티브 라이브러리가 없어서, 같은 API 의 JVM 판으로 바꿔 돌린다.
configurations.matching { it.name.contains("HostTest") && it.name.endsWith("RuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.sqlite:sqlite-bundled")).using(module(libs.sqlite.bundled.jvm.get().toString()))
    }
}
