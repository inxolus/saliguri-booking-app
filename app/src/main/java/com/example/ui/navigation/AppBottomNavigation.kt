package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.*

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Column {
        HorizontalDivider(color = Outline, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Surface)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Filled.Dashboard,
                label = "Home",
                isActive = currentRoute == "dashboard",
                onClick = { 
                    navController.navigate("dashboard") { 
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true 
                    } 
                }
            )
            NavItem(
                icon = Icons.Filled.CalendarToday,
                label = "Calendar",
                isActive = currentRoute == "calendar",
                onClick = { 
                    navController.navigate("calendar") { 
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true 
                    } 
                }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                label = "Bookings",
                isActive = currentRoute == "bookings",
                onClick = { 
                    navController.navigate("bookings") { 
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true 
                    } 
                }
            )
            NavItem(
                icon = Icons.Filled.Analytics,
                label = "Reports",
                isActive = currentRoute == "reports",
                onClick = { 
                    navController.navigate("reports") { 
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true 
                    } 
                }
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = OnPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextSubtitle,
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) OnPrimaryContainer else TextSubtitle
        )
    }
}
