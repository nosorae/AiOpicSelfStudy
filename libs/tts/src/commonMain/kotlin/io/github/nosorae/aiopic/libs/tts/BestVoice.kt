package io.github.nosorae.aiopic.libs.tts

/**
 * 영어(미국) 음성 가운데 품질이 가장 높은 것. iOS 는 Premium, Enhanced, 기본 순이고 Android 는 품질 값이 큰 순이다.
 * 같은 품질이면 앞에 있는 것을 고른다. 영어(미국)가 없으면 null 이다.
 */
internal fun <T> bestVoice(voices: List<T>, isUsEnglish: (T) -> Boolean, quality: (T) -> Int): T? = voices.filter(isUsEnglish).maxByOrNull(quality)
