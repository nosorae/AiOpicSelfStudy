plugins {
    alias(libs.plugins.aiopicUiFeature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // 검증된 모델 목록(ModelCatalog)과 기본 모델로 돌리는 규칙(choose)을 쓴다.
        // 틀림 표시 내보내기도 답변과 표시 타입을 그대로 읽는다(F8.3).
        implementation(project(":engine:answer"))
        // 키를 넣을 수 있는 제공사 목록(KeyProvider)을 키 입력 화면과 같이 쓴다.
        implementation(project(":ui:feature-onboarding"))
        implementation(project(":libs:keyvault"))
    }
}
