plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    android {
        // 에뮬레이터의 실제 View 와 Vibrator 로 모든 효과가 예외 없이 불리는지 본다. 손 느낌은 실기기에서 사람이 본다.
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        getByName("androidHostTest").dependencies {
            // 효과마다 어느 시스템 진동 상수를 쓰는지 API 레벨별로 JVM 에서 본다.
            implementation(libs.robolectric)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.junit)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.kotlin.test)
        }
    }
}
