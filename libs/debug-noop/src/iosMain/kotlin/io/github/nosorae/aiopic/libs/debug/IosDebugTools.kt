package io.github.nosorae.aiopic.libs.debug

import platform.UIKit.UIViewController

public fun DebugTools.install(config: DebugConfig = DebugConfig()) {}

/** 빈 화면. 릴리스 빌드에서는 앱이 이 화면을 띄울 길(흔들기, 설정 줄)을 열지 않는다. */
public fun DebugTools.viewController(): UIViewController = UIViewController()
