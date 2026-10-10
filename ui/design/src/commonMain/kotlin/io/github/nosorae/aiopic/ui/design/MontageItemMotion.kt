package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 목록 한 줄의 움직임. 화면을 보는 동안 새로 들어온 줄만 짧게(250ms) 나타나고, 밀려난 줄은 새 위치로 미끄러진다.
 * 목록이 처음 그려질 때 이미 있던 줄은 움직이지 않는다. 탭을 오가거나 화면을 다시 열 때 목록 전체가 다시 등장하면 무엇이 새로 왔는지 안 보여서다.
 * 불러오기 전 빈 목록이 처음 채워질 때처럼 새 줄로 보면 안 되는 때는 [appear] 를 false 로 준다. 동작 줄이기면 바로 끝 장면이다.
 */
@Composable
fun LazyItemScope.montageItemMotion(appear: Boolean = true): Modifier = Modifier.animateItem(
    fadeInSpec = if (appear) MontageMotion.spec() else null,
    placementSpec = MontageMotion.spec(),
    fadeOutSpec = null,
)
