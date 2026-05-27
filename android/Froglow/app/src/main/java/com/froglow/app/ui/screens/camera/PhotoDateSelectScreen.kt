package com.froglow.app.ui.screens.camera

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val ScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)
private val DisabledGreen = Color(0xFFDDEAD6)

private val DateFieldBg = Color(0xFFF6F2EA)
private val InfoBg = Color(0xFFFFFCF7)

@Composable
fun PhotoDateSelectScreen(
    photoUris: List<String>,
    onBack: () -> Unit = {},
    onSaveDone: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var selectedDate by remember {
        mutableStateOf(today)
    }

    var showDateDialog by remember {
        mutableStateOf(false)
    }

    if (photoUris.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "사진 정보가 없습니다.",
                color = PrimaryText
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = PrimaryText
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "날짜 선택",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryText
            )
        }

        Text(
            text = "촬영한 사진 3장을 어떤 날짜로 진단 기록할지 선택해주세요.",
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            photoUris.take(3).forEachIndexed { index, uri ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = DateFieldBg,
                    border = BorderStroke(1.dp, CardBorder),
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "사진 ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        Text(
            text = "선택 날짜",
            modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDateDialog = true },
            shape = RoundedCornerShape(18.dp),
            color = DateFieldBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedDate.format(formatter),
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(
            onClick = {
                val selectedDateString = selectedDate.format(formatter)

                Toast.makeText(
                    context,
                    "$selectedDateString 날짜로 진단을 시작합니다.",
                    Toast.LENGTH_SHORT
                ).show()

                onSaveDone(selectedDateString)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                contentColor = Color.White,
                disabledContainerColor = DisabledGreen,
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Text(
                text = "이 날짜로 진단하기",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            shape = RoundedCornerShape(18.dp),
            color = InfoBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = GreenSoft,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "안내",
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "사진은 안전하게 분석 후 자동 삭제돼요.\n안심하고 진단 받으실 수 있어요 :)",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    if (showDateDialog) {
        FroglowDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDateDialog = false },
            onConfirm = {
                selectedDate = it
                showDateDialog = false
            }
        )
    }
}

@Composable
private fun FroglowDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var displayedMonth by remember {
        mutableStateOf(YearMonth.from(initialDate))
    }

    var tempSelectedDate by remember {
        mutableStateOf(initialDate)
    }

    val days = buildCalendarCells(displayedMonth)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GreenSoft,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "${tempSelectedDate.year}",
                            color = GreenPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = tempSelectedDate.format(DateTimeFormatter.ofPattern("M월 d일")),
                            color = PrimaryText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DateFieldBg,
                        tonalElevation = 0.dp
                    ) {
                        IconButton(
                            onClick = {
                                displayedMonth = displayedMonth.minusMonths(1)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "이전 달",
                                tint = PrimaryText
                            )
                        }
                    }

                    Text(
                        text = "${displayedMonth.year}년 ${displayedMonth.monthValue}월",
                        modifier = Modifier.weight(1f),
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = CircleShape,
                        color = DateFieldBg,
                        tonalElevation = 0.dp
                    ) {
                        IconButton(
                            onClick = {
                                displayedMonth = displayedMonth.plusMonths(1)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "다음 달",
                                tint = PrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (date == null) {
                                    Spacer(modifier = Modifier.size(1.dp))
                                } else {
                                    val isSelected = date == tempSelectedDate
                                    val isCurrentMonth = date.month == displayedMonth.month

                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable {
                                                tempSelectedDate = date
                                            },
                                        shape = CircleShape,
                                        color = if (isSelected) GreenPrimary else Color.Transparent,
                                        tonalElevation = 0.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                color = when {
                                                    isSelected -> Color.White
                                                    isCurrentMonth -> PrimaryText
                                                    else -> SecondaryText.copy(alpha = 0.4f)
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "취소",
                            color = SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onConfirm(tempSelectedDate)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "확인",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()

    val startOffset = dayOfWeekToSundayStart(firstDay.dayOfWeek)

    val cells = mutableListOf<LocalDate?>()

    repeat(startOffset) {
        cells.add(null)
    }

    var current = firstDay
    while (!current.isAfter(lastDay)) {
        cells.add(current)
        current = current.plusDays(1)
    }

    while (cells.size % 7 != 0) {
        cells.add(null)
    }

    return cells
}

private fun dayOfWeekToSundayStart(dayOfWeek: DayOfWeek): Int {
    return when (dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}