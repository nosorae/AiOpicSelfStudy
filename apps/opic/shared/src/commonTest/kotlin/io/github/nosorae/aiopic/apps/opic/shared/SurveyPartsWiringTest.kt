package io.github.nosorae.aiopic.apps.opic.shared

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import io.github.nosorae.aiopic.ui.featureexam.ExamViewModel
import io.github.nosorae.aiopic.ui.featureexam.SurveyPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 시험 팩의 서베이 Part 1~3 질문이 앱 그래프를 거쳐 시험 화면 상태까지 오는지 본다(F2.2). */
class SurveyPartsWiringTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `F2_2 시험 화면의 서베이에 팩의 Part 1부터 3 질문과 보기가 그대로 들어온다`() = runTest(dispatcher) {
        val graph = testAppGraph()
        val viewModel = ViewModelProvider.create(store, viewModelFactory { initializer { graph.examViewModelFactory.create("s1") } })[ExamViewModel::class]

        val state = viewModel.uiState.first { it.step is ExamStep.Survey }

        assertEquals(OpicEn.surveyParts.map { (question, options) -> SurveyPart(question, options) }, state.surveyParts)
    }
}
