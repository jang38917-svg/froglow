package com.froglow.app.ui.screens.skin_type

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.SurveySubmitRequest
import kotlinx.coroutines.launch

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

data class SkinOption(
    val title: String,
    val desc: String,
    val emoji: String
)

private val CARD_OPTIONS = listOf(
    SkinOption("건성", "피부가 쉽게 건조해요", "💧"),
    SkinOption("지성", "피지가 많이 올라와요", "🧴"),
    SkinOption("복합성", "부위마다 피부 상태가 달라요", "⚖️"),
    SkinOption("모르겠습니다", "잘 모르겠어요", "🤷")
)

@Composable
fun SkinTypeScreen(
    onConfirm: (SkinTypeChoice) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<SkinOption?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val api = RetrofitClient.create(context)
            api.getPresetOptions()
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 26.dp)
    ) {
        Text(
            text = "어떤 피부 타입에 가까우신가요?",
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "피부 상태에 맞는 루틴과 제품 추천을 위해 필요해요.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (loading) {
            LoadingCard()
        } else {
            CARD_OPTIONS.forEach { option ->
                val isSelected = selected == option

                SkinOptionCard(
                    option = option,
                    selected = isSelected,
                    enabled = !saving,
                    onClick = {
                        selected = option
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = ErrorSoft,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = error ?: "",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Button(
            enabled = selected != null && !saving,
            onClick = {
                val opt = selected ?: return@Button

                if (opt.title == "모르겠습니다") {
                    onConfirm(SkinTypeChoice.UNKNOWN)
                    return@Button
                }

                val choice = when (opt.title) {
                    "건성" -> SkinTypeChoice.DRY
                    "지성" -> SkinTypeChoice.OILY
                    "복합성" -> SkinTypeChoice.COMBINATION
                    else -> return@Button
                }

                scope.launch {
                    saving = true
                    error = null

                    try {
                        val api = RetrofitClient.create(context)

                        api.submitSurvey(
                            SurveySubmitRequest(
                                preset_skin_type = opt.title,
                                answers = emptyList()
                            )
                        )

                        onConfirm(choice)
                    } catch (e: Exception) {
                        error = e.message ?: "저장 실패"
                    } finally {
                        saving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 18.dp)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                contentColor = Color.White,
                disabledContainerColor = DisabledGreen,
                disabledContentColor = Color.White.copy(alpha = 0.85f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = "저장중...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Text(
                    text = "선택 완료",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SkinOptionCard(
    option: SkinOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        color = if (selected) GreenSoft else CardBg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) GreenPrimary else CardBorder
        ),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (selected) 40.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = if (selected) Color(0xFFF3F8EE) else Color(0xFFF8F5EF),
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.emoji,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = option.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Text(
                        text = option.desc,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp),
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
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
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
        shape = RoundedCornerShape(22.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = GreenPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "피부 타입 정보를 불러오는 중이에요.",
                color = SecondaryText,
                fontSize = 13.sp
            )
        }
    }
}