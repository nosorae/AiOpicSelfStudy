plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    android {
        // MediaRecorder, 오디오 포커스, 포그라운드 서비스는 기기에만 있어서 Android 쪽 확인은 계측 테스트로 한다.
        // commonTest 는 넣지 않는다. 그쪽 테스트 이름의 공백을 minSdk 28 의 dex 가 받지 못한다.
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
