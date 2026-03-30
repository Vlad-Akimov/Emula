package com.example.nfctagemulator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.ui.theme.NeonCyan
import com.example.nfctagemulator.ui.theme.SurfaceGlow

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceGlow,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "Saved Tags",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "SAVED",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (selectedTab == 0) NeonCyan else Color.White.copy(alpha = 0.6f)
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = NeonCyan.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "SCAN",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (selectedTab == 1) NeonCyan else Color.White.copy(alpha = 0.6f)
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = NeonCyan.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "CREATE",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (selectedTab == 2) NeonCyan else Color.White.copy(alpha = 0.6f)
                )
            },
            alwaysShowLabel = true,
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