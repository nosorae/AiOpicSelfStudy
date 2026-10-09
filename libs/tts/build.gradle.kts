plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // 음성 엔진의 끝 알림을 기다리는 동안 취소되면 읽기도 멈춘다.
            implementation(libs.kotlinx.coroutines.core)
        }
        getByName("androidHostTest").dependencies {
            // Android 음성 엔진의 끝 알림을 JVM 에서 흉내 낸다.
            implementation(libs.robolectric)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.junit)
        }
    }
}
