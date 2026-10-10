plugins {
    alias(libs.plugins.aiopicKmpCompose)
}

// 앱 틀의 하단 탭 아이콘과 화면의 버튼 아이콘이 Montage 아이콘을 꺼내 쓴다.
compose.resources {
    publicResClass = true
}

kotlin {
    sourceSets {
        // 움직임 부품을 테스트 시계로 멈춰 끝 장면과 동작 줄이기를 비교한다.
        getByName("androidHostTest").dependencies {
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.compose.uiTestManifest)
            implementation(libs.roborazzi.core)
        }
    }
}
