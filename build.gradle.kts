plugins {
    alias(libs.plugins.aiopicLayerCheck)
    alias(libs.plugins.aiopicSpotless)
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
