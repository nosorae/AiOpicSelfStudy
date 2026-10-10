package io.github.nosorae.aiopic.libs.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import kotlin.time.Instant

private enum class DebugTab { Logs, Network, Switches, Panels, Info, Crashes }

/**
 * 디버깅 화면. 앱 디자인 체계와 묶이지 않게 이 모듈의 토큰([DebugTheme])으로 그린다.
 * 탭은 로그, 네트워크, 스위치, 앱 패널, 기기와 앱 정보, 크래시이고, 공유는 [DebugCore.report] 전체를 [onShare] 로 넘긴다.
 * 직접 만든 움직임은 없다. Material 부품의 짧은 전환(탭 밑줄, 스위치)과 기다리는 동안의 막대뿐이다.
 * ponytail: iOS 동작 줄이기를 따로 읽지 않는다. 디버깅 화면에 계속 움직이는 것이 생기면 그때 읽는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugScreen(core: DebugCore, onShare: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(DebugTab.Logs) }
    var sharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Debug tools", style = DebugType.title) },
                    actions = {
                        // 보고서는 앱 패널을 다시 읽어서 만들어 시간이 걸릴 수 있다. 그동안 버튼을 막고 막대로 보인다.
                        TextButton(
                            enabled = !sharing,
                            onClick = {
                                sharing = true
                                scope.launch {
                                    try {
                                        onShare(core.report())
                                    } finally {
                                        sharing = false
                                    }
                                }
                            },
                        ) { Text(if (sharing) "Preparing report" else "Share") }
                        TextButton(onClick = onClose) { Text("Close") }
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = DebugSpace.s) {
                    DebugTab.entries.forEach {
                        Tab(selected = it == tab, onClick = { tab = it }, text = { Text(it.name) })
                    }
                }
                if (sharing) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when (tab) {
            DebugTab.Logs -> LogsTab(core, content)
            DebugTab.Network -> NetworkTab(core, content)
            DebugTab.Switches -> SwitchesTab(core, content)
            DebugTab.Panels -> PanelsTab(core, content)
            DebugTab.Info -> Rows(remember { core.info() }, content) { Empty("No device info", "The platform did not report any.") }
            DebugTab.Crashes -> CrashesTab(core, content)
        }
    }
}

// ---------- 로그 ----------

@Composable
private fun LogsTab(core: DebugCore, modifier: Modifier = Modifier) {
    val logs by core.logs.collectAsState()
    var query by remember { mutableStateOf("") }
    // null 은 전체. 고른 단계와 그보다 높은 단계만 보여 준다.
    var minLevel by remember { mutableStateOf<DebugLevel?>(null) }
    // 켜져 있으면 새 줄이 들어올 때 맨 아래로 내려간다. 끄면 읽던 곳에 머문다.
    var follow by remember { mutableStateOf(true) }
    val min = minLevel
    val shown = logs.filter { log ->
        (min == null || log.level >= min) &&
            (query.isBlank() || listOf(log.tag, log.message, log.error.orEmpty()).any { it.contains(query, ignoreCase = true) })
    }
    // 예외는 첫 줄만 보이고 줄을 누르면 스택 전체가 펼쳐진다. 스택이 목록을 다 덮지 않게 한다.
    var opened by remember { mutableStateOf<DebugLog?>(null) }
    val list = rememberLazyListState()
    LaunchedEffect(follow, shown.size, shown.lastOrNull()) {
        // 움직임 없이 바로 옮긴다. 새 줄이 빠르게 들어올 때 미끄러지는 동안 화면이 흔들리지 않는다.
        if (follow && shown.isNotEmpty()) list.scrollToItem(shown.lastIndex)
    }
    Column(modifier) {
        SearchField(query, { query = it }, placeholder = "Search tag, message, exception")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = DebugSpace.l),
            horizontalArrangement = Arrangement.spacedBy(DebugSpace.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(selected = minLevel == null, onClick = { minLevel = null }, label = { Text("All") })
            DebugLevel.entries.forEach { level ->
                FilterChip(selected = minLevel == level, onClick = { minLevel = level }, label = { Text(level.name) })
            }
        }
        // 몇 줄이 걸러졌는지와 따라가기를 목록 바로 위에 둔다. 따라가기는 목록을 움직이는 손잡이라 목록 가까이 있어야 한다.
        Row(
            Modifier.fillMaxWidth().padding(start = DebugSpace.l, end = DebugSpace.l, top = DebugSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (shown.size == logs.size) "${logs.size} lines" else "${shown.size} of ${logs.size} lines",
                style = DebugType.caption,
                color = debugColors.muted,
                modifier = Modifier.weight(1f),
            )
            FilterChip(selected = follow, onClick = { follow = !follow }, label = { Text("Follow") })
        }
        HorizontalDivider(color = debugColors.line)
        if (shown.isEmpty()) {
            if (logs.isEmpty()) {
                Empty("No logs yet", "Lines the app writes with DebugTools.log appear here, oldest at the top.")
            } else {
                Empty("No matching logs", "Clear the search or pick All to see every line.")
            }
            return@Column
        }
        LazyColumn(state = list, contentPadding = PaddingValues(vertical = DebugSpace.s)) {
            items(shown) { log -> LogRow(log, query, opened == log) { opened = if (opened == log) null else log } }
        }
    }
}

@Composable
private fun LogRow(log: DebugLog, query: String, open: Boolean, onToggle: () -> Unit) {
    val c = debugColors
    val color = levelColor(log.level)
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = c.muted)) { append(log.time.toString().substring(11, 19)) }
        append("  ")
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(log.level.name.first()) }
        append("  ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(log.tag) }
        append("  ")
        append(log.message)
        log.error?.let { error ->
            append("\n")
            withStyle(SpanStyle(color = color)) {
                append(if (open) error.trimEnd() else error.lineSequence().first())
            }
        }
    }.highlight(query, SpanStyle(background = c.tint(c.accent), color = c.ink))
    // 경고와 오류는 왼쪽 띠로 표시해서 긴 목록을 훑을 때 색 글자보다 먼저 눈에 걸린다.
    val stripe = if (log.level >= DebugLevel.Warn) color else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (log.error != null) Modifier.clickable(onClickLabel = if (open) "Hide stack" else "Show stack", onClick = onToggle) else Modifier),
    ) {
        Box(Modifier.width(DebugSpace.bar).fillMaxHeight().background(stripe))
        Text(
            text,
            style = DebugType.code,
            color = c.ink,
            modifier = Modifier.padding(start = DebugSpace.m, end = DebugSpace.l, top = DebugSpace.xs, bottom = DebugSpace.xs),
        )
    }
}

@Composable
private fun levelColor(level: DebugLevel): Color {
    val c = debugColors
    return when (level) {
        DebugLevel.Debug -> c.muted
        DebugLevel.Info -> c.accent
        DebugLevel.Warn -> c.warn
        DebugLevel.Error -> c.fail
    }
}

/** [query] 가 나오는 곳마다 [style] 을 덧칠한다. 대소문자는 가리지 않는다. */
private fun AnnotatedString.highlight(query: String, style: SpanStyle): AnnotatedString {
    if (query.isBlank()) return this
    return buildAnnotatedString {
        append(this@highlight)
        var from = text.indexOf(query, ignoreCase = true)
        while (from >= 0) {
            addStyle(style, from, from + query.length)
            from = text.indexOf(query, from + query.length, ignoreCase = true)
        }
    }
}

