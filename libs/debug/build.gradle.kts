import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    // 앱은 컴파일할 때 debug-noop 을 보고 디버그 빌드에서만 이 모듈로 바꿔 끼운다. 그래서 두 모듈의 공개 API 가 같아야 한다.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // installDebugTools 가 HttpClientConfig 를 받아서 api 로 둔다.
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.junit)
        }
    }
}

// 앱은 debug-noop 으로 컴파일하고 디버그 빌드에서 이 모듈로 바꿔 끼운다. noop 에만 있는 공개 API 가 있으면
// 디버그 빌드가 실행 중에 그 함수를 못 찾고 죽는다. 그래서 noop 덤프의 모든 줄이 이 모듈 덤프에 있는지 본다.
// 덤프가 코드와 같은지는 두 모듈의 checkKotlinAbi 가 본다. 모듈 이름 줄은 모듈마다 달라 뺀다.
val checkNoopAbi by tasks.registering {
    group = "verification"
    description = "debug-noop 의 공개 API 가 모두 이 모듈에 있는지 검사한다."
    val dumps = mapOf(
        file("api/debug.klib.api") to file("../debug-noop/api/debug-noop.klib.api"),
        file("api/android/debug.api") to file("../debug-noop/api/android/debug-noop.api"),
    )
    inputs.files(dumps.keys + dumps.values)
    doLast {
        val missing = dumps.flatMap { (real, noop) ->
            val realLines = real.readLines().toSet()
            noop.readLines().filter { it !in realLines && !it.startsWith("// Library unique name") }
        }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "debug-noop 에만 있는 공개 API ${missing.size}줄. 고칠 방법: libs/debug 에 같은 API 를 더하고 " +
                    "updateKotlinAbi 로 두 모듈 덤프를 다시 쓴다.\n" + missing.joinToString("\n"),
            )
        }
    }
}
tasks.named("check") { dependsOn(checkNoopAbi) }
