plugins {
    alias(libs.plugins.aiopicKmpCompose)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":ui:design"))
        implementation(project(":ui:feature-onboarding"))
        implementation(project(":libs:keyvault"))
        implementation(libs.androidx.lifecycle.viewmodelCompose)
    }
}
