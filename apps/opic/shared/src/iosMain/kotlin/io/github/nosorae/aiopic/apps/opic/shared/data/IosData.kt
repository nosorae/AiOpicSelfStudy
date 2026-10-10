package io.github.nosorae.aiopic.apps.opic.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.nosorae.aiopic.engine.answer.store.OpicDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/** 앱 지원 폴더의 `opic.db`. iCloud 백업에 실린다(기록은 기기를 바꿔도 남긴다). */
fun opicDatabase(): OpicDatabase = Room.databaseBuilder<OpicDatabase>(name = "${applicationSupport()}/opic.db")
    .setDriver(BundledSQLiteDriver())
    .build()

/**
 * 녹음 폴더 `Application Support/recordings`. 폴더를 만들고 백업 제외를 건다. 녹음이 커지면 iCloud 백업 용량을 채우고,
 * 녹음은 기기에만 둔다(개인정보). 폴더에 걸면 안의 파일도 빠진다.
 */
@OptIn(ExperimentalForeignApi::class)
fun recordingsRoot(): String {
    val root = "${applicationSupport()}/recordings"
    NSFileManager.defaultManager.createDirectoryAtPath(root, withIntermediateDirectories = true, attributes = null, error = null)
    NSURL.fileURLWithPath(root).setResourceValue(true, NSURLIsExcludedFromBackupKey, null)
    return root
}

/** 앱 지원 폴더를 통째로 지운다(DB, 녹음). UI 테스트의 첫 실행 초기화만 쓴다. */
@OptIn(ExperimentalForeignApi::class)
internal fun deleteAppData() {
    NSFileManager.defaultManager.removeItemAtPath(applicationSupport(), error = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupport(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, create = true, error = null)
    return checkNotNull(url?.path) { "앱 지원 폴더를 못 연다" }
}
