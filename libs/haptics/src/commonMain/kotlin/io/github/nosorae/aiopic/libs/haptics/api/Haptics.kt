package io.github.nosorae.aiopic.libs.haptics.api

/**
 * 손에 느껴지는 짧은 진동 하나를 낸다. 화면은 이 함수만 부르고, 어느 플랫폼 진동을 쓸지는 앱 틀이 정한다.
 * 기기의 시스템 터치 진동 설정을 따르므로, 사용자가 시스템에서 꺼 두면 아무것도 느껴지지 않는다.
 */
public fun interface Haptics {
    public fun play(effect: HapticEffect)

    public companion object {
        /** 아무것도 하지 않는다. 미리보기, 스크린샷, 테스트의 기본값이다. */
        public val None: Haptics = Haptics { }
    }
}

/** 진동의 쓰임. 언제 무엇을 쓰는지는 README 의 대응표가 정한다. */
public enum class HapticEffect {
    /** 주요 버튼을 눌렀다. 모든 버튼에 쓰지 않는다. */
    Tap,

    /** 칩, 목록, 토글처럼 고른 것이 바뀌었다. */
    Selection,
    RecordStart,
    RecordStop,

    /** 키 저장처럼 사용자가 기다린 일이 됐다. */
    Success,

    /** 되돌릴 수 없는 일을 하기 전에 묻는다. */
    Warning,

    /** 오류 안내를 띄웠다. */
    Error,

    /** 모의시험을 마치고 등급을 처음 보인다. 회차에 한 번만 쓴다. */
    Celebrate,
}
