package io.github.nosorae.aiopic.spikes.nativechrome.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Montage v4.0.0 (MIT) 라이트 시맨틱 토큰 몇 개. brand 는 우리 주홍(ADR 0018).
private object Montage {
    val foregroundNeutralPrimary = Color(0xFF171719)
    val foregroundNeutralTertiary = Color(0x9C37383C)
    val backgroundNeutralPrimary = Color(0xFFFFFFFF)
    val backgroundNeutralSecondary = Color(0xFFF7F7F8)
    val brand = Color(0xFFC83F1A)
    val title3Bold = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W700, lineHeight = 32.sp, letterSpacing = (-0.023).em)
    val headline1Bold = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W600, lineHeight = 26.sp, letterSpacing = (-0.002).em)
    val body1Medium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W500, lineHeight = 24.sp, letterSpacing = 0.0057.em)
    val caption1Medium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W500, lineHeight = 16.sp, letterSpacing = 0.0252.em)
}

// 탭 전환 뒤에도 같은 composition 인지 보려고 화면이 처음 만들어질 때마다 번호를 하나씩 올린다.
private var nextInstance = 1

/** 앱 틀이 부르는 진입점. 화면은 플랫폼도, 탭 바 높이도 모른다. 여백은 safeDrawing 하나만 본다. */
@Composable
fun ScreenContent(
    screen: Screen,
    onOpen: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (screen.chrome) {
        Chrome.TopLevel -> ListScreen(screen.title, onOpen = { onOpen(Screen.ReportDetail) }, modifier)
        Chrome.Sub -> DetailScreen(screen.title, "리포트 내용. 아래 버튼으로 시험 화면을 연다.", "시험 시작", { onOpen(Screen.Exam) }, modifier)
        Chrome.FullScreen -> DetailScreen(screen.title, "시험 진행 중. 탭 바가 없어야 한다.", null, {}, modifier)
    }
}

@Composable
fun ListScreen(
    title: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instance = remember { nextInstance++ }
    var taps by rememberSaveable { mutableIntStateOf(0) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Montage.backgroundNeutralPrimary),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = insets.calculateTopPadding() + 16.dp,
            bottom = insets.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(title, style = Montage.title3Bold, color = Montage.foregroundNeutralPrimary) }
        item {
            Text(
                "화면 #$instance, 누른 횟수 $taps",
                style = Montage.caption1Medium,
                color = Montage.foregroundNeutralTertiary,
                modifier = Modifier.clickable { taps++ },
            )
        }
        items((1..20).toList()) { n ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Montage.backgroundNeutralSecondary, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpen)
                    .padding(16.dp),
            ) {
                Text("리포트 $n", style = Montage.headline1Bold, color = Montage.foregroundNeutralPrimary)
                Text("모의시험 ${n}회차", style = Montage.body1Medium, color = Montage.foregroundNeutralTertiary)
            }
        }
    }
}

@Composable
fun DetailScreen(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Montage.backgroundNeutralPrimary)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = Montage.title3Bold, color = Montage.foregroundNeutralPrimary)
        Text(body, style = Montage.body1Medium, color = Montage.foregroundNeutralTertiary)
        if (action != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Montage.brand, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(16.dp),
            ) {
                Text(action, style = Montage.headline1Bold, color = Color.White)
            }
        }
    }
}
