package com.froglow.app.ui.screens.main.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.froglow.app.R
import com.froglow.app.network.ApiService
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.RoutineCompletionRequest
import com.froglow.app.network.RoutineItemDto
import com.froglow.app.ui.screens.main.home.CalendarCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val HomeScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val YellowSoft = Color(0xFFFFF4DD)
private val PurpleSoft = Color(0xFFF3EDFF)
private val YellowText = Color(0xFFE0A527)
private val PurpleText = Color(0xFF7D6BC8)

@Composable
fun HomeTab(
    onOpenCalendarDetail: (String) -> Unit
) {
    MainContent(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeScreenBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        onOpenCalendarDetail = onOpenCalendarDetail
    )
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    onOpenCalendarDetail: (String) -> Unit
) {
    val today = LocalDate.now()
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var refreshKey by remember { mutableIntStateOf(0) }
    val externalRefreshKey by RoutineRefreshStore.refreshKey.collectAsState()

    var selectedDate by remember { mutableStateOf(today) }
    var calendarDates by remember { mutableStateOf<List<String>>(emptyList()) }

    var amRoutineItems by remember { mutableStateOf<List<RoutineItemDto>>(emptyList()) }
    var pmRoutineItems by remember { mutableStateOf<List<RoutineItemDto>>(emptyList()) }

    var amRoutineLoaded by remember { mutableStateOf(false) }
    var pmRoutineLoaded by remember { mutableStateOf(false) }

    var amCompleted by remember { mutableStateOf(false) }
    var pmCompleted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val routineMap = mapOf(
        today.minusDays(1) to true,
        today.minusDays(3) to true,
        today.minusDays(6) to true
    )

    LaunchedEffect(refreshKey, externalRefreshKey) {
        val api = RetrofitClient.create(context)
        val todayStr = today.format(formatter)

        try {
            val now = YearMonth.now()

            val dateStrings = withContext(Dispatchers.IO) {
                loadCalendarDatesAroundMonth(
                    api = api,
                    centerMonth = now,
                    beforeMonths = 12,
                    afterMonths = 12
                )
            }

            calendarDates = dateStrings

            android.util.Log.d(
                "HOME_CALENDAR",
                "loaded calendar dates=${calendarDates.joinToString()}"
            )
        } catch (e: Exception) {
            android.util.Log.e("HOME_CALENDAR", "calendar dates load failed", e)
            calendarDates = emptyList()
        }

        try {
            val completionRes = withContext(Dispatchers.IO) {
                api.getRoutineCompletion(todayStr)
            }

            amCompleted = completionRes.AM
            pmCompleted = completionRes.PM
        } catch (e: Exception) {
            android.util.Log.e("HOME_COMPLETION", "completion load failed", e)
            amCompleted = false
            pmCompleted = false
        }

        amRoutineLoaded = false
        pmRoutineLoaded = false

        try {
            val amRes = withContext(Dispatchers.IO) {
                api.getRoutine("AM")
            }
            amRoutineItems = amRes.items.sortedBy { it.step_order }
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 404) {
                android.util.Log.d("HOME_ROUTINE", "AM routine not found")
            } else {
                android.util.Log.e("HOME_ROUTINE", "AM load failed", e)
            }
            amRoutineItems = emptyList()
        } finally {
            amRoutineLoaded = true
        }

        try {
            val pmRes = withContext(Dispatchers.IO) {
                api.getRoutine("PM")
            }
            pmRoutineItems = pmRes.items.sortedBy { it.step_order }
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 404) {
                android.util.Log.d("HOME_ROUTINE", "PM routine not found")
            } else {
                android.util.Log.e("HOME_ROUTINE", "PM load failed", e)
            }
            pmRoutineItems = emptyList()
        } finally {
            pmRoutineLoaded = true
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoutineAlertCard(
            amRoutineItems = amRoutineItems,
            pmRoutineItems = pmRoutineItems,
            amRoutineLoaded = amRoutineLoaded,
            pmRoutineLoaded = pmRoutineLoaded,
            amCompleted = amCompleted,
            pmCompleted = pmCompleted,
            onAmComplete = {
                val todayStr = today.format(formatter)
                coroutineScope.launch {
                    try {
                        val api = RetrofitClient.create(context)
                        withContext(Dispatchers.IO) {
                            api.saveRoutineCompletion(
                                RoutineCompletionRequest(
                                    date = todayStr,
                                    routine_type = "AM",
                                    is_completed = true
                                )
                            )
                        }
                        amCompleted = true
                    } catch (e: Exception) {
                        android.util.Log.e("HOME_COMPLETION", "save AM failed", e)
                    }
                }
            },
            onPmComplete = {
                val todayStr = today.format(formatter)
                coroutineScope.launch {
                    try {
                        val api = RetrofitClient.create(context)
                        withContext(Dispatchers.IO) {
                            api.saveRoutineCompletion(
                                RoutineCompletionRequest(
                                    date = todayStr,
                                    routine_type = "PM",
                                    is_completed = true
                                )
                            )
                        }
                        pmCompleted = true
                    } catch (e: Exception) {
                        android.util.Log.e("HOME_COMPLETION", "save PM failed", e)
                    }
                }
            },
            onAmCancel = {
                val todayStr = today.format(formatter)
                coroutineScope.launch {
                    try {
                        val api = RetrofitClient.create(context)
                        withContext(Dispatchers.IO) {
                            api.saveRoutineCompletion(
                                RoutineCompletionRequest(
                                    date = todayStr,
                                    routine_type = "AM",
                                    is_completed = false
                                )
                            )
                        }
                        amCompleted = false
                    } catch (e: Exception) {
                        android.util.Log.e("HOME_COMPLETION", "cancel AM failed", e)
                    }
                }
            },
            onPmCancel = {
                val todayStr = today.format(formatter)
                coroutineScope.launch {
                    try {
                        val api = RetrofitClient.create(context)
                        withContext(Dispatchers.IO) {
                            api.saveRoutineCompletion(
                                RoutineCompletionRequest(
                                    date = todayStr,
                                    routine_type = "PM",
                                    is_completed = false
                                )
                            )
                        }
                        pmCompleted = false
                    } catch (e: Exception) {
                        android.util.Log.e("HOME_COMPLETION", "cancel PM failed", e)
                    }
                }
            }
        )

        CalendarCard(
            routineDoneMap = routineMap,
            hasSkinRecordMap = calendarDates
                .mapNotNull { dateString ->
                    try {
                        LocalDate.parse(dateString) to true
                    } catch (e: Exception) {
                        null
                    }
                }
                .toMap(),
            selectedDate = selectedDate,
            onDateSelected = { clickedDate ->
                selectedDate = clickedDate
                onOpenCalendarDetail(clickedDate.format(formatter))
            }
        )
    }
}

