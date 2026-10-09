package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.Serializable

/** 등급 이름을 낮은 것부터 늘어놓은 목록. 값은 시험 팩이 준다. */
@Serializable
data class GradeScale(val grades: List<String>) {
    init {
        require(grades.isNotEmpty() && grades.distinct().size == grades.size) { "등급은 하나 이상이고 겹치면 안 된다: $grades" }
    }
}
