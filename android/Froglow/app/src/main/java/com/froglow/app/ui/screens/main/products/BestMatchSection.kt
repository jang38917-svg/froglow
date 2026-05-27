package com.froglow.app.ui.screens.main.products

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val ImageBg = Color(0xFFFFFEFC)
private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val Rank1Bg = Color(0xFFEAF4E2)
private val Rank2Bg = Color(0xFFF3F0EA)
private val Rank3Bg = Color(0xFFFFF4DD)

private val Rank1Text = Color(0xFF77A95B)
private val Rank2Text = Color(0xFF7C756C)
private val Rank3Text = Color(0xFFD79A27)

@Composable
fun BestMatchSection(
    title: String,
    subtitle: String,
    items: List<ProductUi>,
    pagerState: PagerState,
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (items.isEmpty()) {
            EmptyBestMatchCard()
            return
        }

        LaunchedEffect(items.size) {
            val max = items.size - 1
            if (pagerState.currentPage > max) {
                pagerState.scrollToPage(0)
            }
        }

        HorizontalPager(
            state = pagerState
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BestMatchCard(
                    product = item,
                    rank = page + 1,
                    onClick = { onClick(item.id.toLong()) },
                    modifier = Modifier.fillMaxWidth(0.78f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DotsIndicator(
            total = items.size,
            current = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BestMatchCard(
    product: ProductUi,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(0.78f)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ImageBg,
                    border = BorderStroke(1.dp, CardBorder),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                RankBadge(
                    rank = rank,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "추천 점수 ${formatBestScore(product.rating)}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            BestMatchTags(
                tags = toTags(product.subTag)
            )
        }
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val bgColor = when (rank) {
        1 -> Rank1Bg
        2 -> Rank2Bg
        3 -> Rank3Bg
        else -> GreenSoft
    }

    val textColor = when (rank) {
        1 -> Rank1Text
        2 -> Rank2Text
        3 -> Rank3Text
        else -> GreenPrimary
    }

    val label = when (rank) {
        1 -> "BEST 1"
        2 -> "BEST 2"
        3 -> "BEST 3"
        else -> "BEST"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun BestMatchTags(
    tags: List<String>
) {
    val displayTags = if (tags.isEmpty()) {
        listOf("#여드름", "#저자극")
    } else {
        tags
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        displayTags.take(2).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GreenSoft,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyBestMatchCard() {
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
        Text(
            text = "추천 제품을 불러오는 중이에요.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
    }
}

@Composable
private fun DotsIndicator(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    if (total <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val selected = i == current

            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 8.dp else 6.dp)
                    .clip(CircleShape),
                color = if (selected) GreenPrimary else CardBorder,
                tonalElevation = 0.dp
            ) {}
        }
    }
}

private fun formatBestScore(score: Double): String {
    return String.format("%.1f", ((score * 10).roundToInt() / 10.0))
}

private fun toTags(subTag: String): List<String> {
    return subTag
        .split(",", "|", "/")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { toKoreanConcernTag(it) }
        .distinct()
        .take(2)
}

private fun toKoreanConcernTag(value: String): String {
    val key = value
        .trim()
        .lowercase()
        .replace("#", "")
        .replace(" ", "")
        .replace("_", "")

    return when (key) {
        "acne", "pimple", "trouble", "blemish", "여드름" -> "#여드름"
        "redness", "red", "홍조" -> "#홍조"
        "spot", "spots", "pigmentation", "darkspot", "darkspots", "freckle", "freckles", "잡티", "색소침착" -> "#잡티"
        "wrinkle", "wrinkles", "주름" -> "#주름"
        else -> "#$value"
    }
}