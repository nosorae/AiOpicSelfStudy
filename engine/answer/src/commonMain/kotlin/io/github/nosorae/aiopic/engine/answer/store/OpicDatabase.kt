package io.github.nosorae.aiopic.engine.answer.store

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import kotlinx.coroutines.flow.Flow

/**
 * 회차 진행 상태, 답변, 틀림 표시를 기기에 저장하는 DB. 앱이 플랫폼 빌더(파일 경로, BundledSQLiteDriver)로 만들어 [OpicStore] 에 넘긴다.
 *
 * 행에는 찾을 때 쓰는 열만 두고 모델은 JSON 한 칸에 통째로 넣는다. 모델은 이미 직렬화 왕복이 테스트돼 있고(F3.1 통째 저장),
 * 필드가 늘어도 표를 고치지 않는다.
 */
// ponytail: exportSchema 를 끈다. 표가 JSON 칸 하나라 바뀔 일이 드물다. 표를 바꾸는 이전이 생기면 room3 Gradle 플러그인으로 스키마를 내보낸다.
// 틀림 표시 표를 더하면서도 version 1 로 둔다. 앱이 아직 이 DB 를 만든 적이 없어(배포 전) 이전할 파일이 없다.
@Database(entities = [SessionRow::class, AnswerRow::class, FlagRow::class], version = 1, exportSchema = false)
@ConstructedBy(OpicDatabaseConstructor::class)
abstract class OpicDatabase : RoomDatabase() {
    internal abstract fun dao(): OpicDao
}

@Suppress("KotlinNoActualForExpect")
expect object OpicDatabaseConstructor : RoomDatabaseConstructor<OpicDatabase> {
    override fun initialize(): OpicDatabase
}

@Entity(tableName = "exam_session")
internal data class SessionRow(@PrimaryKey val id: String, val json: String)

@Entity(tableName = "answer", indices = [Index("examSessionId")])
internal data class AnswerRow(@PrimaryKey val id: String, val examSessionId: String?, val json: String)

/** 틀림 표시 하나(F8.1). 답변 id 와 교정 순서만 둔다. 교정 내용은 답변 JSON 에 있다. */
@Entity(tableName = "correction_flag", primaryKeys = ["answerId", "index"])
internal data class FlagRow(val answerId: String, val index: Int)

@Dao
internal interface OpicDao {
    // @Upsert 는 충돌 예외의 메시지로 갈래를 정해서, 메시지가 비는 호스트 테스트 환경에서 깨진다. SQL 한 줄로 덮어쓴다.
    @Query("INSERT INTO exam_session (id, json) VALUES (:id, :json) ON CONFLICT(id) DO UPDATE SET json = excluded.json")
    suspend fun saveSession(id: String, json: String)

    @Query("SELECT * FROM exam_session WHERE id = :id")
    fun session(id: String): Flow<SessionRow?>

    @Query("SELECT * FROM exam_session ORDER BY rowid")
    fun sessions(): Flow<List<SessionRow>>

    // 같은 행을 고치므로(REPLACE 처럼 지웠다 넣지 않으므로) rowid 순서가 처음 저장한 순서다.
    @Query(
        "INSERT INTO answer (id, examSessionId, json) VALUES (:id, :examSessionId, :json) " +
            "ON CONFLICT(id) DO UPDATE SET examSessionId = excluded.examSessionId, json = excluded.json",
    )
    suspend fun saveAnswer(id: String, examSessionId: String?, json: String)

    @Query("SELECT * FROM answer WHERE examSessionId = :examSessionId ORDER BY rowid")
    fun answers(examSessionId: String): Flow<List<AnswerRow>>

    @Query("SELECT * FROM answer ORDER BY rowid")
    fun answers(): Flow<List<AnswerRow>>

    @Query("INSERT OR IGNORE INTO correction_flag (answerId, `index`) VALUES (:answerId, :index)")
    suspend fun addFlag(answerId: String, index: Int)

    @Query("DELETE FROM correction_flag WHERE answerId = :answerId AND `index` = :index")
    suspend fun removeFlag(answerId: String, index: Int)

    @Query("SELECT * FROM correction_flag")
    fun flags(): Flow<List<FlagRow>>
}
