plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    android {
        // Keystore 는 기기에만 있어서 Android 쪽 확인은 계측 테스트로 한다. commonTest 의 계약 테스트를 그대로 돌린다.
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    // 서명 안 된 테스트 실행 파일은 Keychain 에 errSecMissingEntitlement(-34018)로 막힌다.
    // 시뮬레이터는 실행 파일 안 __entitlements 구역을 권한으로 읽으므로 테스트 실행 파일에만 넣는다.
    iosSimulatorArm64().binaries.getTest("DEBUG").linkerOpts(
        "-sectcreate",
        "__TEXT",
        "__entitlements",
        file("src/iosTest/keychain-test.entitlements").absolutePath,
    )
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.testExt.junit)
        }
    }
}

// Keychain 은 부팅된 시뮬레이터에서만 열린다. 테스트를 시뮬레이터 없이 따로 띄우는 기본 실행에서는
// SecItem 호출이 errSecNotAvailable(-25291)로 끝난다. 그래서 iPhone 17 을 띄우고 그 안에서 돌린다.
val bootSimulator by tasks.registering(Exec::class) {
    commandLine("xcrun", "simctl", "boot", "iPhone 17")
    // 이미 떠 있으면 0 이 아닌 값으로 끝난다.
    isIgnoreExitValue = true
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    dependsOn(bootSimulator)
    standalone.set(false)
    device.set("iPhone 17")
}
