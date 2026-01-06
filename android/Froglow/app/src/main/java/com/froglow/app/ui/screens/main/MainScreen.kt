package com.froglow.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import androidx.compose.material3.*

enum class MainBottomTab { COMMUNITY, PRODUCTS, MAIN, CHAT, MYPAGE }

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(MainBottomTab.MAIN) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            MainContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 80.dp) // 하단바 공간 확보
            )

            // 개구리(카메라) 버튼 자리만 잡기 (나중에 카메라 연결)
            FrogCameraButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 80.dp), // 하단바 위에 살짝
                onClick = {
                    // TODO: 나중에 CameraScreen으로 이동
                }
            )
        }
    }
}

@Composable
private fun MainContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoutineAlertCard()
        CalendarSection()
    }
}

private enum class RoutineTime { MORNING, MIDDAY, NIGHT }

//루틴 알림 창
@Composable
fun RoutineAlertCard(
    modifier: Modifier = Modifier
) {
    val time = remember { currentRoutineTime() }

    // FE용 임시 상태(나중에 서버 연결하면 교체)
    var skin by remember { mutableStateOf(false) }
    var lotion by remember { mutableStateOf(false) }
    var cream by remember { mutableStateOf(false) }

    val (emoji, title) = when (time) {
        RoutineTime.MORNING -> "☀️" to "아침 루틴"
        RoutineTime.MIDDAY -> "💦" to "중간 루틴"
        RoutineTime.NIGHT -> "🌙" to "저녁 루틴"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 아이콘(시간대)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE7E7)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoutineCheckItem(
                    label = "스킨",
                    checked = skin,
                    onToggle = { skin = !skin }
                )
                RoutineCheckItem(
                    label = "로션",
                    checked = lotion,
                    onToggle = { lotion = !lotion }
                )
                RoutineCheckItem(
                    label = "크림",
                    checked = cream,
                    onToggle = { cream = !cream }
                )
            }
        }
    }
}

@Composable
private fun RoutineCheckItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Black, CircleShape)
                .background(if (checked) Color.Black else Color.Transparent)
        )
    }
}

/**
 * 시간대 규칙
 * - MORNING: 05~11
 * - MIDDAY : 12~17
 * - NIGHT  : 18~04
 */
private fun currentRoutineTime(): RoutineTime {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> RoutineTime.MORNING
        in 12..17 -> RoutineTime.MIDDAY
        else -> RoutineTime.NIGHT
    }
}

//달력
@Composable
private fun CalendarSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("달력", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // 주/달 전환 같은 건 나중에
        CalendarDotsBlock()
    }
}

@Composable
private fun CalendarDotsBlock() {
    // 임시 데이터: 4주 * 7일
    val weeks = remember {
        List(4) { weekIndex ->
            List(7) { dayIndex ->
                (weekIndex + dayIndex) % 5 == 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { checked ->
                    Dot(checked = checked)
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun Dot(checked: Boolean) {
    val size = 14.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            )
    )
}

//카메라 버튼
@Composable
private fun FrogCameraButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(55.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xFF6BCF9E),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("🐸", style = MaterialTheme.typography.titleLarge)
        }
    }
}

//하단 바
@Composable
private fun BottomNavBar(
    selected: MainBottomTab,
    onSelect: (MainBottomTab) -> Unit
) {
    NavigationBar {
        BottomNavItem(label = "통계", isSelected = selected == MainBottomTab.COMMUNITY) {
            onSelect(MainBottomTab.COMMUNITY)
        }
        BottomNavItem(label = "제품", isSelected = selected == MainBottomTab.PRODUCTS) {
            onSelect(MainBottomTab.PRODUCTS)
        }
        BottomNavItem(label = "메인", isSelected = selected == MainBottomTab.MAIN) {
            onSelect(MainBottomTab.MAIN)
        }
        BottomNavItem(label = "챗봇", isSelected = selected == MainBottomTab.CHAT) {
            onSelect(MainBottomTab.CHAT)
        }
        BottomNavItem(label = "마이", isSelected = selected == MainBottomTab.MYPAGE) {
            onSelect(MainBottomTab.MYPAGE)
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {}, // 아이콘은 나중에
        label = { Text(label) }
    )
}
