package io.github.nosorae.aiopic.apps.opic.shared

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
