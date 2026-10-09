package io.github.nosorae.aiopic.ui.featuresettings.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featuresettings.Res
import io.github.nosorae.aiopic.ui.featuresettings.app_info_about
import io.github.nosorae.aiopic.ui.featuresettings.app_info_design
import io.github.nosorae.aiopic.ui.featuresettings.app_info_font
import io.github.nosorae.aiopic.ui.featuresettings.app_info_title
import org.jetbrains.compose.resources.stringResource

/**
 * 앱 정보와 라이선스 고지(ADR 0018). 디자인 시스템과 글꼴 출처를 적고, 저장소의 `THIRD_PARTY_LICENSES.md` 를 그대로 싣는다.
 * 고정 문서라 ViewModel 을 두지 않고 리소스 파일을 바로 읽는다.
 */
@Composable
fun AppInfoRoute(modifier: Modifier = Modifier) {
    val licenses by produceState<String?>(null) { value = Res.readBytes(LICENSES_FILE).decodeToString() }
    AppInfoScreen(licenses, modifier)
}

/** 저장소 루트 파일의 사본이다. 둘이 같은지는 `LicenseFileTest` 가 본다. */
internal const val LICENSES_FILE = "files/THIRD_PARTY_LICENSES.md"

/** [licenses] 는 고지 문서 원문이다. 읽기 전에는 null 이고 출처 줄만 보인다. */
@Composable
fun AppInfoScreen(licenses: String?, modifier: Modifier = Modifier) {
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.app_info_title)) }) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        ) {
            listOf(Res.string.app_info_about, Res.string.app_info_design, Res.string.app_info_font).forEach {
                Text(stringResource(it), style = typography.body1Reading.regular, color = colors.foregroundNeutralPrimary)
            }
            licenses?.let { text ->
                licenseBlocks(text).forEach { block ->
                    when (block) {
                        is LicenseBlock.Heading -> Text(
                            text = block.text,
                            style = typography.headline1.bold,
                            color = colors.foregroundNeutralStrong,
                            modifier = Modifier.padding(top = MontageSpacing.spacing16).semantics { heading() },
                        )

                        is LicenseBlock.Body -> Text(block.text, style = typography.body2Reading.regular, color = colors.foregroundNeutralPrimary)

                        is LicenseBlock.Verbatim -> Text(block.text, style = typography.label2.regular, color = colors.foregroundNeutralSecondary)
                    }
                }
            }
        }
    }
}

internal sealed interface LicenseBlock {
    val text: String

    data class Heading(override val text: String) : LicenseBlock

    data class Body(override val text: String) : LicenseBlock

    /** 라이선스 전문. 원문 줄바꿈을 그대로 둔다. */
    data class Verbatim(override val text: String) : LicenseBlock
}

/**
 * 고지 문서의 마크다운을 화면 블록으로 나눈다. 이 문서가 쓰는 제목(`#`), 본문 줄, 코드 울타리(```)만 알아본다.
 * 울타리 안은 라이선스 원문이라 한 덩어리로 그대로 싣는다.
 */
// ponytail: 마크다운 라이브러리 없이 이 문서 하나에 쓰는 문법만 읽는다. 고지 문서가 표나 링크 문법을 쓰게 되면 그때 라이브러리를 넣는다.
internal fun licenseBlocks(markdown: String): List<LicenseBlock> {
    val blocks = mutableListOf<LicenseBlock>()
    var fence: MutableList<String>? = null
    markdown.lines().forEach { line ->
        val open = fence
        when {
            line.startsWith("```") && open == null -> fence = mutableListOf()

            line.startsWith("```") && open != null -> {
                blocks += LicenseBlock.Verbatim(open.joinToString("\n").trim())
                fence = null
            }

            open != null -> open += line

            line.startsWith("#") -> blocks += LicenseBlock.Heading(line.trimStart('#').trim())

            line.isNotBlank() -> blocks += LicenseBlock.Body(line.trim().replace("`", ""))
        }
    }
    return blocks
}

@Preview
@Composable
private fun AppInfoScreenPreview() {
    MontageTheme { AppInfoScreen(licenses = "# 제3자 라이선스\n\n본문\n\n```\nMIT License\n```") }
}
