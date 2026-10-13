package io.github.nosorae.aiopic.buildlogic

import java.io.File
import java.util.Properties

/** local.properties 의 키 이름이자 환경 변수 이름. 값은 서명 정보 파일의 경로다. */
const val RELEASE_SIGNING_KEY = "AIOPIC_RELEASE_SIGNING"

/**
 * 업로드 키 정보. 키와 비밀번호는 저장소 밖 파일 하나에 두고 저장소는 그 경로만 받는다.
 * 파일 형식은 `storeFile`, `storePassword`, `keyAlias`, `keyPassword` 네 줄이다. `storeFile` 이 상대 경로면 이 파일 옆을 본다.
 */
data class ReleaseSigning(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)

/** 서명 정보를 읽는다. 없거나 모자라면 서명 없이 빌드할 이유를 담은 실패를 돌려준다. */
fun readReleaseSigning(path: String?): Result<ReleaseSigning> = runCatching {
    check(!path.isNullOrBlank()) { "$RELEASE_SIGNING_KEY 가 local.properties 와 환경 변수 어디에도 없다" }
    val file = File(path)
    check(file.isFile) { "서명 정보 파일 $path 가 없다" }
    val props = Properties().apply { file.inputStream().use(::load) }
    val fields = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    val missing = fields.filter { props.getProperty(it).isNullOrBlank() }
    check(missing.isEmpty()) { "$path 에 ${missing.joinToString()} 이 없다" }
    val store = file.absoluteFile.parentFile.resolve(props.getProperty("storeFile"))
    check(store.isFile) { "키 저장소 $store 가 없다" }
    ReleaseSigning(store, props.getProperty("storePassword"), props.getProperty("keyAlias"), props.getProperty("keyPassword"))
}
