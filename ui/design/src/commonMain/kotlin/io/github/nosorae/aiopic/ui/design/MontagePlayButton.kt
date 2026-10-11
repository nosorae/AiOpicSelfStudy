package io.github.nosorae.aiopic.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val CIRCLE = 88.dp

/**
 * 문항 화면 가운데의 큰 Play. 강조색 원 하나와 그 아래 [text] 다. 누르면 원이 소리 물결([MontageGatheringDots])로 바뀌어
 * 질문을 고르거나 읽는 중([busy])임을 보인다. 그동안은 눌리지 않고 화면 읽기 도구에는 "처리 중" 으로 읽힌다.
 * 원과 물결은 같은 높이 안에서 바뀌어 아래 글자와 버튼이 움직이지 않는다.
 */
@Composable
fun MontagePlayButton(
    text: String,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: DrawableResource = Res.drawable.icon_normal_play,
) {
    val colors = MontageTheme.colors
    // 원과 글자 어디를 눌러도 된다. 눌림 물결은 원에만 그린다.
    val interaction = remember { MutableInteractionSource() }
    Column(
        // 너비를 상태와 상관없이 같게 둔다. 처리 중일 때만 넓어지면 Crossfade 가 원을 왼쪽에 그렸다가 가운데로 옮긴다(#201).
        modifier = modifier.fillMaxWidth().then(
            if (busy) {
                // ponytail: 앱이 한국어 하나라 문구를 여기 둔다. 언어를 더하면 리소스로 옮긴다.
                Modifier.clearAndSetSemantics { contentDescription = "처리 중" }
            } else {
                Modifier.clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            },
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
    ) {
        Crossfade(busy, Modifier.fillMaxWidth(), animationSpec = MontageMotion.spec(MontageMotion.MEDIUM_MILLIS), label = "play") { waiting ->
            if (waiting) {
                Box(Modifier.fillMaxWidth().height(CIRCLE).padding(horizontal = MontageSpacing.spacing20), contentAlignment = Alignment.Center) {
                    MontageGatheringDots("처리 중")
                }
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(CIRCLE)
                            .clip(CircleShape)
                            .background(if (enabled) colors.surfaceBrandPrimary else colors.surfaceDisablePrimary)
                            .indication(interaction, ripple(color = colors.onBrand)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(icon), contentDescription = null, tint = if (enabled) colors.onBrand else colors.foregroundDisablePrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
        // 물결일 때도 글자를 투명하게 남겨 원일 때와 높이가 같다.
        Text(
            text = text,
            style = MontageTheme.typography.headline1.bold,
            color = if (enabled) colors.foregroundBrandPrimary else colors.foregroundDisablePrimary,
            modifier = if (busy) Modifier.alpha(0f) else Modifier,
        )
    }
}
