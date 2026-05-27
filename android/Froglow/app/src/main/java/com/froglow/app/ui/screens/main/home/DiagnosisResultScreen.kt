package com.froglow.app.ui.screens.main.home

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.froglow.app.network.DiagnosisAnalyzeResponse
import com.froglow.app.network.RecommendationDto
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.RoutineItemDto
import com.froglow.app.network.RoutineProductDto
import com.froglow.app.network.RoutineRequest
import com.froglow.app.network.RoutineStepDto
import com.froglow.app.ui.screens.camera.CapturedPhotoTempStore
import com.froglow.app.ui.screens.main.tabs.RoutineProgressStore
import com.froglow.app.ui.screens.main.tabs.RoutineRefreshStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardSoftBg = Color(0xFFFFFCF7)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)
private val DisabledGreen = Color(0xFFDDEAD6)

private val YellowSoft = Color(0xFFFFF4DD)

private val MorningSelectedBg = Color(0xFFFFF4DD)
private val MorningSelectedText = Color(0xFFD79A27)
private val MorningSelectedBorder = Color(0xFFEBD9A8)

private val NightSelectedBg = Color(0xFFF3EDFF)
private val NightSelectedText = Color(0xFF7B63B8)
private val NightSelectedBorder = Color(0xFFE2D7F5)

private val ErrorSoft = Color(0xFFFFEFEF)
private val ErrorText = Color(0xFFE95F62)

private enum class RoutineTab {
    MORNING, NIGHT
}

