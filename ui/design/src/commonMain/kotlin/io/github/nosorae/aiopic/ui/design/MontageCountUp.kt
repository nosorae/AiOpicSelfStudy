package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * 처음 보일 때 숫자 하나하나가 0 에서 굴러 올라가 [value] 에서 멈추는 숫자. 점수가 나왔다는 것을 글을 읽기 전에 보이게 한다.
 * 0~9 숫자 띠는 한 번만 배치하고 움직이는 동안에는 띠의 위치만 바꿔서, 매 프레임 글자를 다시 재지 않는다.
 * 화면 읽기 프로그램과 테스트는 굴러가는 중간 값 대신 [value] 를 바로 읽는다. 동작 줄이기면 처음부터 [value] 다.
 */
@Composable
fun MontageCountUp(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val digits = value.toString()
    Row(modifier.clearAndSetSemantics { text = AnnotatedString(digits) }) {
        digits.forEach { char ->
            if (char.isDigit()) DigitRoller(char.digitToInt(), style, color) else Text(char.toString(), style = style, color = color)
        }
    }
}

/** 숫자 한 글자. 보이는 크기는 [digit] 한 글자 크기이고, 그 안에서 0~9 띠가 미끄러진다. */
@Composable
private fun DigitRoller(digit: Int, style: TextStyle, color: Color) {
    val spec = MontageMotion.settle<Float>()
    val reduceMotion = MontageMotion.reduceMotion
    val rolled = remember { Animatable(if (reduceMotion) digit.toFloat() else 0f) }
    LaunchedEffect(digit) { rolled.animateTo(digit.toFloat(), spec) }
    Layout(
        content = {
            Text(digit.toString(), style = style, color = Color.Transparent)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                (0..9).forEach { key(it) { Text(it.toString(), style = style, color = color) } }
            }
        },
        modifier = Modifier.clipToBounds(),
    ) { measurables, constraints ->
        val sizer = measurables[0].measure(constraints)
        val strip = measurables[1].measure(Constraints())
        val step = strip.height / 10f
        layout(sizer.width, sizer.height) {
            sizer.place(0, 0)
            strip.placeWithLayer((sizer.width - strip.width) / 2, 0) { translationY = -(rolled.value * step).roundToInt().toFloat() }
        }
    }
}
