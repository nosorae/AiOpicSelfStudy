package io.github.nosorae.aiopic.apps.opic.shared.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.nosorae.aiopic.engine.answer.store.OPIC_MIGRATIONS
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import java.io.File

/** 앱 데이터 폴더의 `opic.db`. 자동 백업에 실린다(기록은 기기를 바꿔도 남긴다). */
fun opicDatabase(context: Context): OpicDatabase = Room.databaseBuilder<OpicDatabase>(context.applicationContext, context.getDatabasePath("opic.db").path)
    .setDriver(BundledSQLiteDriver())
    .addMigrations(*OPIC_MIGRATIONS)
    .build()

/** 녹음 폴더 `filesDir/recordings`. 백업 규칙(res/xml 의 backup_rules, data_extraction_rules)이 이 경로를 뺀다. */
fun recordingsRoot(context: Context): String = File(context.filesDir, "recordings").path
