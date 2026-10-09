import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

val jvm = libs.versions.jvm.get()

java {
    sourceCompatibility = JavaVersion.toVersion(jvm)
    targetCompatibility = JavaVersion.toVersion(jvm)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvm)
    }
}

application {
    mainClass = "io.github.nosorae.aiopic.tools.eval.MainKt"
}
