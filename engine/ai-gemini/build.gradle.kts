plugins {
    alias(libs.plugins.aiopicKmpLibrary)
}

// engine/answer 의 분석 포트를 libs/llm-gemini 클라이언트로 구현하는 어댑터 모듈이다. 구현은 n-gemini 노드에서 한다.
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":engine:answer"))
        implementation(project(":libs:llm-gemini"))
    }
}
