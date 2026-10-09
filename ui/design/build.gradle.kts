plugins {
    alias(libs.plugins.aiopicKmpCompose)
}

// 앱 틀의 하단 탭 아이콘과 화면의 버튼 아이콘이 Montage 아이콘을 꺼내 쓴다.
compose.resources {
    publicResClass = true
}
