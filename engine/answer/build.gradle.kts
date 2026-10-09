plugins {
    alias(libs.plugins.aiopicKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":engine:question"))
            // 저장소가 회차 진행 상태(ExamSession)를 답변과 같은 DB 에 둔다(F3.1).
            api(project(":engine:session"))
            api(libs.kotlinx.serialization.json)
            api(libs.room3.runtime)
            api(libs.sqlite.bundled)
        }
    }
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room3.compiler) }
}

// 호스트 JVM 테스트에는 Android 용 SQLite 네이티브 라이브러리가 없어서, 같은 API 의 JVM 판으로 바꿔 돌린다.
configurations.matching { it.name.contains("HostTest") && it.name.endsWith("RuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.sqlite:sqlite-bundled")).using(module(libs.sqlite.bundled.jvm.get().toString()))
    }
}