private suspend fun loadCalendarDatesAroundMonth(
    api: ApiService,
    centerMonth: YearMonth,
    beforeMonths: Long,
    afterMonths: Long
): List<String> {
    val result = mutableSetOf<String>()

    for (offset in -beforeMonths..afterMonths) {
        val targetMonth = centerMonth.plusMonths(offset)

        try {
            val response = api.getCalendarDates(
                year = targetMonth.year,
                month = targetMonth.monthValue
            )

            response.dates.forEach { item ->
                result.add(item.date)
            }

            android.util.Log.d(
                "HOME_CALENDAR",
                "loaded ${targetMonth.year}-${targetMonth.monthValue}: ${response.dates.map { it.date }}"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "HOME_CALENDAR",
                "failed ${targetMonth.year}-${targetMonth.monthValue}",
                e
            )
        }
    }

    return result.sorted()
}

@Composable
private fun RoutineAlertCard(
    modifier: Modifier = Modifier,
    amRoutineItems: List<RoutineItemDto>,
    pmRoutineItems: List<RoutineItemDto>,
    amRoutineLoaded: Boolean,
    pmRoutineLoaded: Boolean,
    amCompleted: Boolean,
    pmCompleted: Boolean,
    onAmComplete: () -> Unit,
    onPmComplete: () -> Unit,
    onAmCancel: () -> Unit,
    onPmCancel: () -> Unit
) {
    val completedCount = listOf(amCompleted, pmCompleted).count { it }

    val frogImage = when (completedCount) {
        0 -> R.drawable.frog_skin_dry
        1 -> R.drawable.frog_skin_nice
        else -> R.drawable.frog_skin_glow
    }

    Surface(
        modifier = modifier
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "오늘 루틴 요약",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrogStatusCard(
                    modifier = Modifier.weight(1f),
                    imageRes = frogImage
                )

                RoutineMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "☀",
                    iconColor = YellowText,
                    cardTint = YellowSoft,
                    title = "아침 루틴",
                    stepText = buildRoutineStepText(amRoutineItems, amRoutineLoaded),
                    completed = amCompleted,
                    onCardClick = {
                        if (amCompleted) {
                            onAmCancel()
                        } else {
                            onAmComplete()
                        }
                    }
                )

                RoutineMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "☾",
                    iconColor = PurpleText,
                    cardTint = PurpleSoft,
                    title = "저녁 루틴",
                    stepText = buildRoutineStepText(pmRoutineItems, pmRoutineLoaded),
                    completed = pmCompleted,
                    onCardClick = {
                        if (pmCompleted) {
                            onPmCancel()
                        } else {
                            onPmComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FrogStatusCard(
    modifier: Modifier = Modifier,
    imageRes: Int
) {
    Surface(
        modifier = modifier.height(162.dp),
        shape = RoundedCornerShape(20.dp),
        color = GreenSoft,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun RoutineMiniCard(
    modifier: Modifier = Modifier,
    icon: String,
    iconColor: Color,
    cardTint: Color,
    title: String,
    stepText: String,
    completed: Boolean,
    onCardClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(162.dp)
            .clickable {
                onCardClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = cardTint,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                color = iconColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (completed) "완료" else "미완료",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (completed) GreenPrimary else iconColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stepText,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

private fun buildRoutineStepText(
    items: List<RoutineItemDto>,
    loaded: Boolean
): String {
    return when {
        !loaded -> "불러오는 중"
        items.isEmpty() -> "루틴 없음"
        else -> "${items.size}단계"
    }
}