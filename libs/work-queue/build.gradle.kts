plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime)
        }
        getByName("androidHostTest").dependencies {
            // WorkManager 를 JVM 에서 돌리고 네트워크 조건과 재시도 시각을 테스트에서 직접 채운다.
            implementation(libs.androidx.work.testing)
            implementation(libs.robolectric)
            implementation(libs.junit)
        }
    }
}
