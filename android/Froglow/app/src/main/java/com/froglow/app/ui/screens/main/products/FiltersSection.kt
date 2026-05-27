package com.froglow.app.ui.screens.main.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val SheetBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(
    selectedSkinTypes: Set<SkinTypeFilter>,
    selectedProductTypes: Set<ProductTypeFilter>,
    onReset: () -> Unit,
    onApply: (Set<SkinTypeFilter>, Set<ProductTypeFilter>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            OutlinedButton(
                onClick = { showSheet = true },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Text(
                    text = "카테고리별",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary
                )
            }
        }
    }

    if (showSheet) {
        CategoryBottomSheet(
            initialSkinTypes = selectedSkinTypes,
            initialProductTypes = selectedProductTypes,
            onDismiss = { showSheet = false },
            onReset = {
                onReset()
            },
            onApply = { skins, products ->
                onApply(skins, products)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryBottomSheet(
    initialSkinTypes: Set<SkinTypeFilter>,
    initialProductTypes: Set<ProductTypeFilter>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (Set<SkinTypeFilter>, Set<ProductTypeFilter>) -> Unit
) {
    var skinSelected by remember { mutableStateOf(initialSkinTypes) }
    var productSelected by remember { mutableStateOf(initialProductTypes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "카테고리",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 1.dp, color = CardBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "피부 타입",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkinTypeFilter.entries
                    .filter { it != SkinTypeFilter.ALL }
                    .forEach { opt ->
                        CategoryChip(
                            text = opt.label,
                            selected = skinSelected.contains(opt),
                            onClick = {
                                skinSelected = if (skinSelected.contains(opt)) {
                                    skinSelected - opt
                                } else {
                                    skinSelected + opt
                                }
                            }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Divider(thickness = 1.dp, color = CardBorder)
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "제품 종류",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProductTypeFilter.entries
                    .filter { it != ProductTypeFilter.ALL }
                    .forEach { opt ->
                        CategoryChip(
                            text = opt.label,
                            selected = productSelected.contains(opt),
                            onClick = {
                                productSelected = if (productSelected.contains(opt)) {
                                    productSelected - opt
                                } else {
                                    productSelected + opt
                                }
                            }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(thickness = 1.dp, color = CardBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        skinSelected = emptySet()
                        productSelected = emptySet()
                        onReset()
                    }
                ) {
                    Text(
                        text = "초기화",
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText
                    )
                }

                Button(
                    onClick = { onApply(skinSelected, productSelected) },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "적용하기",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) GreenSoft else CardBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) GreenPrimary else CardBorder
        ),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) GreenPrimary else SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}