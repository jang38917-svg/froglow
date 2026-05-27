package com.froglow.app.ui.screens.main.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val ProductsScreenBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val ErrorSoft = Color(0xFFFFEFEF)
private val ErrorText = Color(0xFFE95F62)

@Composable
fun ProductsTab(
    viewModel: ProductsViewModel,
    onProductClick: (Long) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val skinSelected by viewModel.selectedSkinTypes.collectAsState()
    val productSelected by viewModel.selectedProductTypes.collectAsState()
    val bestMatch3 by viewModel.bestMatch3.collectAsState()

    val pagerState = rememberPagerState(
        pageCount = { bestMatch3.size.coerceAtLeast(1) }
    )

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductsScreenBg),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            when {
                loading -> {
                    LoadingSection()
                }

                error != null -> {
                    ErrorSection(
                        message = error ?: "상품 추천 조회 실패",
                        onRetry = { viewModel.retryLoadProducts() }
                    )
                }

                bestMatch3.isEmpty() && products.isEmpty() -> {
                    EmptySection()
                }

                else -> {
                    BestMatchSection(
                        title = "추천 제품",
                        subtitle = "피부 상태에 잘 맞는 제품을 먼저 보여드려요",
                        items = bestMatch3,
                        pagerState = pagerState,
                        onClick = { id -> onProductClick(id) }
                    )
                }
            }
        }

        if (!loading && error == null && (bestMatch3.isNotEmpty() || products.isNotEmpty())) {
            item {
                FiltersSection(
                    selectedSkinTypes = skinSelected,
                    selectedProductTypes = productSelected,
                    onReset = {
                        viewModel.resetCategoryFilters()
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    onApply = { skins, productsSet ->
                        viewModel.applyCategoryFilters(skins, productsSet)
                        scope.launch { pagerState.scrollToPage(0) }
                    }
                )
            }

            items(products) { item ->
                ProductItem(
                    product = item,
                    selectedCategory = ConcernCategory.ACNE,
                    onClick = { onProductClick(item.id) }
                )
            }

            if (products.isEmpty()) {
                item {
                    EmptyFilterResultCard()
                }
            }
        }
    }
}

@Composable
private fun LoadingSection() {
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "추천 제품을 불러오는 중이에요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Text(
                text = "피부 상태에 맞는 제품을 찾고 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun ErrorSection(
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ErrorSoft,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "제품 정보를 불러오지 못했어요",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ErrorText
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "다시 시도",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptySection() {
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "추천 제품 결과가 없어요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Text(
                text = "피부 진단 후 다시 확인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyFilterResultCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Text(
            text = "선택한 조건에 맞는 제품이 없습니다.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
    }
}