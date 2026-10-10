import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
    sourceSets {
        commonMain.dependencies {
            // installDebugTools 가 HttpClientConfig 를 받아서 api 로 둔다.
            api(libs.ktor.client.core)
        }
    }
}
