plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 답변, 회차, 약점 점수 타입을 그대로 받아 그린다(F7).
        implementation(project(":engine:answer"))
    }
}
