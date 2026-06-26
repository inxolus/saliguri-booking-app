package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ui.theme.*
import com.example.ui.viewmodels.DetailViewModel
import com.example.utils.Formatters
import com.example.utils.StatusConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailScreen(
    navController: NavController,
    reservationId: String,
    viewModel: DetailViewModel = viewModel()
) {
    val reservation by viewModel.reservation.collectAsState()
    val guest by viewModel.guest.collectAsState()
    val units by viewModel.units.collectAsState()
    val services by viewModel.services.collectAsState()
    
    val allUnitsState by viewModel.allUnits.collectAsState()
    val allFoodsState by viewModel.allFoods.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    LaunchedEffect(reservationId) {
        viewModel.loadReservation(reservationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservation Detail") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = OnBackgroundLight
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (reservation == null || guest == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val res = reservation!!
            val g = guest!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Guest Info
                Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Guest Information", style = MaterialTheme.typography.titleMedium, color = Primary)
                        Text("Name: ${g.name}")
                        Text("Phone: ${g.phone}")
                    }
                }

                // Reservation Info
                Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                     Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reservation Details", style = MaterialTheme.typography.titleMedium, color = Primary)
                        Text("Status: ${StatusConfig.getDisplayName(res.statusCode)}")
                        Text("Dates: ${Formatters.displayDateOnly(res.checkInDate)} to ${Formatters.displayDateOnly(res.checkOutDate)}")
                        Text("Total Amount: ${Formatters.currency(res.totalAmount)}")
                        Text("DP Amount: ${Formatters.currency(res.downPayment)}")
                        Text("Already Paid: ${Formatters.currency(res.paidAmount)}")
                        Text("Remaining: ${Formatters.currency(maxOf(0L, res.totalAmount - res.paidAmount))}")
                        Text("Notes: ${res.notes}")
                    }
                }

                // Units Info
                if (units.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Units", style = MaterialTheme.typography.titleMedium, color = Primary)
                            units.forEach { u ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val unitName = allUnitsState.find { it.unitCode == u.unitCode }?.unitName ?: u.unitCode
                                    Text(unitName, style = MaterialTheme.typography.bodyMedium)
                                    Text("Dates: ${Formatters.displayDateOnly(u.checkInDate)} to ${Formatters.displayDateOnly(u.checkOutDate)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Nights: ${u.nightsCount} | Rate: ${Formatters.currency(u.nightlyRate)} | EB: ${u.extraBedCount}", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Services Info
                if (services.isNotEmpty()) {
                     Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Additional Services", style = MaterialTheme.typography.titleMedium, color = Primary)
                            services.forEach { s ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("Service: ${s.serviceCode}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Qty: ${s.quantity} | Total: ${Formatters.currency(s.totalPrice)}", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Foods Info
                val foods by viewModel.foods.collectAsState()
                if (foods.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Food Packages", style = MaterialTheme.typography.titleMedium, color = Primary)
                            foods.forEach { f ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val foodName = allFoodsState.find { it.code == f.foodPackageCode }?.packageName ?: f.foodPackageCode
                                    Text(foodName, style = MaterialTheme.typography.bodyMedium)
                                    Text("People: ${f.numberOfPeople} | Total: ${Formatters.currency(f.totalPrice)}", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Payments Info
                val payments by viewModel.payments.collectAsState()
                if (payments.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Payment History", style = MaterialTheme.typography.titleMedium, color = Primary)
                            payments.forEach { p ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val methodName = paymentMethods.find { it.code == p.paymentMethodCode }?.name ?: p.paymentMethodCode
                                    Text("Amount: ${Formatters.currency(p.amount)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Date: ${Formatters.displayDateTime(p.paymentDate)} | Method: $methodName", style = MaterialTheme.typography.bodySmall)
                                    if (!p.notes.isNullOrEmpty()) {
                                        Text("Notes: ${p.notes}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Actions
                Spacer(modifier = Modifier.height(16.dp))
                
                var showAddPaymentDialog by remember { mutableStateOf(false) }

                var showCancelDialog by remember { mutableStateOf(false) }
                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Cancel Reservation") },
                        text = { Text("Are you sure you want to cancel this reservation?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateReservationStatus(reservationId, com.example.utils.AppConstants.STATUS_CANCELLED)
                                showCancelDialog = false
                            }) { Text("Yes, Cancel", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) { Text("No") }
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val remaining = maxOf(0L, res.totalAmount - res.paidAmount)
                    val canAddPayment = res.statusCode !in listOf(
                        com.example.utils.AppConstants.STATUS_CHECKED_OUT,
                        com.example.utils.AppConstants.STATUS_CANCELLED,
                        com.example.utils.AppConstants.STATUS_NO_SHOW
                    )
                    
                    if (canAddPayment && remaining > 0) {
                        Button(onClick = { showAddPaymentDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add Payment")
                        }
                        if (showAddPaymentDialog) {
                            val methods by viewModel.paymentMethods.collectAsState()
                            AddPaymentDialog(
                                totalAmount = res.totalAmount,
                                paidAmount = res.paidAmount,
                                methods = methods,
                                onDismiss = { showAddPaymentDialog = false },
                                onConfirm = { amount, method, notes ->
                                    viewModel.addPayment(reservationId, amount, method, notes)
                                    showAddPaymentDialog = false
                                }
                            )
                        }
                    }
                    
                    if (res.statusCode == com.example.utils.AppConstants.STATUS_PENDING || res.statusCode == com.example.utils.AppConstants.STATUS_CONFIRMED) {
                        Button(onClick = { viewModel.updateReservationStatus(reservationId, com.example.utils.AppConstants.STATUS_CHECKED_IN) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Check-In")
                        }
                    } else if (res.statusCode == com.example.utils.AppConstants.STATUS_CHECKED_IN) {
                        Button(onClick = { viewModel.updateReservationStatus(reservationId, com.example.utils.AppConstants.STATUS_CHECKED_OUT) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Check-Out")
                        }
                    }

                    val canEdit = res.statusCode in listOf(
                        com.example.utils.AppConstants.STATUS_PENDING,
                        com.example.utils.AppConstants.STATUS_CONFIRMED,
                        com.example.utils.AppConstants.STATUS_CHECKED_IN
                    )
                    if (canEdit) {
                        Button(onClick = { navController.navigate("form?id=${reservationId}") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Edit Reservation")
                        }
                    }

                    val canCancel = res.statusCode in listOf(
                        com.example.utils.AppConstants.STATUS_PENDING,
                        com.example.utils.AppConstants.STATUS_CONFIRMED
                    )
                    if (canCancel) {
                        OutlinedButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel Reservation", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    totalAmount: Long,
    paidAmount: Long,
    methods: List<com.example.data.models.PaymentMethod>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit
) {
    val remaining = maxOf(0L, totalAmount - paidAmount)
    var amountStr by remember { mutableStateOf(remaining.toString()) }
    var selectedMethod by remember { mutableStateOf(methods.firstOrNull()) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Tagihan: ${Formatters.currency(totalAmount)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Sudah Dibayar: ${Formatters.currency(paidAmount)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Sisa Bayar: ${Formatters.currency(remaining)}", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Primary)
                    }
                }
                
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod?.name ?: "Select Method",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        methods.forEach { method ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(method.name) },
                                onClick = {
                                    selectedMethod = method
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toLongOrNull() ?: 0L
            val isValid = amt > 0 && selectedMethod != null
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(amt, selectedMethod!!.code, notes)
                    }
                },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
