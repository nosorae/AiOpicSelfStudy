package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.ui.window.ComposeUIViewController

// iOS 쪽 Swift 코드가 이 이름으로 부르므로 함수 이름 규칙에서 뺀다.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = createAppGraph().let { graph -> ComposeUIViewController { App(graph) } }
