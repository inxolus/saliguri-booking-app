package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.ReservationWithGuestName
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.utils.AppConstants
import com.example.utils.Formatters
import com.example.utils.StatusConfig

import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationListScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val reservations by viewModel.recentReservations.collectAsState(initial = emptyList())
    val guestTypes by viewModel.guestTypes.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    
    // Status Filter Dialog/Dropdown logic
    var isFilterExpanded by remember { mutableStateOf(false) }
    val statuses = listOf(null, AppConstants.STATUS_PENDING, AppConstants.STATUS_CONFIRMED, AppConstants.STATUS_CHECKED_IN, AppConstants.STATUS_CHECKED_OUT, AppConstants.STATUS_CANCELLED)

    // Delete/Cancel Confirmation State
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogConfirmAction by remember { mutableStateOf({}) }

    val confirmCancel = { id: String ->
        dialogTitle = "Cancel Reservation"
        dialogMessage = "Are you sure you want to cancel this reservation?"
        dialogConfirmAction = { viewModel.updateReservationStatus(id, AppConstants.STATUS_CANCELLED) }
        showDialog = true
    }

    val confirmDelete = { id: String ->
        dialogTitle = "Delete Reservation"
        dialogMessage = "Are you sure you want to permanently delete this reservation? This cannot be undone."
        dialogConfirmAction = { viewModel.deleteReservation(id) }
        showDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservations") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = OnBackgroundLight
                ),
                actions = {
                    Box {
                        IconButton(onClick = { isFilterExpanded = true }) {
                            Icon(Icons.Filled.FilterList, "Filter")
                        }
                        DropdownMenu(
                            expanded = isFilterExpanded,
                            onDismissRequest = { isFilterExpanded = false }
                        ) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(if (status == null) "All Statuses" else StatusConfig.getDisplayName(status)) },
                                    onClick = { 
                                        selectedStatusFilter = status
                                        isFilterExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = BackgroundLight,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface
                )
            )

            val filteredList = reservations.filter { res ->
                val matchesSearch = res.guest_name.contains(searchQuery, ignoreCase = true) || res.guest_phone.contains(searchQuery)
                val matchesStatus = selectedStatusFilter == null || res.statusCode == selectedStatusFilter
                matchesSearch && matchesStatus
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reservations found.", color = TextSubtitle)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { res ->
                        ReservationEnhancedCard(
                            res = res,
                            guestTypes = guestTypes,
                            onClick = { navController.navigate("detail/${res.id}") },
                            onCheckIn = { viewModel.updateReservationStatus(res.id, AppConstants.STATUS_CHECKED_IN) },
                            onCheckOut = { viewModel.updateReservationStatus(res.id, AppConstants.STATUS_CHECKED_OUT) },
                            onCancel = { confirmCancel(res.id) },
                            onDelete = { confirmDelete(res.id) },
                            onEdit = { navController.navigate("form?id=${res.id}") }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(dialogTitle) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { 
                        dialogConfirmAction() 
                        showDialog = false 
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ReservationEnhancedCard(
    res: ReservationWithGuestName,
    guestTypes: List<com.example.data.models.GuestType>,
    onClick: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isWedding = res.guestTypeCode == AppConstants.GUEST_WEDDING
    val icon = if (isWedding) Icons.Filled.Celebration else Icons.Filled.Person
    val bgColor = if (isWedding) CardWeddingBg else Surface
    
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(StatusConfig.getBackgroundColor(res.statusCode), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = res.guest_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val guestTypeName = guestTypes.find { it.code == res.guestTypeCode }?.name ?: res.guestTypeCode
                Text(
                    text = "${StatusConfig.getDisplayName(res.statusCode)} • $guestTypeName • ${Formatters.currency(res.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtitle
                )
                Text(
                    text = "${Formatters.displayDateOnly(res.checkInDate)} - ${Formatters.displayDateOnly(res.checkOutDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, "More actions")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    when (res.statusCode) {
                        AppConstants.STATUS_PENDING, AppConstants.STATUS_CONFIRMED -> {
                            DropdownMenuItem(text = { Text("Check-In") }, onClick = { onCheckIn(); expanded = false })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); expanded = false })
                            DropdownMenuItem(text = { Text("Cancel") }, onClick = { onCancel(); expanded = false })
                        }
                        AppConstants.STATUS_CHECKED_IN -> {
                            DropdownMenuItem(text = { Text("Check-Out") }, onClick = { onCheckOut(); expanded = false })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); expanded = false })
                        }
                        AppConstants.STATUS_CANCELLED -> {
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); expanded = false })
                        }
                        AppConstants.STATUS_CHECKED_OUT -> {
                            DropdownMenuItem(text = { Text("View details") }, onClick = { onClick(); expanded = false })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); expanded = false })
                        }
                    }
                }
            }
        }
    }
}