// ---------- 네트워크 ----------

@Composable
private fun NetworkTab(core: DebugCore, modifier: Modifier = Modifier) {
    val calls by core.calls.collectAsState()
    var query by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf<DebugCall?>(null) }
    val shown = calls.asReversed().filter { query.isBlank() || it.url.contains(query, ignoreCase = true) || it.method.contains(query, ignoreCase = true) }
    // 걸린 시간 막대는 지금 보이는 호출 중 가장 오래 걸린 것을 끝으로 잡는다. 느린 호출이 막대 길이로 바로 보인다.
    val slowest = shown.maxOfOrNull { it.duration.inWholeMilliseconds }?.coerceAtLeast(1) ?: 1
    Column(modifier) {
        SearchField(query, { query = it }, placeholder = "Search URL or method")
        if (shown.isEmpty()) {
            if (calls.isEmpty()) {
                Empty("No requests yet", "Requests from an HttpClient with installDebugTools() appear here, newest at the top.")
            } else {
                Empty("No matching requests", "Clear the search to see every request.")
            }
            return@Column
        }
        LazyColumn {
            items(shown) { call ->
                CallRow(call, call.duration.inWholeMilliseconds.toFloat() / slowest, open = opened == call) {
                    opened = if (opened == call) null else call
                }
                if (opened == call) CallDetail(call)
                HorizontalDivider(color = debugColors.line)
            }
        }
    }
}

