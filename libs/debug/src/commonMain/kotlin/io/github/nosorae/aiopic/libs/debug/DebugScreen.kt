package io.github.nosorae.aiopic.libs.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class DebugTab { Logs, Network, Switches, Panels, Info, Crashes }

private val mono = FontFamily.Monospace

/**
 * 디버깅 도구 화면. 앱 디자인 체계와 묶이지 않게 Material 기본 테마만 쓴다.
 * 탭은 로그, 네트워크, 스위치, 앱 패널, 기기와 앱 정보, 크래시이고, 공유는 [DebugCore.report] 전체를 [onShare] 로 넘긴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugScreen(core: DebugCore, onShare: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(DebugTab.Logs) }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Debug tools") },
                    actions = {
                        TextButton(onClick = { scope.launch { onShare(core.report()) } }) { Text("Share") }
                        TextButton(onClick = onClose) { Text("Close") }
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
                    DebugTab.entries.forEach {
                        Tab(selected = it == tab, onClick = { tab = it }, text = { Text(it.name) })
                    }
                }
            }
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when (tab) {
            DebugTab.Logs -> LogsTab(core, content)
            DebugTab.Network -> NetworkTab(core, content)
            DebugTab.Switches -> SwitchesTab(core, content)
            DebugTab.Panels -> PanelsTab(core, content)
            DebugTab.Info -> Rows(remember { core.info() }, content)
            DebugTab.Crashes -> CrashesTab(core, content)
        }
    }
}

@Composable
private fun LogsTab(core: DebugCore, modifier: Modifier = Modifier) {
    val logs by core.logs.collectAsState()
    var query by remember { mutableStateOf("") }
    // null 은 전체. 고른 단계와 그보다 높은 단계만 보여 준다.
    var minLevel by remember { mutableStateOf<DebugLevel?>(null) }
    val min = minLevel
    val shown = logs.asReversed().filter { log ->
        (min == null || log.level >= min) &&
            (query.isBlank() || listOf(log.tag, log.message, log.error.orEmpty()).any { it.contains(query, ignoreCase = true) })
    }
    Column(modifier) {
        SearchField(query, { query = it })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = minLevel == null, onClick = { minLevel = null }, label = { Text("All") })
            DebugLevel.entries.forEach { level ->
                FilterChip(selected = minLevel == level, onClick = { minLevel = level }, label = { Text(level.name) })
            }
        }
        // 예외는 첫 줄만 보이고 줄을 누르면 스택 전체가 펼쳐진다. 스택이 목록을 다 덮지 않게 한다.
        var opened by remember { mutableStateOf<DebugLog?>(null) }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(shown) { log ->
                val error = log.error?.let { if (log == opened) it else it.lineSequence().first() + " (tap for stack)" }
                Text(
                    "${log.time.toString().substring(11, 19)} ${log.level.name.first()} ${log.tag}: ${log.message}" +
                        error?.let { "\n  $it" }.orEmpty(),
                    color = levelColor(log.level),
                    fontFamily = mono,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clickable { opened = if (opened == log) null else log }.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun levelColor(level: DebugLevel): Color = when (level) {
    DebugLevel.Debug -> MaterialTheme.colorScheme.onSurfaceVariant
    DebugLevel.Info -> MaterialTheme.colorScheme.onSurface
    DebugLevel.Warn -> Color(0xFFB26A00)
    DebugLevel.Error -> MaterialTheme.colorScheme.error
}

@Composable
private fun NetworkTab(core: DebugCore, modifier: Modifier = Modifier) {
    val calls by core.calls.collectAsState()
    var query by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf<DebugCall?>(null) }

    // ponytail: 새 클립보드 API 는 넣는 값을 플랫폼마다 따로 만들어야 해서 예전 API 를 쓴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    Column(modifier) {
        SearchField(query, { query = it })
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(calls.asReversed().filter { query.isBlank() || it.url.contains(query, ignoreCase = true) }) { call ->
                val failed = call.status == null || call.status >= 400
                Column(Modifier.fillMaxWidth().clickable { opened = if (opened == call) null else call }.padding(vertical = 6.dp)) {
                    Text(
                        "${call.method} ${call.status ?: "ERR"} ${call.duration.inWholeMilliseconds}ms",
                        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(call.url, fontFamily = mono, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (opened == call) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(call.curl())) }) { Text("Copy cURL") }
                        SelectionContainer {
                            Text(callDetail(call), fontFamily = mono, fontSize = 11.sp)
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun callDetail(call: DebugCall): String = buildString {
    call.error?.let { append("Error: $it\n\n") }
    append("Request headers\n")
    call.requestHeaders.forEach { (name, value) -> append("$name: $value\n") }
    append("\nRequest body\n${call.requestBody.ifEmpty { "(none)" }}\n\nResponse headers\n")
    call.responseHeaders.forEach { (name, value) -> append("$name: $value\n") }
    append("\nResponse body\n${call.responseBody.ifEmpty { "(none)" }}")
}

/** 셸에 그대로 붙여 다시 보낼 수 있게 모든 값을 작은따옴표로 감싼다. 가린 헤더는 `***` 그대로라 키를 다시 넣어야 한다. */
internal fun DebugCall.curl(): String = buildString {
    fun quote(text: String) = "'" + text.replace("'", "'\\''") + "'"
    append("curl -X $method ${quote(url)}")
    requestHeaders.forEach { (name, value) -> append(" -H ${quote("$name: $value")}") }
    if (requestBody.isNotEmpty()) append(" --data-raw ${quote(requestBody)}")
}

