package com.froglow.app.ui.screens.main.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.froglow.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val CalendarCardBg = Color(0xFFFFFFFF)

private val HeaderTextColor = Color(0xFF2F2B26)
private val WeekdayColor = Color(0xFF3B3630)
private val SundayColor = Color(0xFFFF5A5A)
private val SaturdayColor = Color(0xFF2F7FEA)

private val ArrowColor = Color(0xFF4C5D3F)
private val SelectedDayBg = Color(0xFF8B6F55)
private val SelectedDayText = Color(0xFFFFFFFF)

private val CalendarOuterBorder = Color(0xFFE4D9C8)
private val DividerColor = Color(0xFFE6DCCB)

@Composable
fun CalendarCard(
    routineDoneMap: Map<LocalDate, Boolean>,
    hasSkinRecordMap: Map<LocalDate, Boolean>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    yearMonth: YearMonth = YearMonth.now()
) {
    var currentMonth by remember { mutableStateOf(yearMonth) }

    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()

    val leadingEmpty = when (firstDay.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        else -> firstDay.dayOfWeek.value
    }

    val allCells = buildList<LocalDate?> {
        repeat(leadingEmpty) {
            add(null)
        }

        for (day in 1..daysInMonth) {
            add(currentMonth.atDay(day))
        }

        while (size % 7 != 0) {
            add(null)
        }
    }

    val rows = allCells.chunked(7)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = CalendarCardBg,
        border = BorderStroke(1.dp, CalendarOuterBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 20.dp
            )
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPrevClick = {
                    currentMonth = currentMonth.minusMonths(1)
                },
                onNextClick = {
                    currentMonth = currentMonth.plusMonths(1)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            WeekHeader()

            Spacer(modifier = Modifier.height(9.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.2.dp)
                    .background(DividerColor)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.82f)
                                )
                            } else {
                                val isSelected = selectedDate == date
                                val hasRecord = hasSkinRecordMap[date] == true

                                FrogDayCell(
                                    modifier = Modifier.weight(1f),
                                    date = date,
                                    isSelected = isSelected,
                                    hasRecord = hasRecord,
                                    onClick = {
                                        onDateSelected(date)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "‹",
                modifier = Modifier.clickable { onPrevClick() },
                color = ArrowColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp
            )
        }

        Box(
            modifier = Modifier.weight(3f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                color = HeaderTextColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "›",
                modifier = Modifier.clickable { onNextClick() },
                color = ArrowColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp
            )
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, label ->
            val color = when (index) {
                0 -> SundayColor
                6 -> SaturdayColor
                else -> WeekdayColor
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = color,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun FrogDayCell(
    modifier: Modifier,
    date: LocalDate,
    isSelected: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit
) {
    val dayColor = when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> SundayColor
        DayOfWeek.SATURDAY -> SaturdayColor
        else -> WeekdayColor
    }

    val frogImage = if (hasRecord) {
        R.drawable.frog_record
    } else {
        R.drawable.frog_empty
    }

    Column(
        modifier = modifier
            .aspectRatio(0.82f)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = frogImage),
                contentDescription = null,
                modifier = Modifier
                    .size(37.dp)
                    .alpha(if (hasRecord) 1f else 0.72f),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SelectedDayBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = SelectedDayText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                color = dayColor,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}