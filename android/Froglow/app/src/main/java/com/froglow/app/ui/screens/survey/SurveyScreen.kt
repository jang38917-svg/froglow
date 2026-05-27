package com.froglow.app.ui.screens.survey

import android.util.Log
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.froglow.app.network.SurveyOption
import com.froglow.app.network.SurveyQuestion

private val ScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)
private val DisabledGreen = Color(0xFFDDEAD6)

private val ErrorRed = Color(0xFFE95F62)
private val ErrorSoft = Color(0xFFFFEFEF)

@Composable
fun SurveyScreen(
    onNavigateToCamera: () -> Unit,
    viewModel: SurveyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var currentIndex by remember { mutableIntStateOf(0) }

    val sortedQuestions = state.questions.sortedBy { it.order }
    val totalCount = sortedQuestions.size
    val safeCurrentIndex = currentIndex.coerceIn(0, (totalCount - 1).coerceAtLeast(0))
    val currentQuestion = sortedQuestions.getOrNull(safeCurrentIndex)

    val currentSelectedOptionId = currentQuestion?.let {
        state.selectedOptionIds[it.id]
    }

    val isLastQuestion = totalCount > 0 && safeCurrentIndex == totalCount - 1
    val canMoveNext = currentSelectedOptionId != null && !state.loading

    LaunchedEffect(totalCount) {
        if (totalCount > 0 && currentIndex > totalCount - 1) {
            currentIndex = totalCount - 1
        }
    }

    LaunchedEffect(state.navigateToCamera) {
        Log.d("SURVEY_DEBUG", "navigateToCamera 변경 감지 = ${state.navigateToCamera}")

        if (state.navigateToCamera) {
            Log.d("SURVEY_DEBUG", "카메라 이동 콜백 호출")
            onNavigateToCamera()
            viewModel.onCameraNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        SurveyTopProgress(
            currentIndex = safeCurrentIndex,
            totalCount = totalCount
        )

        Spacer(modifier = Modifier.height(34.dp))

        if (state.loading && state.questions.isEmpty()) {
            LoadingCard()
        } else if (currentQuestion != null) {
            Text(
                text = currentQuestion.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "피부 상태에 가장 가까운 답변을 골라주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            currentQuestion.options.forEach { option ->
                SurveyOptionCard(
                    option = option,
                    selected = currentSelectedOptionId == option.id,
                    onClick = {
                        viewModel.selectOption(currentQuestion.id, option.id)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (!canMoveNext) return@Button

                    if (isLastQuestion) {
                        Log.d("SURVEY_DEBUG", "설문 완료 버튼 클릭")
                        viewModel.submitSurvey()
                    } else {
                        currentIndex += 1
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = canMoveNext,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = DisabledGreen,
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLastQuestion) "설문 완료" else "다음",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ErrorSoft,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = state.error ?: "",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            EmptySurveyCard()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SurveyTopProgress(
    currentIndex: Int,
    totalCount: Int
) {
    val safeTotal = totalCount.coerceAtLeast(1)
    val displayCurrent = if (totalCount == 0) 0 else currentIndex + 1
    val progress = displayCurrent.toFloat() / safeTotal.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(7.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFEDE6DA),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(GreenPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Text(
                text = "$displayCurrent/$totalCount",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SurveyOptionCard(
    option: SurveyOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (selected) GreenSoft else CardBg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) GreenPrimary else CardBorder
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = PrimaryText
            )

            if (selected) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = GreenPrimary,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "선택됨",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
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
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = GreenPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "설문 질문을 불러오는 중이에요.",
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptySurveyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Text(
            text = "설문 질문이 없습니다.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}