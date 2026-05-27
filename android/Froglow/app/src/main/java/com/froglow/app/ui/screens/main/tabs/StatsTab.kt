package com.froglow.app.ui.screens.main.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.froglow.app.network.DiagnosisStatsItemDto
import java.time.LocalDate

private val StatsScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val ChartBg = Color(0xFFFFFEFC)
private val GridLineColor = Color(0xFFEDE6DA)
private val AxisLineColor = Color(0xFFD9CDBB)
private val SummaryCardBg = Color(0xFFFFFCF7)

private enum class StatsMetric(
    val label: String,
    val color: Color,
    val selectedBg: Color
) {
    ACNE("여드름", Color(0xFFE95F62), Color(0xFFFFE8E8)),
    REDNESS("홍조", Color(0xFFE98357), Color(0xFFFFEFE7)),
    SPOT("잡티", Color(0xFF8A7464), Color(0xFFF2ECE6)),
    WRINKLE("주름", Color(0xFF8A78C8), Color(0xFFF0EBFF))
}

@Composable
fun StatsTab(
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMetric by remember { mutableStateOf(StatsMetric.ACNE) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StatsScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "피부 통계",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        StatsModeSelector(
            selectedMode = uiState.selectedMode,
            onWeeklyClick = { viewModel.selectWeekly() },
            onMonthlyClick = { viewModel.selectMonthly() }
        )

        StatsDateNavigator(
            selectedMode = uiState.selectedMode,
            selectedDate = uiState.selectedDate,
            onPrevClick = { viewModel.movePrevious() },
            onNextClick = { viewModel.moveNext() }
        )

        MetricSelector(
            selectedMetric = selectedMetric,
            onSelect = { selectedMetric = it }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GreenPrimary
                    )
                }
            }

            uiState.errorMessage != null -> {
                ErrorCard(
                    message = uiState.errorMessage ?: "오류가 발생했습니다.",
                    onRetry = { viewModel.refresh() }
                )
            }

            uiState.stats != null -> {
                MetricTrendCard(
                    metric = selectedMetric,
                    items = uiState.stats!!.items,
                    valueSelector = { item ->
                        when (selectedMetric) {
                            StatsMetric.ACNE -> item.acne
                            StatsMetric.REDNESS -> item.redness
                            StatsMetric.SPOT -> item.spot
                            StatsMetric.WRINKLE -> item.wrinkle
                        }
                    }
                )
            }

            else -> {
                ErrorCard(
                    message = "통계 데이터가 없습니다.",
                    onRetry = { viewModel.refresh() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatsModeSelector(
    selectedMode: StatsMode,
    onWeeklyClick: () -> Unit,
    onMonthlyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModeButton(
            text = "주간",
            selected = selectedMode == StatsMode.WEEKLY,
            onClick = onWeeklyClick,
            modifier = Modifier.weight(1f)
        )

        ModeButton(
            text = "월간",
            selected = selectedMode == StatsMode.MONTHLY,
            onClick = onMonthlyClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) GreenPrimary else CardBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) GreenPrimary else CardBorder
        ),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else GreenPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StatsDateNavigator(
    selectedMode: StatsMode,
    selectedDate: LocalDate,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val title = when (selectedMode) {
        StatsMode.WEEKLY -> "${selectedDate.year}년 ${selectedDate.monthValue}월"
        StatsMode.MONTHLY -> "${selectedDate.year}년"
    }

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
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateMoveButton(
                text = "이전",
                onClick = onPrevClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            DateMoveButton(
                text = "다음",
                onClick = onNextClick
            )
        }
    }
}

@Composable
private fun DateMoveButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = GreenPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MetricSelector(
    selectedMetric: StatsMetric,
    onSelect: (StatsMetric) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsMetric.entries.forEach { metric ->
            val selected = metric == selectedMetric

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(metric) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) {
                    metric.selectedBg
                } else {
                    CardBg
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) metric.selectedBg else CardBorder
                ),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = metric.label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) metric.color else SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MetricTrendCard(
    metric: StatsMetric,
    items: List<DiagnosisStatsItemDto>,
    valueSelector: (DiagnosisStatsItemDto) -> Float?
) {
    val values = items.mapNotNull { valueSelector(it) }
    val latestValue = items.asReversed().firstNotNullOfOrNull { valueSelector(it) }
    val averageValue = values.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val minValue = values.minOrNull()

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
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${metric.label} 변화",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Text(
                        text = "점수가 낮을수록 양호한 상태예요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                Text(
                    text = latestValue?.let { String.format("최근 %.1f점", it) } ?: "기록 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (latestValue != null) metric.color else SecondaryText,
                    fontWeight = FontWeight.Bold
                )
            }

            TrendLineChart(
                items = items,
                color = metric.color,
                valueSelector = valueSelector
            )

            StatsXAxisLabels(
                items = items
            )

            StatsSummaryRow(
                metricColor = metric.color,
                averageValue = averageValue,
                minValue = minValue,
                latestValue = latestValue
            )
        }
    }
}

@Composable
private fun StatsSummaryRow(
    metricColor: Color,
    averageValue: Float?,
    minValue: Float?,
    latestValue: Float?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsSummaryItem(
            modifier = Modifier.weight(1f),
            title = "평균 점수",
            value = averageValue?.let { String.format("%.1f점", it) } ?: "-",
            valueColor = metricColor
        )

        StatsSummaryItem(
            modifier = Modifier.weight(1f),
            title = "최저 점수",
            value = minValue?.let { String.format("%.1f점", it) } ?: "-",
            valueColor = GreenPrimary
        )

        StatsSummaryItem(
            modifier = Modifier.weight(1f),
            title = "최근 점수",
            value = latestValue?.let { String.format("%.1f점", it) } ?: "-",
            valueColor = metricColor
        )
    }
}

@Composable
private fun StatsSummaryItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SummaryCardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatsXAxisLabels(
    items: List<DiagnosisStatsItemDto>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, item ->
            Text(
                text = formatStatsRangeLabel(item, index),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TrendLineChart(
    items: List<DiagnosisStatsItemDto>,
    color: Color,
    valueSelector: (DiagnosisStatsItemDto) -> Float?
) {
    val hasData = items.any { valueSelector(it) != null }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = ChartBg,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val leftPadding = 12f
            val rightPadding = 12f
            val topPadding = 24f
            val bottomPadding = 28f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            val left = leftPadding
            val right = leftPadding + chartWidth
            val top = topPadding
            val bottom = topPadding + chartHeight

            for (i in 0..5) {
                val y = bottom - (chartHeight / 5f) * i
                drawLine(
                    color = GridLineColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.2f
                )
            }

            drawLine(
                color = AxisLineColor,
                start = Offset(left, top),
                end = Offset(left, bottom),
                strokeWidth = 2f
            )

            drawLine(
                color = AxisLineColor,
                start = Offset(left, bottom),
                end = Offset(right, bottom),
                strokeWidth = 2f
            )

            if (items.isEmpty()) return@Canvas

            val slotWidth = chartWidth / items.size.coerceAtLeast(1)

            val indexedPoints = items.mapIndexedNotNull { index, item ->
                val value = valueSelector(item) ?: return@mapIndexedNotNull null
                val safeValue = value.coerceIn(0f, 10f)

                Triple(
                    index,
                    safeValue,
                    Offset(
                        x = left + slotWidth * index + slotWidth / 2f,
                        y = bottom - (safeValue / 10f) * chartHeight
                    )
                )
            }

            indexedPoints.zipWithNext().forEach { (first, second) ->
                if (second.first == first.first + 1) {
                    drawLine(
                        color = color,
                        start = first.third,
                        end = second.third,
                        strokeWidth = 4.5f
                    )
                }
            }

            indexedPoints.forEach { (_, value, point) ->
                drawCircle(
                    color = color,
                    radius = 7f,
                    center = point
                )

                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = point
                )

                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", value),
                    point.x,
                    point.y - 18f,
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.rgb(88, 82, 74)
                        textSize = 22f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }
        }

        if (!hasData) {
            Text(
                text = "아직 기록된 데이터가 없어요",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatStatsRangeLabel(
    item: DiagnosisStatsItemDto,
    index: Int
): String {
    if (item.label.isNotBlank()) {
        return item.label
    }

    return "${index + 1}주차"
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
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
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("다시 시도")
            }
        }
    }
}