package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** 아이콘 24dp, 눌리는 영역 48dp 원형. 상단 바의 뒤로가기, 닫기 같은 곳에 쓴다. 입력칸 지우기처럼 덜 눈에 띄어야 하면 [tint] 를 옅게 준다. */
@Composable
fun MontageIconButton(
    icon: DrawableResource,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MontageTheme.colors.foregroundNeutralPrimary,
) {
    val colors = MontageTheme.colors
    // ponytail: Material 3 IconButton 이 48dp 영역, 원형 눌림 효과, 비활성 처리를 이미 한다. 색만 Montage 로 바꾼다.
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.foregroundDisablePrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}
