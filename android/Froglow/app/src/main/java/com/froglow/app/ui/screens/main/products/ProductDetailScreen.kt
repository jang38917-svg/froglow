package com.froglow.app.ui.screens.main.products

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

private val DetailScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val ImageBg = Color(0xFFFFFEFC)
private val InfoBg = Color(0xFFFFFCF7)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val ScoreStar = Color(0xFFFFC542)

@Composable
fun ProductDetailScreen(
    product: ProductUi,
    modifier: Modifier = Modifier
) {
    val baseScore = product.rating
        .coerceIn(0.0, 10.0)
        .roundToInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DetailScreenBg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProductImageCard(
            imageUrl = product.imageUrl,
            productName = product.name
        )

        ProductInfoCard(
            productName = product.name,
            rating = product.rating
        )

        ConcernScoreCard(
            baseScore = baseScore
        )

        RecommendationInfoCard()

        BuyButton(
            buyUrl = product.buyUrl
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProductImageCard(
    imageUrl: String?,
    productName: String
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp),
                color = ImageBg,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                AsyncImage(
                    model = imageUrl.orEmpty(),
                    contentDescription = productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }
    }
}

@Composable
private fun ProductInfoCard(
    productName: String,
    rating: Double
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GreenSoft,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★",
                        color = ScoreStar,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.size(5.dp))

                    Text(
                        text = "추천 점수 ${formatScore(rating)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcernScoreCard(
    baseScore: Int
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = "고민별 적합도",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            ConcernScoreRow("여드름", baseScore)
            ConcernScoreRow("홍조", (baseScore - 1).coerceAtLeast(0))
            ConcernScoreRow("잡티", (baseScore - 2).coerceAtLeast(0))
            ConcernScoreRow("주름", (baseScore - 3).coerceAtLeast(0))
        }
    }
}

@Composable
private fun ConcernScoreRow(
    label: String,
    value: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f)
        )

        Row(
            modifier = Modifier.weight(2.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedBar(
                value = value,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SegmentedBar(
    value: Int,
    modifier: Modifier = Modifier
) {
    val safe = value.coerceIn(0, 10)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 1..10) {
            val filled = i <= safe

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(9.dp),
                shape = RoundedCornerShape(999.dp),
                color = if (filled) {
                    GreenPrimary
                } else {
                    GreenSoft
                },
                tonalElevation = 0.dp
            ) {}
        }
    }
}

@Composable
private fun RecommendationInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = InfoBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "추천 기준",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Text(
                text = "AI 피부 분석 결과와 제품의 고민별 등급을 기반으로 계산되었습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun BuyButton(
    buyUrl: String?
) {
    val context = LocalContext.current
    if (buyUrl.isNullOrBlank()) return

    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buyUrl))
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GreenPrimary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "구매 페이지로 이동",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatScore(score: Double): String {
    return String.format("%.1f", score)
}