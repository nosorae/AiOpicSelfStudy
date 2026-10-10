import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.aiopicKmpComposeExplicit)
}

kotlin {
    // 앱은 컴파일할 때 debug-noop 을 보고 디버그 빌드에서만 이 모듈로 바꿔 끼운다. 그래서 두 모듈의 공개 API 가 같아야 한다.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        // Compose 컴파일러가 만드는 람다 보관 클래스는 화면 코드를 고칠 때마다 이름이 바뀌고 앱이 쓰지 않는다.
        filters.exclude.byNames.add("**.ComposableSingletons**")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // installDebugTools 가 HttpClientConfig 를 받아서 api 로 둔다.
            api(libs.ktor.client.core)
            // fakeHttpEngine 의 가짜 응답. 이 모듈은 디버그 빌드에만 실려서 릴리스 빌드에는 이 의존성이 없다.
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            // 화면을 앱과 따로 띄우는 Activity 와 공유할 보고서 파일을 넘기는 FileProvider.
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.compose.uiTestManifest)
            implementation(libs.roborazzi.core)
        }
    }
}

// 문자열과 그림 리소스가 없다. Res 클래스를 만들지 않아 공개 API 덤프에 섞이지 않게 한다.
compose.resources { generateResClass = never }

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
