package com.froglow.app.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.froglow.app.R
import com.froglow.app.data.SessionStore
import com.froglow.app.ui.screens.main.products.ProductsTab
import com.froglow.app.ui.screens.main.products.ProductsViewModel
import com.froglow.app.ui.screens.main.tabs.ChatbotTab
import com.froglow.app.ui.screens.main.tabs.HomeTab
import com.froglow.app.ui.screens.main.tabs.MyPageTab
import com.froglow.app.ui.screens.main.tabs.StatsTab

enum class MainBottomTab { STATS, PRODUCTS, MAIN, CHAT, MYPAGE }

private val AppBackground = Color(0xFFFFFEFC)

private val BottomBarBg = Color(0xFFFFFFFF)
private val BottomBarBorder = Color(0xFFE4D9C8)

private val NavSelectedBg = Color(0xFFEAF4E2)
private val NavSelectedColor = Color(0xFF77A95B)
private val NavUnselectedColor = Color(0xFF8B867F)

@Composable
fun MainScreen(
    sessionStore: SessionStore,
    onOpenProductDetail: (Long) -> Unit,
    viewModel: ProductsViewModel,
    onCameraClick: () -> Unit = {},
    onRoutineChangeClick: () -> Unit = {},
    onOpenCalendarDetail: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainBottomTab.MAIN) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        bottomBar = {
            BottomNavBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                MainBottomTab.STATS -> StatsTab()

                MainBottomTab.PRODUCTS -> ProductsTab(
                    viewModel = viewModel,
                    onProductClick = onOpenProductDetail
                )

                MainBottomTab.MAIN -> HomeTab(
                    onOpenCalendarDetail = onOpenCalendarDetail
                )

                MainBottomTab.CHAT -> ChatbotTab()

                MainBottomTab.MYPAGE -> MyPageTab(
                    sessionStore = sessionStore,
                    onRoutineChangeClick = onRoutineChangeClick
                )
            }

            if (selectedTab == MainBottomTab.MAIN) {
                FloatingActionButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                        .size(68.dp),
                    containerColor = Color.Transparent,
                    contentColor = Color.Unspecified,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.frog_camera),
                        contentDescription = "카메라",
                        modifier = Modifier.size(68.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    selected: MainBottomTab,
    onSelect: (MainBottomTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color(0x08000000),
                spotColor = Color(0x08000000)
            ),
        color = BottomBarBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, BottomBarBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavButton(
                label = "통계",
                icon = Icons.Rounded.AutoGraph,
                selected = selected == MainBottomTab.STATS,
                onClick = { onSelect(MainBottomTab.STATS) }
            )

            BottomNavButton(
                label = "제품",
                icon = Icons.Rounded.Inventory2,
                selected = selected == MainBottomTab.PRODUCTS,
                onClick = { onSelect(MainBottomTab.PRODUCTS) }
            )

            BottomNavButton(
                label = "홈",
                icon = Icons.Rounded.Home,
                selected = selected == MainBottomTab.MAIN,
                onClick = { onSelect(MainBottomTab.MAIN) }
            )

            BottomNavButton(
                label = "챗봇",
                icon = Icons.Rounded.ChatBubbleOutline,
                selected = selected == MainBottomTab.CHAT,
                onClick = { onSelect(MainBottomTab.CHAT) }
            )

            BottomNavButton(
                label = "마이",
                icon = Icons.Rounded.Person,
                selected = selected == MainBottomTab.MYPAGE,
                onClick = { onSelect(MainBottomTab.MYPAGE) }
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp)
                    .background(
                        color = NavSelectedBg,
                        shape = RoundedCornerShape(18.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    NavSelectedColor
                } else {
                    NavUnselectedColor
                },
                modifier = Modifier.size(20.dp)
            )

            if (selected) {
                Text(
                    text = " $label",
                    color = NavSelectedColor,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}