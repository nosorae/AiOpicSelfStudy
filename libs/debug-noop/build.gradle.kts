import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.aiopicKmpComposeExplicit)
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}
