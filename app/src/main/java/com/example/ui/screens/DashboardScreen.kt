package com.example.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.models.ReservationWithGuestName
import com.example.services.MyFirebaseMessagingService
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.utils.FcmManager
import com.example.utils.Formatters
import com.example.utils.StatusConfig
import com.google.firebase.messaging.FirebaseMessaging
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import com.example.services.SyncService

@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val checkInToday by viewModel.checkInToday.collectAsState()
    val checkOutToday by viewModel.checkOutToday.collectAsState()
    val occupiedUnits by viewModel.occupiedUnits.collectAsState()
    val pending by viewModel.pendingReservations.collectAsState()
    val recentReservations by viewModel.recentReservations.collectAsState(initial = emptyList())

    val guestTypes by viewModel.guestTypes.collectAsState(initial = emptyList())
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("booking_prefs", Context.MODE_PRIVATE) }
    var hasNewBooking by remember { mutableStateOf(prefs.getBoolean("has_new_booking", false)) }

    val dataUpdatedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                hasNewBooking = prefs.getBoolean("has_new_booking", false)
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(SyncService.ACTION_DATA_UPDATED)
        androidx.core.content.ContextCompat.registerReceiver(context, dataUpdatedReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(dataUpdatedReceiver)
        }
    }
    
    var showFcmDialog by remember { mutableStateOf(false) }

    if (showFcmDialog) {
        FcmDialog(onDismiss = { showFcmDialog = false })
    }

    Scaffold(
        containerColor = BackgroundLight,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Header(onProfileClick = { showFcmDialog = true })
            
            AnimatedVisibility(visible = hasNewBooking) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            prefs.edit().putBoolean("has_new_booking", false).apply()
                            hasNewBooking = false
                            navController.navigate("bookings") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Ada pesanan baru! Tap untuk lihat.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp
                        )
                    }
                }
            }
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
fun Header(onProfileClick: () -> Unit) {
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
                .background(PrimaryContainer)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "FCM Config",
                tint = OnPrimaryContainer
            )
        }
    }
}

@Composable
fun FcmDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var token by remember { mutableStateOf(FcmManager.getToken(context)) }
    var notificationList by remember { mutableStateOf(FcmManager.getNotifications(context)) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Listen to foreground notifications
    LaunchedEffect(Unit) {
        FcmManager.newNotificationFlow.collect {
            token = FcmManager.getToken(context)
            notificationList = FcmManager.getNotifications(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Primary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = Primary
                )
                Text(
                    text = "Firebase Cloud Messaging",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundLight
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Status & Registration",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtitle
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (token.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            )
                            Text(
                                text = if (token.isNotEmpty()) "FCM Registered & Active" else "FCM Waiting for Token...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackgroundLight
                            )
                        }

                        if (token.isNotEmpty()) {
                            Text(
                                text = token,
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BackgroundLight, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            )
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(token))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Token",
                                    tint = OnPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Registration Token", fontSize = 11.sp, color = OnPrimaryContainer)
                            }
                        } else {
                            Button(
                                onClick = {
                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            FcmManager.saveToken(context, task.result)
                                            token = task.result
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Text("Retrieve Token", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Interactive Simulation Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notification Simulator",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtitle
                        )
                        Text(
                            text = "Test notifications inside the emulator by simulating an incoming FCM push event locally.",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Button(
                            onClick = {
                                FcmManager.saveNotification(
                                    context, 
                                    "New Booking Confirmed", 
                                    "Reservation for Mr. Henderson has been successfully confirmed!"
                                )
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                val channelId = "saliguri_fcm_channel"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val channel = NotificationChannel(
                                        channelId,
                                        "Saliguri Reservations & Updates",
                                        NotificationManager.IMPORTANCE_DEFAULT
                                    )
                                    notificationManager.createNotificationChannel(channel)
                                }
                                val builder = NotificationCompat.Builder(context, channelId)
                                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                                    .setContentTitle("New Booking Confirmed")
                                    .setContentText("Reservation for Mr. Henderson has been successfully confirmed!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true)
                                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                                
                                notificationList = FcmManager.getNotifications(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Simulate",
                                tint = OnSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate FCM Push Event", fontSize = 11.sp, color = OnSecondaryContainer)
                        }
                    }
                }

                // Push Notification History Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Received Notification History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtitle
                        )
                        if (notificationList.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary,
                                modifier = Modifier.clickable {
                                    FcmManager.clearNotifications(context)
                                    notificationList = emptyList()
                                }
                            )
                        }
                    }

                    if (notificationList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(BackgroundLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No push notifications received yet.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (notification in notificationList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Surface, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, Outline))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = null,
                                            tint = OnPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notification.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnBackgroundLight
                                        )
                                        Text(
                                            text = notification.body,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = Formatters.displayDateTime(notification.timestamp),
                                            fontSize = 8.sp,
                                            color = TextMuted,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Surface
    )
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
