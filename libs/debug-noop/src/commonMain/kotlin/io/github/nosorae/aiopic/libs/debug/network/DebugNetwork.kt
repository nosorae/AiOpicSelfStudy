package io.github.nosorae.aiopic.libs.debug.network

import io.ktor.client.HttpClientConfig

/** 릴리스 빌드용 빈 몸. 플러그인을 설치하지 않는다. */
public fun HttpClientConfig<*>.installDebugTools() {}
