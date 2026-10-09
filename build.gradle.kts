plugins {
    alias(libs.plugins.aiopicLayerCheck)
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
