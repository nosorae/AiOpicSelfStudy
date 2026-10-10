plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine:session"))
            implementation(project(":engine:question"))
            implementation(project(":engine:answer"))
            implementation(project(":libs:tts"))
            implementation(project(":libs:recorder"))
            implementation(project(":libs:timer"))
        }
        androidMain.dependencies {
            // 마이크 권한 요청 창을 띄운다.
            implementation(libs.androidx.activity.compose)
        }
        // 처리 중인 버튼이 눌리지 않는지 화면을 띄워 누르는 테스트로 본다.
        getByName("androidHostTest").dependencies {
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.compose.uiTestManifest)
        }
    }
}
