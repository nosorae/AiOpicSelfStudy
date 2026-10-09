plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 이어 볼 회차와 지난 회차를 저장된 회차 상태(ExamSession)에서 고른다. 앱 층이 ExamSessionSource 를 넣으므로 api 다.
        api(project(":engine:session"))
    }
}
