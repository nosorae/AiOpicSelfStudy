package io.github.nosorae.aiopic.apps.opic.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabaseConstructor
import kotlinx.coroutines.Dispatchers

/** 질의를 부른 스레드에서 바로 돌린다. 앱 틀 스크린샷이 DB 를 다 읽은 화면을 찍게 한다. */
actual fun inMemoryDatabase(): OpicDatabase = Room.inMemoryDatabaseBuilder<OpicDatabase>(factory = OpicDatabaseConstructor::initialize)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Unconfined)
    .build()
