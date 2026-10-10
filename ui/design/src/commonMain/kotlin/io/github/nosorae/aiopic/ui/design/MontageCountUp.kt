package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt

/**
 * 처음 보일 때 0 에서 [value] 까지 올라가는 숫자. 점수가 나왔다는 것을 글을 읽기 전에 보이게 한다.
 * 화면 읽기 프로그램과 테스트는 올라가는 중간 값 대신 [value] 를 바로 읽는다.
 */
@Composable
fun MontageCountUp(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    format: (Int) -> String = Int::toString,
) {
    val reduceMotion = MontageMotion.reduceMotion
    val spec = MontageMotion.spec<Float>(MontageMotion.LONG_MILLIS)
    val shown = remember { Animatable(if (reduceMotion) value.toFloat() else 0f) }
    LaunchedEffect(value) { shown.animateTo(value.toFloat(), spec) }
    Text(
        text = format(shown.value.roundToInt()),
        modifier = modifier.clearAndSetSemantics { text = AnnotatedString(format(value)) },
        style = style,
        color = color,
    )
}
