package io.github.nosorae.aiopic.apps.opic.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform