package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.SleekIndigoPrimary
import com.example.ui.theme.SleekTertiary
import com.example.util.AppStrings

enum class BottomNavTab {
    HOME,
    CUSTOMERS,
    ADD,
    REPORTS,
    SETTINGS
}

@Composable
fun SuDhanBottomNav(
    selectedTab: BottomNavTab,
    language: AppLanguage,
    onTabSelected: (BottomNavTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home
            BottomNavItem(
                iconSelected = Icons.Filled.Dashboard,
                iconUnselected = Icons.Outlined.Dashboard,
                label = AppStrings.get("nav_home", language),
                isSelected = selectedTab == BottomNavTab.HOME,
                onClick = { onTabSelected(BottomNavTab.HOME) }
            )

            // Tab 2: Customers
            BottomNavItem(
                iconSelected = Icons.Filled.People,
                iconUnselected = Icons.Outlined.People,
                label = AppStrings.get("nav_customers", language),
                isSelected = selectedTab == BottomNavTab.CUSTOMERS,
                onClick = { onTabSelected(BottomNavTab.CUSTOMERS) }
            )

            // Tab 3: Add (+) Centered Sleek Button
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, spotColor = SleekIndigoPrimary.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SleekIndigoPrimary,
                                SleekTertiary
                            )
                        )
                    )
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Tab 4: Reports
            BottomNavItem(
                iconSelected = Icons.Filled.Assessment,
                iconUnselected = Icons.Outlined.Assessment,
                label = AppStrings.get("nav_reports", language),
                isSelected = selectedTab == BottomNavTab.REPORTS,
                onClick = { onTabSelected(BottomNavTab.REPORTS) }
            )

            // Tab 5: Settings
            BottomNavItem(
                iconSelected = Icons.Filled.Settings,
                iconUnselected = Icons.Outlined.Settings,
                label = AppStrings.get("nav_settings", language),
                isSelected = selectedTab == BottomNavTab.SETTINGS,
                onClick = { onTabSelected(BottomNavTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(30.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
