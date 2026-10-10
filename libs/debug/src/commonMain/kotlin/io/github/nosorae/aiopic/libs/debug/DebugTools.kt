package io.github.nosorae.aiopic.libs.debug

import kotlin.time.Clock

/**
 * 앱 어디서나 같은 기록에 쓰는 디버깅 도구 입구. 로그와 네트워크 기록이 한 곳에 모여야 해서 전역 object 하나로 둔다.
 * 릴리스 빌드는 공개 API 가 같은 `libs/debug-noop` 으로 바꿔 끼워 아무 일도 하지 않는다.
 * 플랫폼마다 `install` 을 부르기 전에는 메모리에만 기록하고 스위치는 기본값을 쓴다.
 */
public object DebugTools {
    internal val core: DebugCore = DebugCore(Clock.System)

    /** 메시지와 예외는 저장하기 전에 키를 가린다. */
    public fun log(level: DebugLevel, tag: String, message: String, error: Throwable? = null) {
        core.log(level, tag, message, error)
    }

    /**
     * 앱이 보여 줄 상태(대기열, 오디오 세션 등)를 "이름, 값" 줄로 더한다. 같은 제목이면 바꾼다.
     * 화면 대신 줄을 받아서 릴리스의 빈 모듈에 앱 패널 코드가 남지 않는다.
     */
    public fun panel(title: String, rows: suspend () -> List<Pair<String, String>>) {
        core.panel(title, rows)
    }

    /** 기기에 저장되는 켜기/끄기 스위치. 같은 key 는 같은 스위치를 돌려준다. */
    public fun switch(key: String, title: String, default: Boolean = false): DebugSwitch = core.switch(key, title, default)

    /** 디버깅 화면을 앱 위에 띄운다. 흔들기와 알림 말고 앱의 설정 화면 줄이나 테스트에서 연다. `install` 전에는 아무 일도 안 한다. */
    public fun open() {
        core.open()
    }
}

public enum class DebugLevel { Debug, Info, Warn, Error }

/** [enabled] 는 읽을 때마다 저장된 값을 본다. 그래서 앱을 다시 시작하지 않아도 바뀐 값이 바로 적용된다. */
public class DebugSwitch internal constructor(
    private val core: DebugCore,
    internal val key: String,
    internal val title: String,
    internal val default: Boolean,
) {
    public val enabled: Boolean get() = core.switchEnabled(key, default)
}

/**
 * @param redactPatterns 앱이 더 가릴 값의 패턴. Google API 키 형식과 URL 의 `key`, `api_key`, `access_token` 값은 늘 가린다.
 * @param maxLogs 메모리에 두는 로그 줄 수. 넘으면 오래된 것부터 버린다.
 * @param redactHeaders 앱이 더 가릴 헤더 이름(대소문자 무시). `x-goog-api-key`, `authorization`, `proxy-authorization`,
 *   `cookie`, `set-cookie`, `x-api-key` 는 늘 가린다.
 * @param maxCalls 메모리에 두는 네트워크 호출 수. 넘으면 오래된 것부터 버린다.
 * @param maxBodyBytes 요청, 응답 본문 하나를 남기는 크기. 녹음이 base64 로 들어간 분석 요청은 수 MB 라서 자르지 않으면
 *   호출 200건이 수백 MB 가 된다. 자른 본문 끝에는 원래 크기를 적는다.
 */
public class DebugConfig(
    public val redactPatterns: List<Regex> = emptyList(),
    public val maxLogs: Int = 2_000,
    public val redactHeaders: Set<String> = emptySet(),
    public val maxCalls: Int = 200,
    public val maxBodyBytes: Int = 16 * 1024,
)
