plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    sourceSets {
        getByName("androidHostTest").dependencies {
            // 부팅 이후 시계를 JVM 에서 앞으로 돌린다.
            implementation(libs.robolectric)
            implementation(libs.junit)
        }
    }
}
