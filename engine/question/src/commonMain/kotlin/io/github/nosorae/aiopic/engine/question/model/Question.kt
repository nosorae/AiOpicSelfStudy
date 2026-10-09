package io.github.nosorae.aiopic.engine.question.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** 질문 하나. 화면에는 글자를 보이지 않고 [text] 를 기기 음성으로 읽는다(F2.3). */
@Serializable
data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    /** 서베이 주제. 약점 출제와 출력 검사(F5.4)가 이 값을 본다. */
    val topic: String,
)

/**
 * 문항 유형(자기소개, 롤플레이 등). 유형 목록은 시험마다 달라서 엔진은 이름만 들고 값은 시험 팩이 정한다.
 * 약점 점수는 이 값으로 묶는다.
 */
@Serializable
@JvmInline
value class QuestionType(val id: String)
