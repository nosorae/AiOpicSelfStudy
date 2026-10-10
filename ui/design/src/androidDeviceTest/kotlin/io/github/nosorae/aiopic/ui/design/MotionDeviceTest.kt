package io.github.nosorae.aiopic.ui.design

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sin

private val grades = listOf("NL", "NM", "NH", "IL", "IM", "IH", "AL")

/**
 * 실제 프레임 시계에서 움직임 부품을 띄운다. 부품마다 UI 스레드가 한 프레임에 쓰는 시간(애니메이션, 측정과 배치, 그리기 기록)을 잰다.
 * 부품이 쓰는 몫이 여기다. 프레임 전체 시간은 GPU 와 합성까지 들어가서, 소프트웨어 GPU 로 도는 에뮬레이터에서는 부품과 상관없이
 * 16.7ms 를 자주 넘는다. 그래서 전체 시간은 기록만 하고, 판정은 UI 스레드 시간의 중앙값을 한 프레임의 절반(8ms) 아래로 잡는다.
 * 에뮬레이터는 같은 화면도 잴 때마다 꼬리 값이 크게 흔들려 95% 값은 기록만 한다.
 */
@RunWith(AndroidJUnit4::class)
class MotionDeviceTest {
    /** 부품마다 UI 스레드가 한 프레임의 절반 안에 끝난다. */
    @Test
    fun eachPartKeepsUiThreadUnderHalfFrame() {
        val parts: List<Pair<String, @Composable () -> Unit>> = listOf(
            "목소리 띠" to { MontageVoiceRibbon(speakingLevel()) },
            "모이는 점" to { MontageGatheringDots("질문을 불러오는 중") },
            "등급 눈금" to { Replay(1_500) { MontageGradeScale(grades, low = 4, high = 5, description = "IM~IH") } },
            "완료 체크" to { Replay(1_000) { MontageCheckMark(Modifier.size(96.dp)) } },
            "숫자" to { Replay(1_500) { MontageCountUp(77, style = MontageTheme.typography.display1.bold) } },
            "진행 점" to { MontageStepTrail(total = 15, done = ticks(500) % 16) },
        )
        val slow = parts.mapNotNull { (name, part) ->
            val ms = measure(name, 3_000) { Column(Modifier.padding(MontageSpacing.spacing16)) { part() } }
            name.takeIf { median(ms) >= UI_BUDGET_MILLIS }
        }
        assertTrue("UI 스레드 중앙값이 8ms 를 넘는 부품: $slow", slow.isEmpty())
    }

    /** 녹화용이다. 테스트가 도는 동안 `adb shell screenrecord` 로 찍는다. 한 번 움직이는 부품은 4초마다 처음부터 다시 움직인다. */
    @Test
    fun showAllPartsForRecording() {
        val ms = measure("함께", 12_000) {
            Column(
                Modifier.fillMaxSize().background(MontageTheme.colors.backgroundNeutralPrimary).systemBarsPadding().padding(MontageSpacing.spacing16),
                verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing16),
            ) {
                MontageVoiceRibbon(speakingLevel())
                MontageGatheringDots("질문을 불러오는 중")
                Replay(4_000) {
                    MontageGradeScale(grades, low = 4, high = 5, description = "IM~IH")
                    MontageCheckMark(Modifier.size(96.dp))
                    MontageCountUp(77, style = MontageTheme.typography.display1.bold)
                }
                MontageStepTrail(total = 15, done = ticks(1_000) % 16)
            }
        }
        assertTrue(ms.isNotEmpty())
    }

    /** 앱이 뒤로 가면 계속 도는 움직임의 시계가 멈춘다. */
    @Test
    fun frameClockStopsWhileInBackground() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var seconds: State<Float>
            scenario.onActivity { activity -> activity.setContent { MontageTheme { seconds = rememberFrameSeconds() } } }
            Thread.sleep(500)
            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(200)
            var stopped = 0f
            scenario.onActivity { stopped = seconds.value }
            Thread.sleep(1_000)
            var later = 0f
            scenario.onActivity { later = seconds.value }

            assertTrue("앞에 있을 때 시계가 흐르지 않았다", stopped > 0f)
            assertEquals("뒤에 있는 동안 시계가 흘렀다", stopped, later, 0.001f)
        }
    }

    /** [content] 를 [millis] 동안 띄우고 프레임마다 UI 스레드 시간(ms)을 모은다. 처음 10 프레임은 준비 시간이라 뺀다. */
    private fun measure(name: String, millis: Long, content: @Composable () -> Unit): List<Double> = ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
        val ms = mutableListOf<Double>()
        var total = 0
        scenario.onActivity { activity ->
            activity.window.addOnFrameMetricsAvailableListener(
                { _: Window, metrics: FrameMetrics, _: Int ->
                    ms += UI_THREAD.sumOf { metrics.getMetric(it) } / 1e6
                    if (metrics.getMetric(FrameMetrics.TOTAL_DURATION) > FRAME_NANOS) total++
                },
                Handler(Looper.getMainLooper()),
            )
            activity.setContent { MontageTheme(content = content) }
        }
        Thread.sleep(millis)
        scenario.onActivity { }
        val kept = ms.drop(10).sorted()
        Log.i(TAG, "$name frames=${kept.size} median=${median(kept)} p95=${kept[kept.size * 95 / 100]} total>16.7ms=$total")
        kept
    }

    private fun median(sorted: List<Double>) = sorted[sorted.size / 2]

    private companion object {
        const val TAG = "MotionDeviceTest"
        const val FRAME_NANOS = 16_700_000L
        const val UI_BUDGET_MILLIS = 8.0
        val UI_THREAD = listOf(
            FrameMetrics.INPUT_HANDLING_DURATION,
            FrameMetrics.ANIMATION_DURATION,
            FrameMetrics.LAYOUT_MEASURE_DURATION,
            FrameMetrics.DRAW_DURATION,
        )
    }
}

/** 말할 때처럼 커졌다 작아지는 음량. 녹음기처럼 1초에 12번 바뀐다. */
@Composable
private fun speakingLevel(): Float {
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var t = 0.0
        while (true) {
            level = abs(sin(t * 2.1) * sin(t * 0.7 + 1)).toFloat()
            delay(83)
            t += 0.083
        }
    }
    return level
}

/** [millis] 마다 1씩 오르는 수. */
@Composable
private fun ticks(millis: Long): Int {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millis)
            count++
        }
    }
    return count
}

/** [millis] 마다 [content] 를 처음부터 다시 그려 한 번 움직이는 부품을 되풀이한다. */
@Composable
private fun Replay(millis: Long, content: @Composable () -> Unit) {
    key(ticks(millis)) { content() }
}
