package io.github.nosorae.aiopic.ui.design

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 글자 너비를 실제로 재야 해서 NATIVE 그리기로 돈다. 대화상자 버튼은 같은 너비로 나란히 놓이고, 글자가 커져 한 칸에 안 들어가면 확인을 위로 세로로 쌓인다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MontageAlertLayoutTest {
    @get:Rule val rule = createComposeRule()

    private fun show(cancel: String = "취소", confirm: String = "나가기") = rule.setContent {
        MontageTheme { MontageAlertContent("본문", confirm, {}, title = "제목", cancel = cancel) }
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun `짧은 버튼은 같은 너비로 나란히 놓인다`() {
        show()
        val cancel = bounds("alert-cancel")
        val confirm = bounds("alert-confirm")
        assertEquals(cancel.top, confirm.top)
        assertEquals(cancel.right - cancel.left, confirm.right - confirm.left)
        assertTrue(cancel.right < confirm.left)
    }

    @Test
    fun `버튼 글자가 반 칸에 안 들어가면 확인이 위로 오게 세로로 쌓는다`() {
        show(confirm = "이 기기에 저장된 녹음을 모두 지우기")
        val cancel = bounds("alert-cancel")
        val confirm = bounds("alert-confirm")
        assertTrue(confirm.bottom <= cancel.top)
        assertEquals(cancel.right - cancel.left, confirm.right - confirm.left)
    }

    @Config(fontScale = 2f)
    @Test
    fun `글자 200% 에서도 버튼 글자가 잘리면 세로로 쌓는다`() {
        show(cancel = "계속 진행하기", confirm = "모의고사 나가기")
        assertTrue(bounds("alert-confirm").bottom <= bounds("alert-cancel").top)
    }
}
