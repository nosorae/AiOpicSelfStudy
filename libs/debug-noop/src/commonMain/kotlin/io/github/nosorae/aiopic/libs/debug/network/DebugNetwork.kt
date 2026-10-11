package io.github.nosorae.aiopic.libs.debug.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine

/** 릴리스 빌드용 빈 몸. 플러그인을 설치하지 않는다. */
public fun HttpClientConfig<*>.installDebugTools() {}

/** 릴리스 빌드용 빈 몸. 가짜 응답 엔진이 없어서 늘 null 이고 앱은 실제 엔진을 쓴다. */
public fun fakeHttpEngine(json: (requestBody: String) -> String): HttpClientEngine? = null
