package io.github.nosorae.aiopic.apps.opic.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabaseConstructor
import kotlinx.coroutines.Dispatchers

actual fun inMemoryDatabase(): OpicDatabase = Room.inMemoryDatabaseBuilder<OpicDatabase>(factory = OpicDatabaseConstructor::initialize)
    .setDriver(BundledSQLiteDriver())
    // 질의를 부른 스레드에서 바로 돌린다. 질의가 다른 스레드에서 도는 동안 테스트 시계가 분석 시간 제한을 넘겨 버리지 않게 한다.
    .setQueryCoroutineContext(Dispatchers.Unconfined)
    .build()
