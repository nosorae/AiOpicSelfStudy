package org.yessorae.side

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform