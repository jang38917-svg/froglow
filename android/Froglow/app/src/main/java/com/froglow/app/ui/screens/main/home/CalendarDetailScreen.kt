package com.froglow.app.ui.screens.main.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.froglow.app.network.DailyItem
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.RoutineItemDto
import com.froglow.app.ui.screens.calendar.CalendarViewModel
import java.time.LocalDate

private val ScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardSoftBg = Color(0xFFFFFCF7)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val YellowSoft = Color(0xFFFFF4DD)
private val YellowText = Color(0xFFD79A27)
private val YellowBorder = Color(0xFFEBD9A8)

private val PurpleSoft = Color(0xFFF3EDFF)
private val PurpleText = Color(0xFF7B63B8)
private val PurpleBorder = Color(0xFFE2D7F5)

private val ErrorSoft = Color(0xFFFFEFEF)
private val ErrorText = Color(0xFFE95F62)

private enum class FaceTab(val label: String, val fullLabel: String) {
    FRONT("정면", "정면 얼굴"),
    LEFT("좌측", "좌측 얼굴"),
    RIGHT("우측", "우측 얼굴")
}

@Composable
fun CalendarDetailScreen(
    selectedDate: String,
    onBack: () -> Unit = {},
    onOpenProductDetail: (Long) -> Unit = {},
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    var selectedRecordIndex by remember { mutableIntStateOf(0) }
    var selectedFaceTab by remember { mutableStateOf(FaceTab.FRONT) }

    LaunchedEffect(selectedDate) {
        viewModel.loadCurrentMonth()
        if (selectedDate.isNotBlank()) {
            viewModel.loadDaily(selectedDate)
        }
    }

    LaunchedEffect(state.dailyItems) {
        selectedRecordIndex = 0
        selectedFaceTab = FaceTab.FRONT
    }

    val currentItem = state.dailyItems.getOrNull(selectedRecordIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailTopBar(
            title = formatHeaderDate(selectedDate),
            onBack = onBack
        )

        when {
            state.loadingDaily -> {
                LoadingCard()
            }

            currentItem == null -> {
                EmptyRecordCard(
                    selectedDate = selectedDate,
                    error = state.error
                )
            }

            else -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, CardBorder),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RecordPagerHeader(
                            count = state.dailyItems.size,
                            selectedIndex = selectedRecordIndex,
                            onPrev = {
                                if (selectedRecordIndex > 0) {
                                    selectedRecordIndex--
                                    selectedFaceTab = FaceTab.FRONT
                                }
                            },
                            onNext = {
                                if (selectedRecordIndex < state.dailyItems.lastIndex) {
                                    selectedRecordIndex++
                                    selectedFaceTab = FaceTab.FRONT
                                }
                            }
                        )

                        FaceTabRow(
                            selectedTab = selectedFaceTab,
                            onSelect = { selectedFaceTab = it }
                        )

                        FaceImageSection(
                            item = currentItem,
                            selectedTab = selectedFaceTab
                        )

                        ScoreTable(item = currentItem)

                        SkinConditionNotice(item = currentItem)

                        RoutineSection(
                            title = "아침 루틴",
                            emoji = "☀️",
                            items = state.dailyRoutineAM,
                            emptyMessage = "해당 날짜의 아침 루틴이 없습니다.",
                            onOpenProductDetail = onOpenProductDetail
                        )

                        RoutineSection(
                            title = "저녁 루틴",
                            emoji = "🌙",
                            items = state.dailyRoutinePM,
                            emptyMessage = "해당 날짜의 저녁 루틴이 없습니다.",
                            onOpenProductDetail = onOpenProductDetail
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit
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
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryText
        )
    }
}

