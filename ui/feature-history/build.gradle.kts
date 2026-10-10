plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 답변, 회차, 약점 점수 타입을 그대로 받아 그린다(F7).
        implementation(project(":engine:answer"))
    }
    // 등급 구슬, 점수 숫자가 멈춘 끝 장면을 테스트 시계로 넘겨 찍고, 동작 줄이기면 처음부터 끝 장면인지 본다.
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.compose.uiTestManifest)
        implementation(libs.roborazzi.core)
    }
}
