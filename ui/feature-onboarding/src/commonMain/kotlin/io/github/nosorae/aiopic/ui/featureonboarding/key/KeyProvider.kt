package io.github.nosorae.aiopic.ui.featureonboarding.key

/**
 * 키를 넣을 수 있는 AI 제공사. [id] 는 키 보관소와 분석 결과에 남는 제공사 이름이고 [label] 은 화면에 보이는 이름이다.
 * [keyPageUrl] 은 키 입력 화면과 설정의 "키 받기" 버튼이 시스템 브라우저로 여는 키 발급 페이지다.
 * 첫 버전은 Gemini 하나만 둔다(F1.1). 제공사를 더하면 이 목록에 한 줄을 더한다.
 */
enum class KeyProvider(val id: String, val label: String, val keyPageUrl: String) {
    Gemini("gemini", "Gemini", "https://aistudio.google.com/apikey"),
}