@Composable
private fun LoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "기록 불러오는 중...",
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyRecordCard(
    selectedDate: String,
    error: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "선택한 날짜",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Text(
                text = selectedDate,
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardSoftBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "해당 날짜에 저장된 피부 진단 기록이 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!error.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ErrorSoft,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(14.dp),
                        color = ErrorText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordPagerHeader(
    count: Int,
    selectedIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSoftBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                enabled = selectedIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "이전 기록",
                    tint = if (selectedIndex > 0) PrimaryText else SecondaryText.copy(alpha = 0.35f)
                )
            }

            Text(
                text = if (count == 0) "기록 없음" else "${selectedIndex + 1} / $count 기록",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryText
            )

            IconButton(
                onClick = onNext,
                enabled = selectedIndex < count - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "다음 기록",
                    tint = if (selectedIndex < count - 1) PrimaryText else SecondaryText.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun FaceTabRow(
    selectedTab: FaceTab,
    onSelect: (FaceTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF6F1E6),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FaceTab.entries.forEach { tab ->
                val selected = tab == selectedTab

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) GreenSoft else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, GreenPrimary) else null,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) GreenPrimary else SecondaryText,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceImageSection(
    item: DailyItem,
    selectedTab: FaceTab
) {
    val rawImagePath = when (selectedTab) {
        FaceTab.FRONT -> item.images.front?.annotated ?: item.images.front?.original
        FaceTab.LEFT -> item.images.left?.annotated ?: item.images.left?.original
        FaceTab.RIGHT -> item.images.right?.annotated ?: item.images.right?.original
    }

    val imageUrl = RetrofitClient.buildImageUrl(rawImagePath)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF6F1E6))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "${selectedTab.label} 얼굴 분석 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "분석 이미지가 없습니다.",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!rawImagePath.isNullOrBlank()) {
                    Text(
                        text = rawImagePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xEFFFFEFC),
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Text(
                text = selectedTab.fullLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = PrimaryText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ScoreTable(item: DailyItem) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "피부 분석 결과",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = CardSoftBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ScoreRow("여드름", item.diagnosis.acne)
                ScoreRow("홍조", item.diagnosis.redness)
                ScoreRow("잡티", item.diagnosis.spot)
                ScoreRow("주름", item.diagnosis.wrinkle)

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = CardBorder)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "주요 고민: ${formatPrimaryConcern(item.primary_concern)}",
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = PrimaryText
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${score}점",
                color = SecondaryText
            )

            Text(
                text = toGrade(score),
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SkinConditionNotice(item: DailyItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GreenSoft,
        border = BorderStroke(1.dp, Color(0xFFD3E4C8)),
        tonalElevation = 0.dp
    ) {
        Text(
            text = buildSkinConditionMessage(
                acneScore = item.diagnosis.acne,
                rednessScore = item.diagnosis.redness,
                spotScore = item.diagnosis.spot,
                wrinkleScore = item.diagnosis.wrinkle
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RoutineSection(
    title: String,
    emoji: String,
    items: List<RoutineItemDto>,
    emptyMessage: String,
    onOpenProductDetail: (Long) -> Unit
) {
    val routineType = if (title.contains("아침")) "AM" else "PM"
    val isMorning = routineType == "AM"

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "$emoji $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = if (isMorning) Color(0xFFFFFCF7) else Color(0xFFFFFCF7),
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            if (items.isEmpty()) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(16.dp),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sortRoutineItemsForDisplay(items, routineType).forEach { item ->
                        RoutineStepRow(
                            item = item,
                            isMorning = isMorning,
                            onClick = onOpenProductDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineStepRow(
    item: RoutineItemDto,
    isMorning: Boolean,
    onClick: (Long) -> Unit
) {
    val productName = item.product_name ?: "제품명 없음"
    val stepName = item.step_name ?: "${item.step_order}단계"
    val imageUrl = item.product_image_url

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.product_id != 0L) {
                    onClick(item.product_id)
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = if (isMorning) Color(0xFFFFFDF7) else Color(0xFFFFFDF7),
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF6F1E6)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = productName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = PrimaryText
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isMorning) YellowSoft else PurpleSoft,
                    border = BorderStroke(
                        1.dp,
                        if (isMorning) YellowBorder else PurpleBorder
                    ),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = stepName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMorning) YellowText else PurpleText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatHeaderDate(date: String): String {
    return try {
        val localDate = LocalDate.parse(date)
        val dayOfWeek = when (localDate.dayOfWeek.value) {
            1 -> "월요일"
            2 -> "화요일"
            3 -> "수요일"
            4 -> "목요일"
            5 -> "금요일"
            6 -> "토요일"
            else -> "일요일"
        }
        "${localDate.monthValue}월 ${localDate.dayOfMonth}일 $dayOfWeek"
    } catch (_: Exception) {
        date
    }
}

private fun toGrade(score: Int): String {
    return when {
        score <= 2 -> "A"
        score <= 4 -> "B"
        score <= 6 -> "C"
        score <= 8 -> "D"
        else -> "E"
    }
}

private fun formatPrimaryConcern(value: String): String {
    return when (value.lowercase()) {
        "acne" -> "여드름"
        "redness" -> "홍조"
        "spot", "spots", "pigmentation" -> "잡티"
        "wrinkle", "wrinkles" -> "주름"
        else -> value
    }
}

private fun buildSkinConditionMessage(
    acneScore: Int,
    rednessScore: Int,
    spotScore: Int,
    wrinkleScore: Int
): String {
    val average = listOf(acneScore, rednessScore, spotScore, wrinkleScore).average()

    return when {
        average <= 3.0 -> {
            "피부 점수는 낮을수록 좋은 상태예요. 이 날은 전반적으로 피부 상태가 안정적인 편이에요."
        }

        average <= 6.0 -> {
            "피부 점수는 낮을수록 좋은 상태예요. 이 날은 일부 항목만 가볍게 관리해주면 좋아요."
        }

        else -> {
            "피부 점수는 낮을수록 좋은 상태예요. 이 날은 피부 컨디션을 조금 더 신경 써주세요."
        }
    }
}

private fun sortRoutineItemsForDisplay(
    items: List<RoutineItemDto>,
    routineType: String
): List<RoutineItemDto> {
    return items.sortedWith(
        compareBy<RoutineItemDto>(
            { routineSortOrder(it, routineType) },
            { it.step_order },
            { it.product_name ?: "" }
        )
    )
}

private fun routineSortOrder(
    item: RoutineItemDto,
    routineType: String
): Int {
    val text = listOfNotNull(
        item.step_name,
        item.product_name,
        item.reason
    ).joinToString(" ").lowercase()

    return when {
        text.contains("토너패드") || text.contains("패드") -> 1
        text.contains("토너") || text.contains("스킨") -> 1

        text.contains("앰플") -> 2
        text.contains("에센스") -> 2
        text.contains("세럼") -> 2

        text.contains("에멀전") -> 3
        text.contains("로션") -> 3
        text.contains("크림") -> 3

        routineType == "AM" && (
                text.contains("선크림") ||
                        text.contains("선블록") ||
                        text.contains("자외선") ||
                        text.contains("sun")
                ) -> 4

        routineType == "PM" && (
                text.contains("마스크") ||
                        text.contains("팩") ||
                        text.contains("나이트")
                ) -> 4

        else -> 99
    }
}