@Composable
fun DiagnosisResultScreen(
    selectedDateString: String?,
    routineMode: String = "normal",
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRoutineChangeMode = routineMode == "routine_change"

    val capturedPhotos = remember { CapturedPhotoTempStore.getPhotos() }
    val photoUris = remember(capturedPhotos) { capturedPhotos }

    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    var selectedDate by remember(selectedDateString) {
        mutableStateOf(parseSelectedDateOrToday(selectedDateString))
    }

    var selectedRoutineTab by remember { mutableStateOf(RoutineTab.MORNING) }

    var diagnosisResult by remember { mutableStateOf<DiagnosisAnalyzeResponse?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var hasAutoAnalyzed by remember { mutableStateOf(false) }

    var existingAMRoutine by remember { mutableStateOf<List<RoutineItemDto>?>(null) }
    var existingPMRoutine by remember { mutableStateOf<List<RoutineItemDto>?>(null) }
    var routineCheckDone by remember { mutableStateOf(false) }

    val selectedMorningByStep = remember { mutableStateMapOf<Int, Set<Long>>() }
    val selectedNightByStep = remember { mutableStateMapOf<Int, Set<Long>>() }

    val photoLabels = listOf("정면 얼굴", "좌측 얼굴", "우측 얼굴")

    val acneScore = diagnosisResult?.diagnosis?.acne
    val rednessScore = diagnosisResult?.diagnosis?.redness
    val spotScore = diagnosisResult?.diagnosis?.spot
    val wrinkleScore = diagnosisResult?.diagnosis?.wrinkle

    val primaryConcern = diagnosisResult?.primary_concern ?: "-"
    val primaryConcernLabel = concernToKorean(primaryConcern)

    val recommendation = diagnosisResult?.recommendation

    val isFirstDiagnosis = routineCheckDone &&
            existingAMRoutine.isNullOrEmpty() &&
            existingPMRoutine.isNullOrEmpty()

    val shouldShowRoutineSelection = isFirstDiagnosis || isRoutineChangeMode

    suspend fun callAnalyze(): DiagnosisAnalyzeResponse {
        val api = RetrofitClient.create(context)

        val frontPart = createImagePart(context, photoUris[0], "front")
        val leftPart = createImagePart(context, photoUris[1], "left")
        val rightPart = createImagePart(context, photoUris[2], "right")

        return withContext(Dispatchers.IO) {
            if (isRoutineChangeMode || isFirstDiagnosis) {
                Log.d("ANALYZE_RESULT", "루틴 변경/첫 진단 모드 → preview API 호출")
                api.analyzeSkinPreview(
                    front = frontPart,
                    left = leftPart,
                    right = rightPart
                )
            } else {
                val recordDateBody = selectedDate.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                Log.d("ANALYZE_RESULT", "일반 진단 모드 → save analyze API 호출")
                api.analyzeSkin(
                    front = frontPart,
                    left = leftPart,
                    right = rightPart,
                    recordDate = recordDateBody
                )
            }
        }
    }

    fun syncDefaultSelections(recommendationDto: RecommendationDto?) {
        selectedMorningByStep.clear()
        selectedNightByStep.clear()

        recommendationDto?.morning_routine?.forEachIndexed { index, step ->
            step.items.firstOrNull()?.let { first ->
                selectedMorningByStep[index] = setOf(first.product_id)
            }
        }

        recommendationDto?.evening_routine?.forEachIndexed { index, step ->
            step.items.firstOrNull()?.let { first ->
                selectedNightByStep[index] = setOf(first.product_id)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val api = RetrofitClient.create(context)

            val am = withContext(Dispatchers.IO) {
                api.getRoutine("AM")
            }

            val pm = withContext(Dispatchers.IO) {
                api.getRoutine("PM")
            }

            existingAMRoutine = am.items
            existingPMRoutine = pm.items

            Log.d("ROUTINE_CHECK", "기존 AM 루틴 = ${am.items}")
            Log.d("ROUTINE_CHECK", "기존 PM 루틴 = ${pm.items}")
        } catch (e: Exception) {
            existingAMRoutine = emptyList()
            existingPMRoutine = emptyList()
            Log.d("ROUTINE_CHECK", "기존 루틴 없음 → 첫 진단으로 처리")
        } finally {
            routineCheckDone = true
        }
    }

    LaunchedEffect(photoUris, routineCheckDone) {
        if (!routineCheckDone) return@LaunchedEffect

        if (!hasAutoAnalyzed && photoUris.size >= 3) {
            hasAutoAnalyzed = true
            isAnalyzing = true
            apiError = null

            try {
                val result = callAnalyze()
                diagnosisResult = result

                if (shouldShowRoutineSelection) {
                    syncDefaultSelections(result.recommendation)
                }

                Log.d("ANALYZE_RESULT", result.toString())
                Log.d("ANALYZE_RESULT", "recordDate = $selectedDate, mode=$routineMode")
            } catch (e: Exception) {
                Log.e("ANALYZE_RESULT", "auto analyze failed", e)
                apiError = e.message ?: "자동 분석 실패"
            } finally {
                isAnalyzing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ResultTopBar(
            title = if (isRoutineChangeMode) "루틴 변경 진단" else "피부 진단 결과",
            onBack = onBack
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PhotoPreviewSection(
                    photoUris = photoUris,
                    currentPhotoIndex = currentPhotoIndex,
                    onPrev = {
                        if (currentPhotoIndex > 0) currentPhotoIndex -= 1
                    },
                    onNext = {
                        if (currentPhotoIndex < 2 && currentPhotoIndex < photoUris.lastIndex) {
                            currentPhotoIndex += 1
                        }
                    },
                    photoLabels = photoLabels
                )

                if (!isRoutineChangeMode) {
                    DateSection(selectedDate = selectedDate)
                }

                if (!routineCheckDone || isAnalyzing) {
                    LoadingStatusCard(
                        text = if (!routineCheckDone) {
                            "기존 루틴 확인 중입니다..."
                        } else {
                            "피부 분석 중입니다..."
                        }
                    )
                }

                AnalysisSection(
                    acneScore = acneScore,
                    rednessScore = rednessScore,
                    spotScore = spotScore,
                    wrinkleScore = wrinkleScore,
                    primaryConcern = primaryConcernLabel
                )

                RoutineNoticeCard(
                    text = when {
                        isRoutineChangeMode -> "현재 피부 상태에 맞춰 루틴을 다시 추천합니다"
                        isFirstDiagnosis -> "당신의 주요 고민 \"$primaryConcernLabel\" 에 맞춘 첫 추천 루틴"
                        else -> buildSkinConditionMessage(
                            acneScore = acneScore,
                            rednessScore = rednessScore,
                            spotScore = spotScore,
                            wrinkleScore = wrinkleScore
                        )
                    },
                    highlight = shouldShowRoutineSelection
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoutineTabSelector(
                        selectedRoutineTab = selectedRoutineTab,
                        onSelect = { selectedRoutineTab = it }
                    )

                    if (shouldShowRoutineSelection) {
                        if (selectedRoutineTab == RoutineTab.MORNING) {
                            RoutineMultiSelectionSection(
                                title = "아침 루틴 추천",
                                emoji = "☀️",
                                steps = recommendation?.morning_routine.orEmpty(),
                                selectedByStep = selectedMorningByStep,
                                emptyMessage = "아침 루틴 추천 결과가 아직 없습니다."
                            )
                        } else {
                            RoutineMultiSelectionSection(
                                title = "저녁 루틴 추천",
                                emoji = "🌙",
                                steps = recommendation?.evening_routine.orEmpty(),
                                selectedByStep = selectedNightByStep,
                                emptyMessage = "저녁 루틴 추천 결과가 아직 없습니다."
                            )
                        }
                    } else {
                        if (selectedRoutineTab == RoutineTab.MORNING) {
                            ExistingRoutineSection(
                                title = "저장된 아침 루틴",
                                emoji = "☀️",
                                items = existingAMRoutine.orEmpty()
                            )
                        } else {
                            ExistingRoutineSection(
                                title = "저장된 저녁 루틴",
                                emoji = "🌙",
                                items = existingPMRoutine.orEmpty()
                            )
                        }
                    }
                }

                apiError?.let { error ->
                    ErrorCard(error)
                }

                ActionButtons(
                    isSaving = isSaving,
                    isAnalyzing = isAnalyzing,
                    routineCheckDone = routineCheckDone,
                    isRoutineChangeMode = isRoutineChangeMode,
                    isFirstDiagnosis = isFirstDiagnosis,
                    onPrimaryClick = {
                        if (photoUris.size < 3) {
                            Toast.makeText(context, "사진 3장이 필요합니다.", Toast.LENGTH_SHORT).show()
                            return@ActionButtons
                        }

                        scope.launch {
                            isSaving = true
                            apiError = null

                            try {
                                val api = RetrofitClient.create(context)

                                if (shouldShowRoutineSelection) {
                                    val morningProductIds = flattenMultiSelectedProductIds(
                                        steps = recommendation?.morning_routine.orEmpty(),
                                        selectedByStep = selectedMorningByStep
                                    )

                                    val nightProductIds = flattenMultiSelectedProductIds(
                                        steps = recommendation?.evening_routine.orEmpty(),
                                        selectedByStep = selectedNightByStep
                                    )

                                    Log.d("ROUTINE_CHECK", "루틴 저장 AM selected = $morningProductIds")
                                    Log.d("ROUTINE_CHECK", "루틴 저장 PM selected = $nightProductIds")

                                    if (morningProductIds.isNotEmpty()) {
                                        withContext(Dispatchers.IO) {
                                            api.saveRoutine(
                                                RoutineRequest(
                                                    routine_type = "AM",
                                                    product_ids = morningProductIds
                                                )
                                            )
                                        }
                                    }

                                    if (nightProductIds.isNotEmpty()) {
                                        withContext(Dispatchers.IO) {
                                            api.saveRoutine(
                                                RoutineRequest(
                                                    routine_type = "PM",
                                                    product_ids = nightProductIds
                                                )
                                            )
                                        }
                                    }

                                    val amReadBack = withContext(Dispatchers.IO) {
                                        api.getRoutine("AM")
                                    }

                                    val pmReadBack = withContext(Dispatchers.IO) {
                                        api.getRoutine("PM")
                                    }

                                    if (isFirstDiagnosis) {
                                        val result = diagnosisResult

                                        if (result?.folder_id != null) {
                                            withContext(Dispatchers.IO) {
                                                api.saveDiagnosis(
                                                    folderId = result.folder_id,
                                                    acne = result.diagnosis.acne,
                                                    redness = result.diagnosis.redness,
                                                    wrinkle = result.diagnosis.wrinkle,
                                                    spot = result.diagnosis.spot,
                                                    primaryConcern = result.primary_concern,
                                                    skinType = null,
                                                    recordDate = selectedDate.toString()
                                                )
                                            }

                                            Log.d("DIAGNOSIS_SAVE", "첫 진단 save 완료 date=$selectedDate")
                                        } else {
                                            throw IllegalStateException("첫 진단 저장 실패: folder_id가 없습니다.")
                                        }
                                    }

                                    Log.d("ROUTINE_CHECK", "AM readback = ${amReadBack.items}")
                                    Log.d("ROUTINE_CHECK", "PM readback = ${pmReadBack.items}")

                                    Toast.makeText(
                                        context,
                                        if (isRoutineChangeMode) "루틴 변경 완료" else "첫 루틴 저장 완료",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Log.d("ROUTINE_CHECK", "기존 루틴 유지 → 루틴 저장 생략")
                                    Toast.makeText(context, "진단 기록 저장 완료", Toast.LENGTH_SHORT).show()
                                }

                                RoutineProgressStore.clearToday(context)
                                RoutineRefreshStore.refresh()
                                onGoHome()
                            } catch (e: Exception) {
                                Log.e("ROUTINE_SAVE", "save routine failed", e)
                                apiError = e.message ?: "저장 실패"
                                Toast.makeText(
                                    context,
                                    e.message ?: "저장 실패",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    onSecondaryClick = onBack
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ResultTopBar(
    title: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ScreenBg,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = PrimaryText
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryText
            )
        }
    }
}

@Composable
private fun PhotoPreviewSection(
    photoUris: List<String>,
    currentPhotoIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    photoLabels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF6F1E6))
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
        ) {
            if (photoUris.isNotEmpty() && currentPhotoIndex in photoUris.indices) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { imageView ->
                        imageView.setImageURI(photoUris[currentPhotoIndex].toUri())
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "사진이 없습니다.",
                        color = SecondaryText
                    )
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
                    text = photoLabels.getOrElse(currentPhotoIndex) { "사진" },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onPrev,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "이전",
                    tint = PrimaryText
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "다음",
                    tint = PrimaryText
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            photoLabels.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (currentPhotoIndex == index) 8.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentPhotoIndex == index) GreenPrimary else CardBorder
                        )
                )
            }
        }
    }
}

@Composable
private fun DateSection(selectedDate: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "진단 기록 날짜",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CardSoftBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(formatter),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryText
                )

                Text(
                    text = "선택 완료",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingStatusCard(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GreenSoft,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = GreenPrimary,
                strokeWidth = 2.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnalysisSection(
    acneScore: Int?,
    rednessScore: Int?,
    spotScore: Int?,
    wrinkleScore: Int?,
    primaryConcern: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                ScoreRow("여드름", acneScore)
                ScoreRow("홍조", rednessScore)
                ScoreRow("잡티", spotScore)
                ScoreRow("주름", wrinkleScore)

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = CardBorder)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "주요 고민: $primaryConcern",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int?) {
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
                text = score?.let { "${it}점" } ?: "-",
                color = SecondaryText
            )

            Text(
                text = score?.let { scoreToGrade(it) } ?: "-",
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RoutineNoticeCard(
    text: String,
    highlight: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (highlight) YellowSoft else GreenSoft,
        border = BorderStroke(
            1.dp,
            if (highlight) Color(0xFFEBD9A8) else Color(0xFFD3E4C8)
        ),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = PrimaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RoutineTabSelector(
    selectedRoutineTab: RoutineTab,
    onSelect: (RoutineTab) -> Unit
) {
    val morningSelected = selectedRoutineTab == RoutineTab.MORNING
    val nightSelected = selectedRoutineTab == RoutineTab.NIGHT

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF6F1E6),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(RoutineTab.MORNING) },
                shape = RoundedCornerShape(999.dp),
                color = if (morningSelected) MorningSelectedBg else Color.Transparent,
                border = if (morningSelected) BorderStroke(1.dp, MorningSelectedBorder) else null,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☀️ 아침 루틴",
                        color = if (morningSelected) MorningSelectedText else SecondaryText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(RoutineTab.NIGHT) },
                shape = RoundedCornerShape(999.dp),
                color = if (nightSelected) NightSelectedBg else Color.Transparent,
                border = if (nightSelected) BorderStroke(1.dp, NightSelectedBorder) else null,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌙 저녁 루틴",
                        color = if (nightSelected) NightSelectedText else SecondaryText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
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

@Composable
private fun ActionButtons(
    isSaving: Boolean,
    isAnalyzing: Boolean,
    routineCheckDone: Boolean,
    isRoutineChangeMode: Boolean,
    isFirstDiagnosis: Boolean,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isSaving && !isAnalyzing && routineCheckDone) {
                    onPrimaryClick()
                },
            shape = RoundedCornerShape(20.dp),
            color = if (!isSaving && !isAnalyzing && routineCheckDone) GreenPrimary else DisabledGreen,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            isRoutineChangeMode -> "선택한 루틴으로 변경하고 홈으로"
                            isFirstDiagnosis -> "선택한 첫 루틴 저장하고 홈으로"
                            else -> "진단 완료하고 홈으로"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSecondaryClick() },
            shape = RoundedCornerShape(20.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "다시 촬영하기",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
        }
    }
}

