package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 컴포넌트 스크린샷 테스트가 그리는 견본. 화면 문구가 아니라 모양 확인용이다.

@Composable
internal fun ButtonSamples() = Samples {
    MontageButtonSize.entries.forEach { size ->
        Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
            MontageButton("녹음 시작", {}, size = size, leadingIcon = Res.drawable.icon_normal_microphone)
            MontageButton("다시 듣기", {}, size = size, type = MontageButtonType.Assistive)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageButton("기록 지우기", {}, type = MontageButtonType.Negative)
        MontageButton("보내기", {}, enabled = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageButton("문항 고르기", {}, variant = MontageButtonVariant.Outlined)
        MontageButton("건너뛰기", {}, variant = MontageButtonVariant.Outlined, type = MontageButtonType.Assistive)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageButton("확인", {}, variant = MontageButtonVariant.Text)
        MontageButton("취소", {}, variant = MontageButtonVariant.Text, type = MontageButtonType.Assistive)
    }
}

@Composable
internal fun IconButtonSamples() = Samples {
    Row {
        MontageIconButton(Res.drawable.icon_normal_close, "닫기", {})
        MontageIconButton(Res.drawable.icon_normal_setting, "설정", {})
        MontageIconButton(Res.drawable.icon_normal_play, "재생", {}, enabled = false)
    }
}

@Composable
internal fun ChipSamples() = Samples {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageChip("영화 보기", {}, active = true)
        MontageChip("공원 가기", {})
        MontageChip("요리", {}, enabled = false)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageChip("음악 감상", {}, active = true, variant = MontageChipVariant.Outlined)
        MontageChip("조깅", {}, variant = MontageChipVariant.Outlined)
    }
}

@Composable
internal fun TextFieldSamples() = Samples {
    MontageTextField("", {}, title = "API 키", placeholder = "키를 붙여 넣으세요", description = "기기 안에만 저장합니다")
    MontageTextField("abc", {}, title = "API 키", description = "키 형식이 맞지 않습니다", error = true)
    MontageTextField("읽기 전용", {}, enabled = false)
}

@Composable
internal fun ProgressBarSamples() = Samples {
    MontageProgressBar(0f)
    MontageProgressBar(0.4f)
    MontageProgressBar(1f)
}

@Composable
internal fun TopBarSamples() = Column {
    MontageTopBar("기록")
    MontageTopBar(
        title = "문항 7 / 15",
        actions = { MontageIconButton(Res.drawable.icon_normal_close, "나가기", {}) },
    )
}

@Composable
internal fun AlertSamples() = Samples {
    MontageAlertContent(
        "진행과 녹음은 저장되고 나가 있는 동안 40분 타이머가 멈춥니다. 모의고사 탭의 이어 보기로 여기서부터 이어서 할 수 있습니다.",
        "나가기",
        {},
        title = "모의고사을 나갈까요?",
        cancel = "취소",
        negative = true,
    )
    MontageAlertContent("마이크 권한이 있어야 답변을 녹음할 수 있습니다.", "설정 열기", {})
}

@Composable
internal fun ListCellSamples() = Column {
    MontageListCell("화면 모드", caption = "시스템 따라가기", chevron = true, onClick = {})
    MontageListCell("라이트", selected = true, onClick = {})
    MontageListCell("다크", onClick = {})
    MontageListCell("음성 전송 동의", caption = "동의하지 않음", enabled = false)
}

@Composable
internal fun SelectionControlSamples() = Samples {
    Row {
        MontageCheckbox(checked = true, onCheckedChange = {})
        MontageCheckbox(checked = false, onCheckedChange = {})
        MontageCheckbox(checked = true, onCheckedChange = {}, enabled = false)
        MontageRadio(selected = true, onClick = {})
        MontageRadio(selected = false, onClick = {})
        MontageRadio(selected = false, onClick = {}, enabled = false)
    }
}

@Composable
private fun Samples(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(MontageSpacing.spacing20),
        verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
        content = content,
    )
}
