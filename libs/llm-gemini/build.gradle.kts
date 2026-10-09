plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // GeminiClient 생성자가 HttpClient 를 받아서 api 로 둔다. 테스트는 MockEngine 을 넣는다.
            api(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