@Composable
private fun RoutineMultiSelectionSection(
    title: String,
    emoji: String,
    steps: List<RoutineStepDto>,
    selectedByStep: MutableMap<Int, Set<Long>>,
    emptyMessage: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$emoji $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (steps.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardSoftBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            steps.forEachIndexed { index, step ->
                RoutineMultiSelectionGroupCard(
                    stepNo = index + 1,
                    stepIndex = index,
                    step = step,
                    selectedSet = selectedByStep[index].orEmpty(),
                    onToggleProduct = { productId ->
                        val current = selectedByStep[index].orEmpty()

                        selectedByStep[index] =
                            if (current.contains(productId)) {
                                current - productId
                            } else {
                                current + productId
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun RoutineMultiSelectionGroupCard(
    stepNo: Int,
    stepIndex: Int,
    step: RoutineStepDto,
    selectedSet: Set<Long>,
    onToggleProduct: (Long) -> Unit
) {
    val firstProductName = step.items.firstOrNull()?.product_name
    val stepTitle = step.step_name?.takeIf { it.isNotBlank() }
        ?: inferRoutineTypeLabel(step.description, firstProductName)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardSoftBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GreenSoft,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "${stepNo}단계",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Text(
                        text = if (selectedSet.isEmpty()) {
                            "선택된 제품 없음"
                        } else {
                            "${selectedSet.size}개 선택됨"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )

                    if (!step.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            if (step.items.isEmpty()) {
                Text(
                    text = "추천 상품이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    step.items.forEach { product ->
                        MultiSelectableProductCard(
                            product = product,
                            selected = selectedSet.contains(product.product_id),
                            onClick = { onToggleProduct(product.product_id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSelectableProductCard(
    product: RoutineProductDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) GreenPrimary else CardBorder
    val bgColor = if (selected) GreenSoft else CardBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = product.product_image_url,
            contentDescription = product.product_name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF6F1E6)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.product_name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = PrimaryText
            )

            product.formulation?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            product.price?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }

        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() },
            colors = CheckboxDefaults.colors(
                checkedColor = GreenPrimary,
                uncheckedColor = SecondaryText
            )
        )
    }
}

@Composable
private fun ExistingRoutineSection(
    title: String,
    emoji: String,
    items: List<RoutineItemDto>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$emoji $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (items.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardSoftBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "저장된 루틴이 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.sortedBy { it.step_order }.forEach { product ->
                ExistingRoutineProductCard(product = product)
            }
        }
    }
}

@Composable
private fun ExistingRoutineProductCard(
    product: RoutineItemDto
) {
    val imageUrl = normalizeImageUrl(product.product_image_url)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = product.product_name ?: "제품 이미지",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF6F1E6)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.step_name?.takeIf { it.isNotBlank() }
                    ?: inferRoutineTypeLabel(null, product.product_name ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = product.product_name ?: "제품명 없음",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = PrimaryText
            )
        }
    }
}

private fun parseSelectedDateOrToday(dateString: String?): LocalDate {
    return try {
        if (dateString.isNullOrBlank()) LocalDate.now()
        else LocalDate.parse(dateString)
    } catch (e: Exception) {
        LocalDate.now()
    }
}

private fun createImagePart(context: Context, uriString: String, name: String): MultipartBody.Part {
    val uri = Uri.parse(uriString)
    val file = File(context.cacheDir, "$name.jpg")

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }

    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(name, file.name, requestFile)
}

private fun flattenMultiSelectedProductIds(
    steps: List<RoutineStepDto>,
    selectedByStep: Map<Int, Set<Long>>
): List<Long> {
    val result = mutableListOf<Long>()

    steps.forEachIndexed { index, step ->
        val selectedSet = selectedByStep[index].orEmpty()

        step.items.forEach { item ->
            if (selectedSet.contains(item.product_id)) {
                result.add(item.product_id)
            }
        }
    }

    return result
}

private fun inferRoutineTypeLabel(
    description: String?,
    productName: String?
): String {
    val text = listOfNotNull(description, productName)
        .joinToString(" ")
        .uppercase()

    return when {
        text.contains("SUN") || text.contains("선크림") || text.contains("자외선") -> "선크림"
        text.contains("CLEANSER") || text.contains("클렌저") || text.contains("세안") -> "클렌저"
        text.contains("TONER PAD") || text.contains("토너패드") || text.contains("패드") -> "토너패드"
        text.contains("TONER") || text.contains("토너") || text.contains("스킨") -> "토너"
        text.contains("AMPOULE") || text.contains("앰플") -> "앰플"
        text.contains("ESSENCE") || text.contains("에센스") -> "에센스"
        text.contains("SERUM") || text.contains("세럼") -> "세럼"
        text.contains("LOTION") || text.contains("로션") -> "로션"
        text.contains("CREAM") || text.contains("크림") -> "크림"
        text.contains("MASK") || text.contains("팩") -> "마스크팩"
        else -> "추천 제품"
    }
}

private fun scoreToGrade(score: Int): String {
    return when {
        score <= 2 -> "A"
        score <= 4 -> "B"
        score <= 6 -> "C"
        score <= 8 -> "D"
        else -> "E"
    }
}

private fun normalizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null

    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

private fun concernToKorean(value: String?): String {
    val normalized = value
        ?.trim()
        ?.lowercase()
        ?: return "-"

    return when (normalized) {
        "acne" -> "여드름"
        "redness" -> "홍조"
        "spot", "spots", "pigmentation" -> "잡티"
        "wrinkle", "wrinkles" -> "주름"
        else -> value.ifBlank { "-" }
    }
}

private fun buildSkinConditionMessage(
    acneScore: Int?,
    rednessScore: Int?,
    spotScore: Int?,
    wrinkleScore: Int?
): String {
    val scores = listOfNotNull(acneScore, rednessScore, spotScore, wrinkleScore)

    if (scores.isEmpty()) {
        return "피부 점수는 낮을수록 좋은 상태를 의미해요."
    }

    val average = scores.average()

    return when {
        average <= 3.0 -> {
            "피부 점수는 낮을수록 좋은 상태예요. 오늘은 전반적으로 피부 상태가 안정적인 편이에요."
        }

        average <= 6.0 -> {
            "피부 점수는 낮을수록 좋은 상태예요. 오늘은 일부 항목만 가볍게 관리해주면 좋아요."
        }

        else -> {
            "피부 점수는 낮을수록 좋은 상태예요. 오늘은 피부 컨디션을 조금 더 신경 써주세요."
        }
    }
}