@Composable
private fun SwitchesTab(core: DebugCore, modifier: Modifier = Modifier) {
    val switches by core.switches.collectAsState()
    // 스위치 값은 기기 저장소에 있어 상태로 관찰되지 않는다. 바꿀 때마다 숫자를 올려 다시 읽는다.
    var version by remember { mutableIntStateOf(0) }
    LazyColumn(modifier, contentPadding = PaddingValues(12.dp)) {
        items(switches.values.toList()) { switch ->
            val enabled = remember(version, switch) { switch.enabled }
            Row(
                Modifier.fillMaxWidth().clickable {
                    core.setSwitch(switch.key, !enabled)
                    version++
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(switch.title)
                    Text(switch.key, fontFamily = mono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 줄 전체가 눌리므로 스위치 자체는 보여 주기만 한다.
                Switch(checked = enabled, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun PanelsTab(core: DebugCore, modifier: Modifier = Modifier) {
    val panels by core.panels.collectAsState()
    var version by remember { mutableIntStateOf(0) }
    val rows by produceState(emptyList<Pair<String, String>>(), panels, version) {
        value = core.panelRows()
    }
    Column(modifier) {
        TextButton(onClick = { version++ }, modifier = Modifier.padding(horizontal = 4.dp)) { Text("Reload") }
        Rows(rows)
    }
}

@Composable
private fun CrashesTab(core: DebugCore, modifier: Modifier = Modifier) {
    var crashes by remember { mutableStateOf(core.crashes()) }
    LazyColumn(modifier, contentPadding = PaddingValues(12.dp)) {
        if (crashes.isEmpty()) item { Text("No crashes") }
        items(crashes) { (name, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    core.deleteCrash(name)
                    crashes = core.crashes()
                }) { Text("Delete") }
            }
            SelectionContainer { Text(text, fontFamily = mono, fontSize = 11.sp) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
    }
}

/** "이름: 값" 줄 목록. 이름이 `## ` 로 시작하면 소제목으로 그린다. */
@Composable
private fun Rows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(12.dp)) {
        items(rows) { (name, value) ->
            if (name.startsWith("## ")) {
                Text(name.removePrefix("## "), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            } else {
                SelectionContainer { Text("$name: $value", fontFamily = mono, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text("Search") },
        singleLine = true,
        modifier = modifier.fillMaxWidth().padding(12.dp),
    )
}

/** 플랫폼 창에 띄우는 입구. 시스템 다크 모드를 따른다. */
@Composable
internal fun DebugApp(core: DebugCore, onShare: (String) -> Unit, onClose: () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        DebugScreen(core, onShare, onClose)
    }
}
