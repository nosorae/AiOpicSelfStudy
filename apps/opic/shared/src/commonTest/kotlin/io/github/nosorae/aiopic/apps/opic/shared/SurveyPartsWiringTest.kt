package io.github.nosorae.aiopic.apps.opic.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import io.github.nosorae.aiopic.ui.featureexam.ExamViewModel
import io.github.nosorae.aiopic.ui.featureexam.SurveyIntent
import io.github.nosorae.aiopic.ui.featureexam.SurveyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 시험 팩의 서베이가 앱 그래프를 거쳐 시험 화면과 설정의 서베이까지 오고, 설정에서 바꾼 답이 모의고사에 채워지는지 본다(F2.2). 한 문항 연습 주제는 AppSettingsTest 가 본다. */
class SurveyPartsWiringTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val settings = AppSettings.inMemory()
    private val graph = testAppGraph(settings)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private inline fun <reified T : ViewModel> viewModel(key: String, crossinline create: () -> T): T = ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[key, T::class]

    @Test
    fun `F2_2 시험 화면의 서베이는 팩의 서베이 Part 1부터 3 질문과 네 묶음이다`() = runTest(dispatcher) {
        val exam = viewModel("exam") { graph.examViewModelFactory.create("s1") }

        val state = exam.uiState.first { it.step is ExamStep.Survey && it.survey != null }

        assertEquals(OpicEn.survey, state.survey?.survey)
    }

    @Test
    fun `F2_2 설정에서 바꾼 서베이 답이 다음 모의고사 서베이에 채워진다`() = runTest(dispatcher) {
        val survey = viewModel("survey") { graph.surveyViewModel }
        advanceUntilIdle()
        listOf("none", "no", "over-5-years", "family").forEach { survey.onIntent(SurveyIntent.ChooseOption(it)) }
        val topics = listOf("movies", "parks", "clubs", "beach", "camping", "music", "cooking", "reading-to-children", "jogging", "walking", "hiking", "domestic-travel")
        topics.forEach { survey.onIntent(SurveyIntent.ToggleTopic(it)) }
        survey.onIntent(SurveyIntent.Submit)
        advanceUntilIdle()

        val exam = viewModel("exam") { graph.examViewModelFactory.create("s2") }
        val state = exam.uiState.first { it.survey != null }

        assertEquals(topics.toSet(), state.survey?.topics)
        assertEquals(mapOf("work" to "none", "student" to "no", "course-last" to "over-5-years", "housing" to "family"), state.survey?.choices)
    }
}
