package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.engine.answer.analysis.AnalysisJobs
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.pick.pickQuestions
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.ui.featureexam.ExamQuestion
import io.github.nosorae.aiopic.ui.featureexam.ExamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 시험 화면의 저장소. 회차와 답변은 [OpicStore] 에 두고(F3.1), 답변은 저장하자마자 분석 대기열에 넣는다(F5.1).
 * 등급 요청(F5.3)도 같은 대기열에 넣는다.
 * 녹음은 [recordingsRoot] 아래 `회차 id/단계 번호` 폴더에 쓴다. 단계마다 폴더가 하나라서 앱을 다시 켜도 같은 폴더에 이어 녹음한다(F3.3).
 *
 * 질문은 회차를 세 몫으로 나눠 받는다(ADR 0030). 점검의 연습 문항과 1번(자기소개)은 [pool] 에서 바로 고른다.
 * 2번부터 재조정 앞까지는 1번 Play 때, 재조정 뒤 문항은 재조정 두 문항 전 Play 때 [scope] 에서 [generate] 로 미리 요청한다.
 * 재조정 뒤 몫은 아직 고르기 전이라 선택지마다 하나씩 요청해 두고 고른 것을 쓴다. 실패하면 [pool] 에서 고른다(F6.2).
 * 서베이 밖 주제는 [maxOutsideTopics] 개까지 받는다. 내장 문항으로 고를 때 고른 주제의 문항이 없으면 [near] 가 주는 같은 묶음 주제를 먼저 쓴다.
 *
 * 질문 음성(#161)도 [prepareVoice] 로 미리 받는다. 몫을 받으면 그 몫의 첫 질문을, Play 마다 다음 질문을 받아서
 * 답하는 동안 다음 음성이 준비된다. 고르지 않은 재조정 선택지는 첫 질문 음성만 버린다.
 */
// ponytail: 미리 받는 요청은 이 객체의 메모리에만 있어서 앱이 꺼지면 사라진다. 이어 볼 때는 그 몫을 Play 에서 다시 요청하고 기다린다.
class StoreExamRepository(
    private val store: OpicStore,
    private val jobs: AnalysisJobs,
    private val spec: ExamSpec,
    private val pool: suspend () -> List<Question>,
    private val recordingsRoot: String,
    private val settings: AppSettings,
    private val maxOutsideTopics: Int,
    private val scope: CoroutineScope,
    /** 고른 주제와 같은 서베이 묶음의 나머지 주제. */
    private val near: (List<String>) -> List<String> = { emptyList() },
    /** 질문 하나의 음성을 받아 둔다. 이미 있으면 아무것도 안 한다. */
    private val prepareVoice: suspend (Question) -> Unit = {},
    /** 넘긴 질문 글의 음성만 남기고 받아 둔 질문 음성을 지운다(#185). */
    private val cleanVoices: (List<String>) -> Unit = {},
    /** 회차(재조정 포함), 받을 문항의 (번호, 유형), 이미 낸 질문으로 모델에 질문을 받는다. 키, 동의가 없거나 실패하면 null 이다. */
    private val generate: suspend (ExamSession, List<IndexedValue<QuestionType>>, List<Question>) -> List<Question>?,
) : ExamRepository {
    /** 미리 보낸 출제 요청. 키는 회차 id 와 몫(재조정 뒤 몫은 선택지까지)이다. Play 를 부르는 메인 스레드에서만 만진다. */
    private val requests = mutableMapOf<String, Deferred<List<Question>?>>()

    override suspend fun session(id: String): ExamSession? = store.session(id).first()

    /**
     * 회차가 끝날 때 답변 분석이 이미 다 끝나 있으면(시간 부족으로 끝난 경우 등) 여기서 등급을 요청한다(F5.3).
     * 질문 음성은 다시 듣기와 이어 보기에만 쓰여서 진행 중인 회차의 질문 음성만 남긴다(#185). 녹음은 리포트가 쓰므로 그대로 둔다.
     */
    override suspend fun save(session: ExamSession) {
        store.save(session)
        if (session.status == ExamStatus.InProgress) return
        jobs.requestGradeIfReady(session.id)
        val running = store.sessions().first().filter { it.status == ExamStatus.InProgress }
        cleanVoices(running.flatMap { settings.examQuestions(it.id).orEmpty() }.map { it.text })
    }

    /**
     * 지금 단계의 질문. 이미 낸 몫은 회차 id 로 저장한 목록에서 읽어서 앱을 다시 켜고 이어 봐도 같은 질문이 나온다(F3.3).
     * 아직 안 낸 몫이면 미리 보낸 요청을 기다리고, 요청이 없으면 지금 보내고 기다린다. 받은 몫은 저장한다.
     * 점검의 연습 문항은 첫 채점 문항과 같은 유형이고 채점 문항과 겹치지 않는다.
     */
    override suspend fun question(session: ExamSession): ExamQuestion? {
        val level = session.level ?: return null
        val firstType = spec.steps.firstNotNullOfOrNull { (it as? ExamStep.Ask)?.questionType } ?: return null
        val slots = spec.steps.withIndex().mapNotNull { (index, step) ->
            when {
                step is ExamStep.Check -> index to firstType
                step is ExamStep.Ask && step.minLevel <= level -> index to step.questionType
                else -> null
            }
        }
        val position = slots.indexOfFirst { it.first == session.stepIndex }.takeIf { it >= 0 } ?: return null
        val order = slots.map { it.second }
        val recalibrate = spec.steps.indexOf(ExamStep.Recalibrate)
        val afterRecalibration = slots.indexOfFirst { it.first > recalibrate }.takeIf { recalibrate >= 0 && it >= 0 } ?: slots.size
        val parts = listOf(0..BUILT_IN_LAST, BUILT_IN_LAST + 1 until afterRecalibration, afterRecalibration until slots.size).filterNot { it.isEmpty() }
        var saved = settings.examQuestions(session.id)?.takeIf { s -> s.map { it.type } == order.take(s.size) }.orEmpty()

        prefetch(session, order, parts, position, saved)
        if (position < saved.size) return ExamQuestion(saved[position]).also { prepareNextVoice(saved, position) }
        var fellBack = false
        for (part in parts.filter { it.first >= saved.size && it.first <= position }) {
            val request = if (part.first == 0) null else requests.getOrPut(key(session, part)) { request(session, order, part, saved) }
            // 받은 뒤에 지운다. 기다리다 Play 가 취소돼도(나가기) 다음 Play 가 같은 요청을 이어 기다린다.
            val generated = request?.await()
            // 재조정 뒤 몫이면 고르지 않은 선택지의 요청을 버린다.
            requests.filterKeys { it.startsWith("${session.id}/${part.first}/") }.forEach { (key, other) ->
                other.cancel()
                requests.remove(key)
            }
            val picked = pickQuestions(order, session.surveyTopics, pool(), maxOutsideTopics, Random(session.id.hashCode()), part, near(session.surveyTopics)) { generated } ?: return null
            fellBack = request != null && picked != generated
            saved = saved + picked
        }
        settings.saveExamQuestions(session.id, saved)
        prepareNextVoice(saved, position)
        return ExamQuestion(saved[position], fellBack)
    }

    private fun prepareNextVoice(saved: List<Question>, position: Int) {
        saved.getOrNull(position + 1)?.let { scope.launch { prepareVoice(it) } }
    }

    /** 1번 Play 에서 다음 몫을, 재조정 두 문항 전 Play 에서 재조정 뒤 몫을 선택지마다 미리 보낸다. 이미 받았거나 보냈으면 그대로 둔다. */
    private fun prefetch(session: ExamSession, order: List<QuestionType>, parts: List<IntRange>, position: Int, saved: List<Question>) {
        val next = parts.getOrNull(1)?.takeIf { position >= BUILT_IN_LAST && saved.size <= it.first }
        next?.let { requests.getOrPut(key(session, it)) { request(session, order, it, saved) } }
        val last = parts.getOrNull(2)?.takeIf { position >= it.first - 2 && saved.size in parts[1].last + 1..it.first } ?: return
        Recalibration.entries.forEach { choice ->
            val chosen = session.copy(recalibration = choice)
            requests.getOrPut(key(chosen, last)) { request(chosen, order, last, saved) }
        }
    }

    private fun request(session: ExamSession, order: List<QuestionType>, part: IntRange, asked: List<Question>): Deferred<List<Question>?> = scope.async {
        generate(session, order.withIndex().toList().slice(part), asked)?.also { questions ->
            // 요청이 취소돼도(고르지 않은 선택지) 받기 시작한 음성은 마저 받는다. 파일 하나라 버려도 작다.
            questions.firstOrNull()?.let { scope.launch { prepareVoice(it) } }
        }
    }

    /** 재조정 뒤 몫의 요청은 선택지마다 다르다. 고르기 전 몫은 재조정이 없어서 키가 하나다. */
    private fun key(session: ExamSession, part: IntRange) = "${session.id}/${part.first}/${session.recalibration ?: Recalibration.Same}"

    override fun recordingDirectory(session: ExamSession): String = "$recordingsRoot/${session.id}/${session.stepIndex}"

    override suspend fun save(answer: Answer) {
        store.save(answer)
        jobs.enqueueAnswer(answer.id)
    }
}

/** 점검의 연습 문항과 1번 문항의 위치. 여기까지는 내장 문항이라 첫 Play 가 모델을 기다리지 않는다. */
private const val BUILT_IN_LAST = 1
