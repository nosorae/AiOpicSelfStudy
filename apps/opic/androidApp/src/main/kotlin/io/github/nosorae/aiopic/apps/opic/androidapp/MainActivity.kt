package io.github.nosorae.aiopic.apps.opic.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.nosorae.aiopic.apps.opic.shared.AppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 모든 화면을 시스템 바 아래까지 그린다. 인셋은 공용 화면 틀(MontageScreen)이 처리한다(ADR 0021).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppShell((application as OpicApplication).graph) }
    }
}
