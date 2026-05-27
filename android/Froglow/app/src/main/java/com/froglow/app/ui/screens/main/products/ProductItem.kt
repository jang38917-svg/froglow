package com.froglow.app.ui.screens.main.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val ImageBg = Color(0xFFFFFEFC)
private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

@Composable
fun ProductItem(
    product: ProductUi,
    selectedCategory: ConcernCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(18.dp),
                color = ImageBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "추천 점수 ${formatScore(product.rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProductTags(
                    tags = toTags(product.subTag)
                )
            }
        }
    }
}

@Composable
private fun ProductTags(
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

private fun formatScore(score: Double): String {
    return String.format("%.1f", score)
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