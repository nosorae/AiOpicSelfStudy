plugins {
    alias(libs.plugins.aiopicAndroidApplication)
}

android {
    defaultConfig {
        applicationId = "io.github.nosorae.aiopic"
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
