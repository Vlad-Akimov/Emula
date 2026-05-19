package com.example.nfctagemulator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.ui.theme.NeonCyan
import com.example.nfctagemulator.ui.theme.SurfaceGlow
import com.example.nfctagemulator.ui.theme.getAdaptiveDimens

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // Dynamic height based on device and orientation
    val barHeight = when {
        isTablet -> 80.dp
        isLandscape -> 56.dp
        else -> 72.dp
    }

    NavigationBar(
        containerColor = SurfaceGlow,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .navigationBarsPadding(), // Add safe padding for navigation bar
        windowInsets = WindowInsets(0, 0, 0, 0) // Remove default insets to avoid double padding
    ) {
        val navItems = listOf(
            Triple(0, Icons.Default.Folder, "SAVED"),
            Triple(1, Icons.Default.QrCodeScanner, "SCAN"),
            Triple(2, Icons.Default.Add, "CREATE")
        )

        navItems.forEach { (index, icon, label) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(dimens.iconSize.dp)
                    )
                },
                label = {
                    Text(
                        label,
                        fontSize = if (isLandscape || isTablet) 11.sp else 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = if (selectedTab == index) NeonCyan else Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                },
                alwaysShowLabel = !isLandscape,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = NeonCyan.copy(alpha = 0.2f)
                )
            )
        }
    }
}