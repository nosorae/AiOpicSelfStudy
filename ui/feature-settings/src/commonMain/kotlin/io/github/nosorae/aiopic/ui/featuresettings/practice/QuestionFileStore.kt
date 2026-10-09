package io.github.nosorae.aiopic.ui.featuresettings.practice

/**
 * 사용자가 불러온 문항 파일(F6.3)을 검사를 마친 JSON 그대로 기기에 남긴다. 앱 층이 이 파일의 문항을 한 문항 연습 풀에 더한다.
 * 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface QuestionFileStore {
    /** 불러온 파일이 없으면 null 이다. */
    suspend fun questionFile(): String?

    suspend fun saveQuestionFile(json: String)
}
