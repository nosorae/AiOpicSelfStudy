package io.github.nosorae.aiopic.apps.opic.shared.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AndroidDataTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `녹음 폴더는 filesDir 의 recordings 아래다`() {
        // 백업 규칙(res/xml)이 이 폴더를 뺀다. 경로가 바뀌면 녹음이 백업에 실린다.
        assertEquals(File(context.filesDir, "recordings").path, recordingsRoot(context))
    }

    @Test
    fun `F3_1 파일 DB 에 저장한 회차를 DB 를 다시 열어도 읽는다`() = runTest {
        val session = ExamSession("s1", level = 2)
        opicDatabase(context).run {
            OpicStore(this).save(session)
            close()
        }

        val db = opicDatabase(context)
        assertEquals(session, OpicStore(db).session("s1").first())
        db.close()
    }
}