@Composable
private fun statusColor(status: Int?): Color {
    val c = debugColors
    return when {
        status == null || status >= 500 -> c.fail
        status >= 400 -> c.warn
        status >= 300 -> c.accent
        else -> c.ok
    }
}

@Composable
private fun CallRow(call: DebugCall, share: Float, open: Boolean, onToggle: () -> Unit) {
    val c = debugColors
    val (host, path) = splitUrl(call.url)
    val ms = call.duration.inWholeMilliseconds
    val status = call.status?.toString() ?: "ERR"
    // 화면 읽기와 UI 테스트가 한 줄로 읽도록 요약 문장을 붙이고 안쪽 글자는 따로 읽지 않는다.
    val summary = "${call.method} $status $ms ms ${call.url}"
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (open) c.raised else Color.Transparent)
            .clickable(onClickLabel = if (open) "Hide details" else "Show details", onClick = onToggle)
            .semantics { text = AnnotatedString(summary) }
            .heightIn(min = DebugSpace.touch)
            .padding(horizontal = DebugSpace.l, vertical = DebugSpace.m),
    ) {
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(DebugSpace.m)) {
            StatusChip(status, statusColor(call.status))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DebugSpace.xs)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(call.method) }
                        append("  ")
                        append(path)
                    },
                    style = DebugType.code,
                    color = c.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(host, style = DebugType.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // 걸린 시간은 막대 바로 옆에 숫자로 둔다. 1초를 넘긴 호출은 막대를 경고 색으로 칠한다.
                Row(horizontalArrangement = Arrangement.spacedBy(DebugSpace.s), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(DebugSpace.bar).background(c.line, DebugShape.chip)) {
                        Box(
                            Modifier.fillMaxWidth(share.coerceIn(0.02f, 1f)).fillMaxHeight()
                                .background(if (ms > SLOW_MS) c.warn else c.muted, DebugShape.chip),
                        )
                    }
                    Text("$ms ms", style = DebugType.codeSmall, color = c.ink)
                    // 응답을 못 받은 호출은 크기가 없다.
                    if (call.status != null) Text(formatBytes(call.responseSize), style = DebugType.codeSmall, color = c.muted)
                }
            }
        }
    }
}

private const val SLOW_MS = 1_000

@Composable
private fun StatusChip(label: String, color: Color) {
    val c = debugColors
    Box(
        Modifier.widthIn(min = DebugSpace.statusChip).background(c.tint(color), DebugShape.chip).padding(horizontal = DebugSpace.xs, vertical = DebugSpace.xs / 2),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = DebugType.codeStrong, color = color)
    }
}

private enum class CallPart { Response, Request, Headers }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallDetail(call: DebugCall) {
    val c = debugColors
    var part by remember(call) { mutableStateOf(CallPart.Response) }
    // 마지막으로 복사한 것. 버튼 글자를 "Copied" 로 바꿔 복사됐다는 것을 보인다.
    var copied by remember(call) { mutableStateOf<String?>(null) }

    // ponytail: 새 클립보드 API 는 넣는 값을 플랫폼마다 따로 만들어야 해서 예전 API 를 쓴다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val body = when (part) {
        CallPart.Response -> call.responseBody
        CallPart.Request -> call.requestBody
        CallPart.Headers -> ""
    }
    val code = remember(body, c) { if (body.isEmpty()) AnnotatedString("(no body)") else jsonColors(prettyJson(body), c) }
    Column(
        Modifier.fillMaxWidth().background(c.raised).padding(horizontal = DebugSpace.l, vertical = DebugSpace.m),
        verticalArrangement = Arrangement.spacedBy(DebugSpace.m),
    ) {
        call.error?.let { Text(it, style = DebugType.code, color = c.fail) }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            CallPart.entries.forEach {
                SegmentedButton(
                    selected = part == it,
                    onClick = { part = it },
                    shape = SegmentedButtonDefaults.itemShape(it.ordinal, CallPart.entries.size),
                    icon = {},
                ) { Text(it.name, maxLines = 1) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DebugSpace.s)) {
            if (part != CallPart.Headers) {
                TextButton(enabled = body.isNotEmpty(), onClick = {
                    clipboard.setText(AnnotatedString(body))
                    copied = "body"
                }) { Text(if (copied == "body") "Copied" else "Copy body") }
            }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(call.curl()))
                copied = "curl"
            }) { Text(if (copied == "curl") "Copied" else "Copy cURL") }
        }
        when (part) {
            CallPart.Headers -> {
                HeaderBlock("Request headers", call.requestHeaders)
                HeaderBlock("Response headers", call.responseHeaders)
                if ((call.requestHeaders + call.responseHeaders).any { it.second == HIDDEN }) {
                    Text("$HIDDEN marks a value hidden before it was saved.", style = DebugType.caption, color = c.muted)
                }
            }

            else -> CodeBlock(code)
        }
    }
}

