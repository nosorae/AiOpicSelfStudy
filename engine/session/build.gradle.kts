plugins {
    alias(libs.plugins.aiopicKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:question"))
            api(libs.kotlinx.serialization.json)
        }
    }
}
