plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 이어 볼 회차와 지난 회차를 저장된 회차 상태(ExamSession)에서 고른다. 앱 층이 ExamSessionSource 를 넣으므로 api 다.
        api(project(":engine:session"))
        // 등급을 기다리는 회차의 분석 진행을 답변(Answer)에서 센다. ExamSessionSource 가 답변을 내보내므로 api 다.
        api(project(":engine:answer"))
    }
    // 회차를 읽는 동안 시험 버튼이 눌리지 않는지 화면을 띄워 누르는 테스트로 본다.
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.compose.uiTestManifest)
        // 새로 시작 확인 대화상자는 따로 뜨는 창이라 화면 전체를 찍는다.
        implementation(libs.roborazzi.core)
    }
}
