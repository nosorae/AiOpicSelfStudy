package io.github.nosorae.aiopic.apps.opic.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabaseConstructor

actual fun inMemoryDatabase(): OpicDatabase = Room.inMemoryDatabaseBuilder<OpicDatabase>(factory = OpicDatabaseConstructor::initialize)
    .setDriver(BundledSQLiteDriver())
    .build()
