plugins {
    alias(libs.plugins.aiopicAndroidApplication)
}

android {
    defaultConfig {
        applicationId = "io.github.nosorae.aiopic.spike.nativechrome"
        versionCode = 1
        versionName = "0"
    }
}

dependencies {
    implementation(project(":spikes:native-chrome:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation("androidx.navigation3:navigation3-ui:1.0.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
}
