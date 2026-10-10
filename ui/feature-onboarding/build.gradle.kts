plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":libs:keyvault"))
    }
    // 키 받기 버튼이 여는 주소와 키 보기 버튼을 화면을 띄워 누르는 테스트로 본다.
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.compose.uiTestManifest)
    }
}
