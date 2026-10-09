plugins {
    alias(libs.plugins.aiopicAndroidApplication)
}

android {
    defaultConfig {
        applicationId = "io.github.nosorae.aiopic"
        versionCode = 1
        versionName = "1.0"
        // 시나리오 테스트가 Gemini 만 저장한 응답으로 바꾼 앱으로 띄운다.
        testInstrumentationRunner = "io.github.nosorae.aiopic.apps.opic.androidapp.E2eRunner"
    }
    // 테스트마다 계측을 새로 띄워서, 앞 테스트가 끝날 때 앱 프로세스가 강제 종료된 뒤 이어 보기를 확인한다.
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":apps:opic:shared"))
    // 기기에 묶인 객체(키 보관소, 녹음기, 기기 음성, 대기열)를 앱이 만들어 그래프에 넘긴다.
    implementation(project(":engine:answer"))
    implementation(project(":libs:keyvault"))
    implementation(project(":libs:recorder"))
    implementation(project(":libs:tts"))
    implementation(project(":libs:work-queue"))
    // 시작 분기(F1.0)가 끝날 때까지 시스템 스플래시를 잡아 두고, 그 결정을 맡은 ViewModel 을 Activity 가 만든다.
    implementation(project(":ui:feature-onboarding"))
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    // 시나리오 테스트가 Gemini 클라이언트를 가짜 HTTP 엔진으로 바꿔 넣는다.
    implementation(project(":libs:llm-gemini"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTestJunit4)
    androidTestImplementation(libs.ktor.client.mock)
    androidTestUtil(libs.androidx.test.orchestrator)
}
