import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.aiopicKmpLibraryExplicit)
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}