private const val HIDDEN = "***"

@Composable
private fun HeaderBlock(title: String, headers: List<Pair<String, String>>) {
    val c = debugColors
    Text(title, style = DebugType.section, color = c.ink, modifier = Modifier.semantics { heading() })
    CodeBlock(
        if (headers.isEmpty()) {
            AnnotatedString("(none)")
        } else {
            buildAnnotatedString {
                headers.forEachIndexed { i, (name, value) ->
                    if (i > 0) append("\n")
                    withStyle(SpanStyle(color = c.muted)) { append("$name: ") }
                    append(value)
                }
            }.markHidden(c)
        },
    )
}

/** 고정폭 본문 상자. 글자를 골라 복사할 수 있다. */
@Composable
private fun CodeBlock(text: AnnotatedString) {
    val c = debugColors
    SelectionContainer {
        Text(
            text,
            style = DebugType.code,
            color = c.ink,
            modifier = Modifier.fillMaxWidth().background(c.sunken, DebugShape.block).padding(DebugSpace.m),
        )
    }
}

private val JSON_TOKEN = Regex("(\"(?:[^\"\\\\]|\\\\.)*\")(\\s*:)?|\\b(true|false|null)\\b|(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")

/** 펴 놓은 JSON 에 색을 입힌다. 키는 강조색, 문자열 값은 성공 색, 숫자와 true, false, null 은 경고 색. 가린 값은 따로 표시한다. */
private fun jsonColors(text: String, c: DebugColors): AnnotatedString = buildAnnotatedString {
    append(text)
    JSON_TOKEN.findAll(text).forEach { m ->
        val color = when {
            m.groups[1] != null && m.groups[2] != null -> c.accent
            m.groups[1] != null -> c.ok
            else -> c.warn
        }
        // 1번 묶음(문자열)은 찾은 곳 맨 앞에서 시작한다. 키 뒤의 콜론은 칠하지 않는다.
        val length = m.groups[1]?.value?.length ?: m.value.length
        addStyle(SpanStyle(color = color), m.range.first, m.range.first + length)
    }
}.markHidden(c)

/** 저장할 때 가린 값(`***`)을 옅은 바탕과 흐린 글자로 표시해 원래 값이 아니라는 것을 보인다. */
private fun AnnotatedString.markHidden(c: DebugColors): AnnotatedString = highlight(HIDDEN, SpanStyle(background = c.line, color = c.muted, fontWeight = FontWeight.Bold))

/** 셸에 그대로 붙여 다시 보낼 수 있게 모든 값을 작은따옴표로 감싼다. 가린 헤더는 `***` 그대로라 키를 다시 넣어야 한다. */
internal fun DebugCall.curl(): String = buildString {
    fun quote(text: String) = "'" + text.replace("'", "'\\''") + "'"
    append("curl -X $method ${quote(url)}")
    requestHeaders.forEach { (name, value) -> append(" -H ${quote("$name: $value")}") }
    if (requestBody.isNotEmpty()) append(" --data-raw ${quote(requestBody)}")
}

// ---------- 스위치 ----------

