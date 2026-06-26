package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.models.ReservationWithGuestName
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.utils.Formatters
import com.example.utils.StatusConfig

@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val checkInToday by viewModel.checkInToday.collectAsState()
    val checkOutToday by viewModel.checkOutToday.collectAsState()
    val occupiedUnits by viewModel.occupiedUnits.collectAsState()
    val pending by viewModel.pendingReservations.collectAsState()
    val recentReservations by viewModel.recentReservations.collectAsState(initial = emptyList())

    val guestTypes by viewModel.guestTypes.collectAsState(initial = emptyList())
    
    Scaffold(
        containerColor = BackgroundLight,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Header()
            StatsGrid(
                checkIn = checkInToday,
                checkOut = checkOutToday,
                occupied = occupiedUnits,
                pending = pending
            )
            QuickActionArea(
                navController = navController,
                onRefresh = { viewModel.refreshStats() }
            )
            RecentReservationsList(
                reservations = recentReservations, 
                guestTypes = guestTypes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Saliguri Cottage Harau",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnPrimaryContainer
            )
            Text(
                text = "MANAGER DASHBOARD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSubtitle,
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile",
                tint = OnPrimaryContainer
            )
        }
    }
}

@Composable
fun StatsGrid(checkIn: String, checkOut: String, occupied: String, pending: String) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Login,
                value = checkIn,
                label = "Check-In Today",
                bgColor = PrimaryContainer,
                contentColor = OnPrimaryContainer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Logout,
                value = checkOut,
                label = "Check-Out Today",
                bgColor = SecondaryContainer,
                contentColor = OnSecondaryContainer
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Bed,
                value = occupied,
                label = "Occupied Units",
                bgColor = TertiaryContainer,
                contentColor = OnTertiaryContainer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PendingActions,
                value = pending,
                label = "Pending",
                bgColor = ErrorContainer,
                contentColor = OnErrorContainer
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    bgColor: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun QuickActionArea(navController: NavController, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Reservations",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSubtitle
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = OnPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Button(
                onClick = { navController.navigate("form") },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Booking",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "New Booking", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RecentReservationsList(reservations: List<ReservationWithGuestName>, guestTypes: List<com.example.data.models.GuestType>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = borderStroke()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            if (reservations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = "Empty",
                                modifier = Modifier.size(36.dp),
                                tint = TextMuted.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No recent reservations",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(reservations) { res ->
                    val isWedding = res.guestTypeCode == com.example.utils.AppConstants.GUEST_WEDDING
                    val icon = if (isWedding) Icons.Filled.Celebration else Icons.Filled.Person
                    val iconBg = if (isWedding) CardWeddingBg else SecondaryContainer
                    val iconTint = if (isWedding) CardWeddingOnBg else OnSecondaryContainer
                    val showDp = res.paymentStatus == "DP" || res.paymentStatus == "PARTIAL"
                    val dpText = if (showDp) "DP/Partial: ${Formatters.currency(res.paidAmount)}" else ""

                    val guestTypeName = guestTypes.find { it.code == res.guestTypeCode }?.name ?: res.guestTypeCode
                    val detailsText = "$guestTypeName • Total: ${Formatters.currency(res.totalAmount)}"
                    
                    ReservationItem(
                        guestName = res.guest_name,
                        statusText = StatusConfig.getDisplayName(res.statusCode),
                        statusBgColor = StatusConfig.getBackgroundColor(res.statusCode),
                        statusTextColor = StatusConfig.getTextColor(res.statusCode),
                        detailsText = detailsText,
                        dateRange = "${Formatters.displayDateOnly(res.checkInDate)} - ${Formatters.displayDateOnly(res.checkOutDate)}",
                        icon = icon,
                        iconBgColor = iconBg,
                        iconTint = iconTint,
                        showDpPending = showDp,
                        dpPendingText = dpText
                    )
                }
            }
        }
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Outline)

@Composable
fun ReservationItem(
    guestName: String,
    statusText: String,
    statusBgColor: Color,
    statusTextColor: Color,
    detailsText: String,
    dateRange: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    showDpPending: Boolean = false,
    dpPendingText: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .background(Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Status Icon",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = guestName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            Text(
                text = detailsText,
                fontSize = 11.sp,
                color = TextMuted
            )
            if (dateRange.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Date",
                        tint = Primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateRange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary
                    )
                }
            }
            if (showDpPending) {
                Text(
                    text = dpPendingText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPending,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
}

// Removed CustomBottomNavigation from here
