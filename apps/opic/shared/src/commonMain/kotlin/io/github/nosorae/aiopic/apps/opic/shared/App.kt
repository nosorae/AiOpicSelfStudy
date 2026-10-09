package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyEntryRoute

@Composable
fun App(graph: AppGraph, modifier: Modifier = Modifier) {
    MontageTheme {
        // ponytail: 화면이 키 입력 하나뿐이라 백스택 없이 바로 그린다. 앱 틀(u-shell)이 Navigation 3 로 바꾼다.
        val viewModel = viewModel { graph.keyEntryViewModelFactory.create(null) }
        KeyEntryRoute(viewModel = viewModel, onDone = {}, modifier = modifier)
    }
}
