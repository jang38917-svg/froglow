package com.froglow.app.ui.screens.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SkinChoice { DRY, OILY, COMBINATION, UNKNOWN }

@Composable
fun SurveyGateScreen(
    onPickKnownType: (String) -> Unit, // "DRY" | "OILY" | "COMBINATION"
    onPickUnknown: () -> Unit
) {
    var selected by remember { mutableStateOf<SkinChoice?>(null) }

    // ✅ 밝고 깨끗한 개구리(민트) 톤
    val mint = androidx.compose.ui.graphics.Color(0xFF2CCB93)        // 포인트(버튼/체크)
    val mintSoft = androidx.compose.ui.graphics.Color(0xFFE9FFF6)    // 선택 배경
    val cardBg = androidx.compose.ui.graphics.Color(0xFFF7FFFC)      // 카드 기본 배경(아주 연한 민트)
    val border = androidx.compose.ui.graphics.Color(0xFFD7EEE6)      // 테두리 연하게
    val screenBg = androidx.compose.ui.graphics.Color(0xFFFFFFFF)    // 완전 화이트

    val titleStyle = MaterialTheme.typography.headlineMedium.merge(
        TextStyle(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ✅ 제목: 짧고 예쁘게 (줄바꿈 깨짐 방지)
        Text(
            text = "피부 타입을 선택해주세요",
            style = titleStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // ✅ 요청한 문구로 변경
        Text(
            text = "피부 타입을 모를 경우 '모르겠음'을 선택해주세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        ChoiceCardClean(
            title = "건성",
            subtitle = "세안 후 당김이 있고 쉽게 건조해져요",
            selected = selected == SkinChoice.DRY,
            onClick = { selected = SkinChoice.DRY },
            mint = mint,
            mintSoft = mintSoft,
            cardBg = cardBg,
            border = border
        )

        ChoiceCardClean(
            title = "지성",
            subtitle = "유분이 많고 번들거림이 자주 느껴져요",
            selected = selected == SkinChoice.OILY,
            onClick = { selected = SkinChoice.OILY },
            mint = mint,
            mintSoft = mintSoft,
            cardBg = cardBg,
            border = border
        )

        ChoiceCardClean(
            title = "복합성",
            subtitle = "T존은 번들, 볼은 건조한 편이에요",
            selected = selected == SkinChoice.COMBINATION,
            onClick = { selected = SkinChoice.COMBINATION },
            mint = mint,
            mintSoft = mintSoft,
            cardBg = cardBg,
            border = border
        )

        ChoiceCardClean(
            title = "모르겠음",
            subtitle = "간단한 설문으로 피부 타입을 찾아드릴게요",
            selected = selected == SkinChoice.UNKNOWN,
            onClick = { selected = SkinChoice.UNKNOWN },
            mint = mint,
            mintSoft = mintSoft,
            cardBg = cardBg,
            border = border
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                when (selected) {
                    SkinChoice.DRY -> onPickKnownType("DRY")
                    SkinChoice.OILY -> onPickKnownType("OILY")
                    SkinChoice.COMBINATION -> onPickKnownType("COMBINATION")
                    SkinChoice.UNKNOWN -> onPickUnknown()
                    null -> {}
                }
            },
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = mint,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = androidx.compose.ui.graphics.Color(0xFFE7F6F0),
                disabledContentColor = androidx.compose.ui.graphics.Color(0xFF9CBEB2)
            )
        ) {
            Text("확인", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // ✅ “선택 후 확인…” 문구 제거 (요청 반영)
    }
}

@Composable
private fun ChoiceCardClean(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    mint: androidx.compose.ui.graphics.Color,
    mintSoft: androidx.compose.ui.graphics.Color,
    cardBg: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color
) {
    val shape = RoundedCornerShape(18.dp)
    val bg = if (selected) mintSoft else cardBg
    val borderColor = if (selected) mint else border

    Surface(
        onClick = onClick,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ 라디오 느낌 (깨끗하게)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(mint)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
