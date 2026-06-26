package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ui.theme.*
import com.example.ui.viewmodels.FormViewModel
import com.example.utils.Formatters
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationFormScreen(navController: NavController, reservationId: String? = null, prefillUnit: String? = null, prefillDate: String? = null, viewModel: FormViewModel = viewModel()) {
    LaunchedEffect(reservationId, prefillUnit, prefillDate) {
        if (reservationId != null) {
            viewModel.loadExistingReservation(reservationId)
        } else if (prefillUnit != null && prefillDate != null) {
            viewModel.prefill(prefillUnit, prefillDate)
        }
    }
    val guestName by viewModel.guestName.collectAsState()
    val guestPhone by viewModel.guestPhone.collectAsState()
    val guestTypes by viewModel.guestTypes.collectAsState()
    val selectedGuestType by viewModel.selectedGuestType.collectAsState()
    
    val allUnits by viewModel.allUnits.collectAsState()
    val selectedUnits by viewModel.selectedUnits.collectAsState()
    
    val allServices by viewModel.allServices.collectAsState()
    val selectedServices by viewModel.selectedServices.collectAsState()
    
    val allFoods by viewModel.allFoodPackages.collectAsState()
    val selectedFoods by viewModel.selectedFoods.collectAsState()
    
    val statuses by viewModel.reservationStatuses.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    
    val hasDownPayment by viewModel.hasDownPayment.collectAsState()
    val isDpPercentage by viewModel.isDpPercentage.collectAsState()
    val dpInputValue by viewModel.dpInputValue.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        launch {
            viewModel.submissionSuccess.collectLatest {
                navController.navigateUp()
            }
        }
        launch {
            viewModel.errorMessage.collectLatest { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Reservation") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight,
        bottomBar = {
            val dpAmount = if (!hasDownPayment) 0L else {
                val input = dpInputValue.toLongOrNull() ?: 0L
                if (isDpPercentage) {
                    (totalAmount * input.coerceIn(0, 100)) / 100
                } else {
                    input.coerceAtMost(totalAmount)
                }
            }
            BottomSummaryBar(
                totalAmount = totalAmount,
                dpAmount = dpAmount,
                isSubmitting = isSubmitting,
                onSubmit = { viewModel.submit() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Guest Card
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Guest Information", style = MaterialTheme.typography.titleMedium, color = Primary)
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { viewModel.guestName.value = it },
                        label = { Text("Guest Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = guestPhone,
                        onValueChange = { viewModel.guestPhone.value = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Guest Type", style = MaterialTheme.typography.bodyMedium)
                    // Simplified Dropdown using ExposedDropdownMenuBox can be complex, so let's use a Row of Selectable Chips or simple buttons for demonstration, or default ExposedDropdownMenuBox
                    ExposedDropdownMenuBoxContainer(
                        items = guestTypes.map { it.name },
                        selectedItem = guestTypes.find { it.code == selectedGuestType }?.name ?: "",
                        onItemSelected = { name -> 
                            val code = guestTypes.find { it.name == name }?.code
                            if (code != null) viewModel.onGuestTypeChanged(code)
                        }
                    )
                }
            }

            // Units Card
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Units Selection", style = MaterialTheme.typography.titleMedium, color = Primary)
                        val totalGuest = selectedUnits.sumOf { it.unit.capacity + it.extraBedCount }
                        val totalMaxGuest = selectedUnits.sumOf { it.unit.capacity + it.unit.maxExtraBed }
                        Text("Total Tamu: $totalGuest orang (Max: $totalMaxGuest)", style = MaterialTheme.typography.bodySmall, color = Primary)
                    }
                    
                    allUnits.forEach { unit ->
                        val isSelected = selectedUnits.any { it.unit.unitCode == unit.unitCode }
                        val unitState = selectedUnits.find { it.unit.unitCode == unit.unitCode }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(unit.unitName, style = MaterialTheme.typography.titleSmall)
                                    Text("Capacity: ${unit.capacity} | Max EB: ${unit.maxExtraBed}", style = MaterialTheme.typography.bodySmall, color = TextSubtitle)
                                    if (unitState?.isWeddingIncluded == true) {
                                        Text("Included in Wedding Package", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    } else {
                                        Text(Formatters.currency(unit.price) + " / night", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Switch(
                                    checked = isSelected,
                                    onCheckedChange = { 
                                        if (it) viewModel.addUnit(unit) else viewModel.removeUnit(unit.unitCode) 
                                    },
                                    enabled = unitState?.isWeddingIncluded != true // disable switch if it's auto-added
                                )
                            }
                            
                            if (isSelected && unitState != null) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Extra Bed: ", style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { if (unitState.extraBedCount > 0) viewModel.updateUnitExtraBed(unit.unitCode, unitState.extraBedCount - 1) }) {
                                            Icon(Icons.Filled.Remove, "Remove")
                                        }
                                        Text(unitState.extraBedCount.toString())
                                        IconButton(onClick = { if (unitState.extraBedCount < unit.maxExtraBed) viewModel.updateUnitExtraBed(unit.unitCode, unitState.extraBedCount + 1) }) {
                                            Icon(Icons.Filled.Add, "Add")
                                        }
                                    }
                                    
                                    var showCheckInPicker by remember { mutableStateOf(false) }
                                    var showCheckOutPicker by remember { mutableStateOf(false) }
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { showCheckInPicker = true }, modifier = Modifier.weight(1f)) {
                                            Text("Check-In:\n${Formatters.displayDateOnly(unitState.checkIn)}", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                        OutlinedButton(onClick = { showCheckOutPicker = true }, modifier = Modifier.weight(1f)) {
                                            Text("Check-Out:\n${Formatters.displayDateOnly(unitState.checkOut)}", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                    }
                                    
                                    if (showCheckInPicker) {
                                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = unitState.checkIn)
                                        DatePickerDialog(
                                            onDismissRequest = { showCheckInPicker = false },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    datePickerState.selectedDateMillis?.let {
                                                        viewModel.updateUnitDates(unit.unitCode, it, unitState.checkOut)
                                                    }
                                                    showCheckInPicker = false
                                                }) { Text("OK") }
                                            }
                                        ) {
                                            DatePicker(state = datePickerState)
                                        }
                                    }
                                    if (showCheckOutPicker) {
                                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = unitState.checkOut)
                                        DatePickerDialog(
                                            onDismissRequest = { showCheckOutPicker = false },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    datePickerState.selectedDateMillis?.let {
                                                        viewModel.updateUnitDates(unit.unitCode, unitState.checkIn, it)
                                                    }
                                                    showCheckOutPicker = false
                                                }) { Text("OK") }
                                            }
                                        ) {
                                            DatePicker(state = datePickerState)
                                        }
                                    }
                                }
                            }
                        }
                        if (unit != allUnits.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            // Services Card
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Additional Services", style = MaterialTheme.typography.titleMedium, color = Primary)
                    allServices.forEach { service ->
                        val isSelected = selectedServices.any { it.service.code == service.code }
                        val serviceState = selectedServices.find { it.service.code == service.code }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.serviceName, style = MaterialTheme.typography.titleSmall)
                                    Text(Formatters.currency(service.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Switch(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) viewModel.addService(service) else viewModel.removeService(service.code) },
                                    enabled = service.code != com.example.utils.AppConstants.WEDDING_AUTO_SERVICE || selectedGuestType != com.example.utils.AppConstants.GUEST_WEDDING
                                )
                            }
                            if (isSelected && serviceState != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Qty: ", style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { if (serviceState.quantity > 1) viewModel.updateServiceQty(service.code, serviceState.quantity - 1) }) {
                                        Icon(Icons.Filled.Remove, "Remove")
                                    }
                                    Text(serviceState.quantity.toString())
                                    IconButton(onClick = { viewModel.updateServiceQty(service.code, serviceState.quantity + 1) }) {
                                        Icon(Icons.Filled.Add, "Add")
                                    }
                                }
                            }
                        }
                        if (service != allServices.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            // Food Packages Card
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Food Packages", style = MaterialTheme.typography.titleMedium, color = Primary)
                    val allFoodPackages by viewModel.allFoodPackages.collectAsState()
                    val selectedFoods by viewModel.selectedFoods.collectAsState()
                    allFoodPackages.forEach { food ->
                        val isSelected = selectedFoods.any { it.food.code == food.code }
                        val foodState = selectedFoods.find { it.food.code == food.code }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(food.packageName, style = MaterialTheme.typography.titleSmall)
                                    Text(food.description ?: "", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(Formatters.currency(food.pricePerPerson) + " / pax", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Switch(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) viewModel.addFood(food) else viewModel.removeFood(food.code) }
                                )
                            }
                            if (isSelected && foodState != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Qty: ", style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { if (foodState.quantity > 1) viewModel.updateFoodQty(food.code, foodState.quantity - 1) }) {
                                        Icon(Icons.Filled.Remove, "Remove")
                                    }
                                    Text(foodState.quantity.toString())
                                    IconButton(onClick = { viewModel.updateFoodQty(food.code, foodState.quantity + 1) }) {
                                        Icon(Icons.Filled.Add, "Add")
                                    }
                                }
                            }
                        }
                        if (food != allFoodPackages.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            // Payment Card
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Payment Details", style = MaterialTheme.typography.titleMedium, color = Primary)
                    
                    Text("Status", style = MaterialTheme.typography.bodyMedium)
                    ExposedDropdownMenuBoxContainer(
                        items = statuses.map { it.name },
                        selectedItem = statuses.find { it.code == selectedStatus }?.name ?: "",
                        onItemSelected = { name -> 
                            val code = statuses.find { it.name == name }?.code
                            if (code != null) viewModel.selectedStatus.value = code
                        }
                    )
                    
                    Text("Payment Method", style = MaterialTheme.typography.bodyMedium)
                    ExposedDropdownMenuBoxContainer(
                        items = paymentMethods.map { it.name },
                        selectedItem = paymentMethods.find { it.code == selectedPaymentMethod }?.name ?: "",
                        onItemSelected = { name -> 
                            val code = paymentMethods.find { it.name == name }?.code
                            if (code != null) viewModel.selectedPaymentMethod.value = code
                        }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasDownPayment, onCheckedChange = { viewModel.hasDownPayment.value = it; viewModel.recalculateTotal() })
                        Text("Add Down Payment (DP)?")
                    }

                    if (hasDownPayment) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !isDpPercentage, onClick = { viewModel.isDpPercentage.value = false; viewModel.recalculateTotal() }, label = { Text("Amount (Rp)") })
                            FilterChip(selected = isDpPercentage, onClick = { viewModel.isDpPercentage.value = true; viewModel.recalculateTotal() }, label = { Text("Percentage (%)") })
                        }
                        OutlinedTextField(
                            value = dpInputValue,
                            onValueChange = { viewModel.dpInputValue.value = it; viewModel.recalculateTotal() },
                            label = { Text(if (isDpPercentage) "DP %" else "DP Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notes.value = it },
                        label = { Text("Notes") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxContainer(items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BottomSummaryBar(totalAmount: Long, dpAmount: Long, isSubmitting: Boolean, onSubmit: () -> Unit) {
    Surface(
        color = Surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Text(Formatters.currency(totalAmount), style = MaterialTheme.typography.titleMedium, color = Primary)
            }
            if (dpAmount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DP/Paid", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(Formatters.currency(dpAmount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
                val remaining = maxOf(0L, totalAmount - dpAmount)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Text(Formatters.currency(remaining), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Reservation")
                }
            }
        }
    }
}
