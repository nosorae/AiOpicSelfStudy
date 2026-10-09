plugins {
    alias(libs.plugins.aiopicKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    // 프롬프트 파일을 composeResources/files 에 두고 Res.readBytes 로 읽으려고 건다. 화면 코드는 없다.
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:session"))
            api(project(":engine:answer"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
        }
        iosTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.nosorae.aiopic.packs.opicen"
}
