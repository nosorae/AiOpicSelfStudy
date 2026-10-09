plugins {
    alias(libs.plugins.aiopicKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

// engine/answer 의 분석 포트를 libs/llm-gemini 클라이언트로 구현하는 어댑터 모듈이다.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine:answer"))
            implementation(project(":libs:llm-gemini"))
        }
        commonTest.dependencies {
            // 저장해 둔 실제 응답을 MockEngine 으로 돌려준다.
            implementation(libs.ktor.client.mock)
        }
    }
}
