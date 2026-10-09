plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.composeCompiler.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    testImplementation(libs.kotlin.testJunit)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.aiopicKmpLibrary.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.KmpLibraryPlugin"
        }
        register("kmpLibraryExplicit") {
            id = libs.plugins.aiopicKmpLibraryExplicit.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.KmpLibraryExplicitPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.aiopicKmpCompose.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.KmpComposePlugin"
        }
        register("uiFeature") {
            id = libs.plugins.aiopicUiFeature.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.UiFeaturePlugin"
        }
        register("androidApplication") {
            id = libs.plugins.aiopicAndroidApplication.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.AndroidApplicationPlugin"
        }
        register("layerCheck") {
            id = libs.plugins.aiopicLayerCheck.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.LayerCheckPlugin"
        }
        register("spotless") {
            id = libs.plugins.aiopicSpotless.get().pluginId
            implementationClass = "io.github.nosorae.aiopic.buildlogic.SpotlessPlugin"
        }
    }
}