@Composable
private fun SwitchesTab(core: DebugCore, modifier: Modifier = Modifier) {
    val c = debugColors
    val switches by core.switches.collectAsState()
    // 스위치 값은 기기 저장소에 있어 상태로 관찰되지 않는다. 바꿀 때마다 숫자를 올려 다시 읽는다.
    var version by remember { mutableIntStateOf(0) }
    if (switches.isEmpty()) {
        Box(modifier) { Empty("No switches yet", "Switches the app makes with DebugTools.switch appear here. Changes apply without a restart.") }
        return
    }
    val values = remember(version, switches) { switches.values.associateWith { it.enabled } }
    // 기본값과 다른 스위치는 앱을 평소와 다르게 돌게 한다. 켜 둔 것을 잊고 버그를 찾는 일이 없게 위에 알린다.
    val changed = values.count { (switch, enabled) -> enabled != switch.default }
    LazyColumn(modifier) {
        if (changed > 0) {
            item {
                Row(Modifier.fillMaxWidth().padding(DebugSpace.l).height(IntrinsicSize.Min).background(c.tint(c.warn), DebugShape.block)) {
                    Box(Modifier.width(DebugSpace.bar).fillMaxHeight().background(c.warn))
                    Text(
                        if (changed == 1) {
                            "1 switch differs from its default. The app is not running the way a normal build does."
                        } else {
                            "$changed switches differ from their defaults. The app is not running the way a normal build does."
                        },
                        style = DebugType.body,
                        color = c.ink,
                        modifier = Modifier.padding(DebugSpace.m),
                    )
                }
            }
        }
        items(switches.values.toList()) { switch ->
            val enabled = values.getValue(switch)
            Row(
                Modifier.fillMaxWidth()
                    .toggleable(value = enabled, role = Role.Switch) {
                        core.setSwitch(switch.key, it)
                        version++
                    }
                    .heightIn(min = DebugSpace.touch)
                    .padding(horizontal = DebugSpace.l, vertical = DebugSpace.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DebugSpace.l),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DebugSpace.xs)) {
                    Text(switch.title, style = DebugType.body, color = c.ink)
                    Text(switch.key, style = DebugType.codeSmall, color = c.muted)
                    if (enabled != switch.default) {
                        Text("Changed, default is ${if (switch.default) "on" else "off"}", style = DebugType.caption, color = c.warn)
                    }
                }
                // 줄 전체가 눌리므로 스위치 자체는 보여 주기만 한다.
                Switch(checked = enabled, onCheckedChange = null)
            }
            HorizontalDivider(Modifier.padding(start = DebugSpace.l), color = c.line)
        }
    }
}

// ---------- 패널, 정보 ----------

@Composable
private fun PanelsTab(core: DebugCore, modifier: Modifier = Modifier) {
    val c = debugColors
    val panels by core.panels.collectAsState()
    var version by remember { mutableIntStateOf(0) }
    // null 은 읽는 중이다. 앱 패널은 suspend 라서 디스크나 대기열을 읽느라 시간이 걸릴 수 있다.
    val rows by produceState<List<Pair<String, String>>?>(null, panels, version) {
        value = null
        value = core.panelRows()
    }
    Column(modifier) {
        if (panels.isEmpty()) {
            Empty("No panels yet", "The app adds one with DebugTools.panel(title) { rows }. Each panel is read when you open this tab.")
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(start = DebugSpace.l, end = DebugSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(if (panels.size == 1) "1 panel, read when opened" else "${panels.size} panels, read when opened", style = DebugType.caption, color = c.muted, modifier = Modifier.weight(1f))
            TextButton(enabled = rows != null, onClick = { version++ }) { Text("Reload") }
        }
        val current = rows
        if (current == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Reading app panels. Each panel's name and value rows appear here.",
                style = DebugType.body,
                color = c.muted,
                modifier = Modifier.padding(DebugSpace.l),
            )
        } else {
            Rows(current) {}
        }
    }
}

/** "이름, 값" 줄 목록. 이름이 `## ` 로 시작하면 소제목으로 그린다. 이름은 흐리게 왼쪽, 값은 고정폭으로 오른쪽에 둔다. */
@Composable
private fun Rows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier, empty: @Composable () -> Unit) {
    val c = debugColors
    if (rows.isEmpty()) {
        Box(modifier) { empty() }
        return
    }
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = DebugSpace.l)) {
        items(rows) { (name, value) ->
            if (name.startsWith("## ")) {
                Text(
                    name.removePrefix("## "),
                    style = DebugType.section,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() }.padding(start = DebugSpace.l, end = DebugSpace.l, top = DebugSpace.l, bottom = DebugSpace.xs),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = DebugSpace.l, vertical = DebugSpace.s),
                    horizontalArrangement = Arrangement.spacedBy(DebugSpace.m),
                ) {
                    Text(name, style = DebugType.caption, color = c.muted, modifier = Modifier.weight(0.4f))
                    SelectionContainer(Modifier.weight(0.6f)) { Text(AnnotatedString(value).markHidden(c), style = DebugType.code, color = c.ink) }
                }
            }
        }
    }
}

