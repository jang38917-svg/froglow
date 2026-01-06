@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.froglow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.froglow.app.ui.theme.FroglowTheme
import com.froglow.app.ui.screens.main.MainScreen  //MainScreen 연결


enum class MainTab { HOME, ROUTINE, HISTORY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FroglowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()    //MainScreen 연결
                }
            }
        }
    }
}

@Composable
fun MainPage() {
    var tab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Froglow", fontWeight = FontWeight.ExtraBold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tab = MainTab.HOME },
                    label = { Text("홈") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == MainTab.ROUTINE,
                    onClick = { tab = MainTab.ROUTINE },
                    label = { Text("루틴") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { tab = MainTab.HISTORY },
                    label = { Text("기록") },
                    icon = {}
                )
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (tab) {
                MainTab.HOME -> HomeTab(onStartDiagnosis = { /* TODO: 카메라 화면 */ })
                MainTab.ROUTINE -> RoutineTab()
                MainTab.HISTORY -> HistoryTab()
            }
        }
    }
}

@Composable
private fun HomeTab(onStartDiagnosis: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("오늘의 상태", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("피부 진단을 시작해볼까요?")
                Text(
                    "카메라로 촬영해서 피부 상태를 확인하는 기능(추후 연결)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onStartDiagnosis, modifier = Modifier.align(Alignment.End)) {
                    Text("진단 시작")
                }
            }
        }

        Text("오늘 루틴", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("아침", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("세안") })
                    AssistChip(onClick = {}, label = { Text("토너") })
                    AssistChip(onClick = {}, label = { Text("크림") })
                }

                HorizontalDivider()

                Text("저녁", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("클렌징") })
                    AssistChip(onClick = {}, label = { Text("세럼") })
                    AssistChip(onClick = {}, label = { Text("보습") })
                }
            }
        }
    }
}

@Composable
private fun RoutineTab() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("루틴 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("루틴을 편집하는 화면(임시 UI)")
                Button(onClick = {}) { Text("루틴 편집") }
            }
        }
    }
}

@Composable
private fun HistoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("달력 + 체크 기록 영역(임시 UI)")
                Button(onClick = {}) { Text("이번 달 보기") }
            }
        }
    }
}
