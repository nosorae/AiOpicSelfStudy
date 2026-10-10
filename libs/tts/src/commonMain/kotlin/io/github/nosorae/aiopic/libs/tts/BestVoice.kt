package io.github.nosorae.aiopic.libs.tts

/**
 * 바로 쓸 수 있는 영어(미국, `en-US`) 음성 가운데 품질이 가장 높은 것. iOS 는 Premium, Enhanced, 기본 순이고 Android 는 품질 값이 큰 순이다.
 * [installed] 가 false 인 음성(Android 의 네트워크가 필요하거나 아직 안 받은 음성)은 고르지 않는다.
 * 같은 품질이면 앞에 있는 것을 고른다. 고를 음성이 없으면 null 이다.
 */
internal fun <T> bestVoice(voices: Iterable<T>, language: (T) -> String, quality: (T) -> Int, installed: (T) -> Boolean = { true }): T? = voices.filter { language(it) == "en-US" && installed(it) }.maxByOrNull(quality)
