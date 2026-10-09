package io.github.nosorae.aiopic.ui.featureexam

import io.github.nosorae.aiopic.engine.session.model.ExamSpec

/** 시험 팩이 주는 시험 한 회의 모양과 서베이 주제. 화면은 시험 팩을 모르고 앱 층이 이 값을 넣는다. */
data class ExamContent(val spec: ExamSpec, val topics: List<String>)
