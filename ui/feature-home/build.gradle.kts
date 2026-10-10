plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 이어 볼 회차와 지난 회차를 저장된 회차 상태(ExamSession)에서 고른다. 앱 층이 ExamSessionSource 를 넣으므로 api 다.
        api(project(":engine:session"))
    }
    // 회차를 읽는 동안 시험 버튼이 눌리지 않는지 화면을 띄워 누르는 테스트로 본다.
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.compose.uiTestManifest)
    }
}
