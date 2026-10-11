package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** 큰 축하는 회차에 한두 번 오는 순간(모의고사 완료, 등급 공개), 작은 축하는 기다린 일이 끝난 순간(키 저장, 답변 분석)이다. */
enum class MontageCelebrationSize { Large, Small }

/**
 * 축하할 일이 실제로 생겼을 때 한 번 터지는 조각. 크기는 [modifier] 로 정한 영역이고 조각은 그 가운데에서 터진다.
 * 큰 축하는 목소리 띠 같은 짧은 막대와 점 40개가 위로 터졌다가 떨어지며 사라진다(1.4초). 같은 때 축하 진동을 낸다.
 * 작은 축하는 불꽃 12개가 둥글게 한 번 퍼지고 사라진다(0.7초). 같은 때 성공 진동을 낸다.
 * 동작 줄이기면 조각을 그리지 않고 진동만 낸다. 끝 장면이 빈 영역이라서다. 결과는 늘 옆의 체크나 문구가 전한다. 이 그림은 꾸밈이다.
 * [playOnceKey] 를 주면 그 키로 한 번 터진 뒤에는 다시 그려도 아무것도 그리지 않고 진동도 내지 않는다. 키에는 무엇의 축하인지를 넣는다.
 * [delayMillis] 뒤에 터진다. 다른 움직임(등급 구슬, 체크 그리기)이 멈춘 뒤에 터지게 할 때 쓴다.
 */
@Composable
fun MontageCelebration(kind: MontageCelebrationSize, modifier: Modifier = Modifier, playOnceKey: String? = null, delayMillis: Int = 0) {
    val played = MontageMotion.played
    if (remember { playOnceKey != null && playOnceKey in played }) return
    val colors = MontageTheme.colors
    val haptics = LocalHaptics.current
    val reduceMotion = MontageMotion.reduceMotion
    val large = kind == MontageCelebrationSize.Large
    val spec = MontageMotion.spec<Float>(if (large) LARGE_MILLIS else SMALL_MILLIS)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        playOnceKey?.let(played::add)
        delay(delayMillis.toLong())
        haptics.play(if (large) HapticEffect.Celebrate else HapticEffect.Success)
        if (!reduceMotion) progress.animateTo(1f, spec)
    }
    if (reduceMotion) return
    val palette = listOf(colors.foregroundBrandPrimary, colors.brandDecoration, colors.foregroundCautionaryPrimary, colors.foregroundAccentLightblue, colors.foregroundAccentLime)
    val pieces = remember(large) { if (large) confetti() else sparks() }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        // 마지막 3할 동안 옅어진다.
        val alpha = ((1f - t) / 0.3f).coerceAtMost(1f)
        val reach = size.minDimension / 2
        pieces.forEach { p ->
            val color = if (p.color == 0) colors.foregroundBrandPrimary else palette[p.color % palette.size]
            if (large) {
                // 위로 터졌다가 무게로 떨어진다. 시간에 곡선이 이미 걸려 있어 처음엔 빠르고 끝엔 느리다.
                val at = Offset(
                    center.x + cos(p.angle) * p.speed * reach * t * 1.6f,
                    center.y + sin(p.angle) * p.speed * reach * t + reach * GRAVITY * t * t,
                )
                val long = reach * 0.07f * p.scale
                rotate(p.spin * t * 360f + p.angle * 57f, at) {
                    if (p.round) {
                        drawCircle(color, long * 0.4f, at, alpha)
                    } else {
                        drawLine(color, Offset(at.x, at.y - long / 2), Offset(at.x, at.y + long / 2), long * 0.36f, StrokeCap.Round, alpha = alpha)
                    }
                }
            } else {
                val from = reach * (0.45f + 0.35f * t)
                val to = from + reach * 0.18f * (1f - t) * p.scale
                val dir = Offset(cos(p.angle), sin(p.angle))
                drawLine(color, center + dir * from, center + dir * to, reach * 0.07f, StrokeCap.Round, alpha = alpha)
            }
        }
    }
}

/** 조각 하나. 방향과 빠르기, 크기, 돌기, 모양, 색 번호(0 은 늘 강조색)는 처음 한 번 정하고 그리는 동안은 시간만 바뀐다. */
private class Piece(val angle: Float, val speed: Float, val scale: Float, val spin: Float, val round: Boolean, val color: Int)

/** 위쪽 반원으로 퍼지는 조각 40개. 씨앗을 고정해 스크린샷이 늘 같다. 절반은 강조색이다. */
private fun confetti(): List<Piece> {
    val random = Random(SEED)
    return List(40) { i ->
        Piece(
            angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 0.9f).toFloat(),
            speed = 1.2f + random.nextFloat() * 1.2f,
            scale = 0.7f + random.nextFloat() * 0.6f,
            spin = random.nextFloat() * 2f - 1f,
            round = i % 3 == 0,
            color = if (i % 2 == 0) 0 else random.nextInt(1, 5),
        )
    }
}

/** 둥글게 고르게 퍼지는 불꽃 12개. 하나 걸러 길이와 색을 바꾼다. */
private fun sparks(): List<Piece> = List(12) { i ->
    Piece(angle = (2 * PI * i / 12 + 0.26).toFloat(), speed = 1f, scale = if (i % 2 == 0) 1f else 0.6f, spin = 0f, round = false, color = if (i % 2 == 0) 0 else 1)
}

private const val LARGE_MILLIS = 1_400
private const val SMALL_MILLIS = 700
private const val SEED = 158
private const val GRAVITY = 2.4f
