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
        implementation(project(":libs:keyvault"))
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