// ---------- 크래시 ----------

@Composable
private fun CrashesTab(core: DebugCore, modifier: Modifier = Modifier) {
    val c = debugColors
    var crashes by remember { mutableStateOf(core.crashes()) }
    if (crashes.isEmpty()) {
        Box(modifier) {
            Empty(
                "No crashes recorded",
                "When the app stops on an unhandled exception, the exception, switches and recent logs are saved here and added to the next report.",
            )
        }
        return
    }
    var opened by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier) {
        items(crashes) { (name, text) ->
            val open = opened == name
            Column(Modifier.padding(horizontal = DebugSpace.l, vertical = DebugSpace.m), verticalArrangement = Arrangement.spacedBy(DebugSpace.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(crashTime(name), style = DebugType.section, color = c.ink, modifier = Modifier.weight(1f).semantics { heading() })
                    TextButton(onClick = {
                        core.deleteCrash(name)
                        crashes = core.crashes()
                    }) { Text("Delete") }
                }
                val lines = text.lines().filter { it.isNotBlank() && !it.startsWith("# Crash") }
                // 예외 줄은 접어서 다 읽히게 하고, 스택은 줄마다 한 줄로 두고 옆으로 밀어 읽는다. 좁은 화면에서 접힌 스택은 프레임 경계가 안 보인다.
                SelectionContainer {
                    Column(Modifier.fillMaxWidth().background(c.sunken, DebugShape.block).padding(DebugSpace.m), verticalArrangement = Arrangement.spacedBy(DebugSpace.s)) {
                        Text(lines.firstOrNull().orEmpty(), style = DebugType.codeStrong, color = c.fail)
                        val rest = if (open) lines.drop(1) else lines.drop(1).take(CRASH_PREVIEW_LINES)
                        Text(
                            rest.joinToString("\n") { it.replace("\t", "  ") },
                            style = DebugType.codeSmall,
                            color = c.ink,
                            softWrap = false,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        )
                    }
                }
                if (lines.size > CRASH_PREVIEW_LINES + 1) {
                    TextButton(onClick = { opened = if (open) null else name }) {
                        Text(if (open) "Show less" else "Show all ${lines.size} lines")
                    }
                }
            }
            HorizontalDivider(color = c.line)
        }
    }
}

private const val CRASH_PREVIEW_LINES = 8

/** `crash-<밀리초>.txt` 를 사람이 읽는 시각으로 바꾼다. 이름이 다르면 그대로 쓴다. */
private fun crashTime(name: String): String = name.removePrefix("crash-").removeSuffix(".txt").toLongOrNull()
    ?.let { Instant.fromEpochMilliseconds(it).toString().substring(0, 19).replace('T', ' ') + " UTC" } ?: name

// ---------- 공통 ----------

/** 비었을 때 무엇이 여기 나타나는지와 어떻게 채우는지를 쓴다. */
@Composable
private fun Empty(title: String, body: String) {
    val c = debugColors
    Column(Modifier.fillMaxWidth().padding(DebugSpace.l), verticalArrangement = Arrangement.spacedBy(DebugSpace.s)) {
        Text(title, style = DebugType.section, color = c.ink)
        Text(body, style = DebugType.body, color = c.muted)
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val clear: (@Composable () -> Unit)? = if (query.isEmpty()) {
        null
    } else {
        { TextButton(onClick = { onQuery("") }) { Text("Clear") } }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text("Search") },
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        trailingIcon = clear,
        modifier = modifier.fillMaxWidth().padding(horizontal = DebugSpace.l, vertical = DebugSpace.m),
    )
}

/** 플랫폼 창에 띄우는 입구. 기본은 시스템 다크 모드를 따른다. */
@Composable
internal fun DebugApp(core: DebugCore, onShare: (String) -> Unit, onClose: () -> Unit, dark: Boolean = isSystemInDarkTheme()) {
    DebugTheme(dark) { DebugScreen(core, onShare, onClose) }
}
