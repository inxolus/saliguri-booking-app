package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.navigation.NavController
import androidx.compose.foundation.clickable

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

fun formatToYmd(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    if (millis % 86400000L == 0L) {
        sdf.timeZone = TimeZone.getTimeZone("UTC")
    } else {
        sdf.timeZone = TimeZone.getDefault()
    }
    return sdf.format(Date(millis))
}

enum class CalendarStatus {
    AVAILABLE, CHECK_IN, OCCUPIED, CHECK_OUT, BLOCKED, CLEANING
}

data class CellData(
    val status: CalendarStatus,
    val reservation: com.example.data.models.CalendarReservation?
)

fun getCalendarCellData(
    unit: com.example.data.models.UnitModel,
    dateMillis: Long,
    reservations: List<com.example.data.models.CalendarReservation>
): CellData {
    if (unit.statusCode == "RM004") return CellData(CalendarStatus.BLOCKED, null)
    if (unit.statusCode == "RM002") return CellData(CalendarStatus.CLEANING, null)
    
    val dateStr = formatToYmd(dateMillis)
    val unitReservations = reservations.filter { it.unit_code == unit.unitCode }
    
    val checkingIn = unitReservations.find { formatToYmd(it.check_in_date) == dateStr && it.status_code != "RS004" }
    val checkingOut = unitReservations.find { formatToYmd(it.check_out_date) == dateStr }
    val staying = unitReservations.find { 
        val cin = formatToYmd(it.check_in_date)
        val cout = formatToYmd(it.check_out_date)
        dateStr > cin && dateStr < cout && it.status_code != "RS004"
    }
    
    return when {
        checkingIn != null && checkingOut != null -> {
            if (checkingOut.status_code == "RS003") {
                CellData(CalendarStatus.CHECK_OUT, checkingOut)
            } else {
                if (checkingIn.status_code == "RS003") {
                    CellData(CalendarStatus.OCCUPIED, checkingIn)
                } else {
                    CellData(CalendarStatus.CHECK_IN, checkingIn)
                }
            }
        }
        checkingIn != null -> {
            if (checkingIn.status_code == "RS003") {
                CellData(CalendarStatus.OCCUPIED, checkingIn)
            } else if (checkingIn.status_code == "RS002") {
                CellData(CalendarStatus.CHECK_IN, checkingIn)
            } else {
                CellData(CalendarStatus.AVAILABLE, null)
            }
        }
        checkingOut != null -> {
            if (checkingOut.status_code == "RS003" || checkingOut.status_code == "RS004") {
                CellData(CalendarStatus.CHECK_OUT, checkingOut)
            } else {
                CellData(CalendarStatus.AVAILABLE, null)
            }
        }
        staying != null -> {
            if (staying.status_code == "RS003") {
                CellData(CalendarStatus.OCCUPIED, staying)
            } else {
                CellData(CalendarStatus.AVAILABLE, null)
            }
        }
        else -> CellData(CalendarStatus.AVAILABLE, null)
    }
}

@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(Color(0xFF4CAF50), "Tersedia")
        LegendItem(Color(0xFFFF9800), "Check-In")
        LegendItem(Color(0xFFF44336), "Terisi")
        LegendItem(Color(0xFF2196F3), "Check-Out")
        LegendItem(Color(0xFF424242), "Diblok")
        LegendItem(Color(0xFFFFEB3B), "Bersihkan")
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = TextSubtitle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    var startDateMillis by remember { mutableStateOf(getStartOfTodayMillis()) }
    val allUnits by viewModel.units.collectAsState(initial = emptyList())
    var reservations by remember { mutableStateOf<List<com.example.data.models.CalendarReservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedReservation by remember { mutableStateOf<com.example.data.models.CalendarReservation?>(null) }
    var selectedUnit by remember { mutableStateOf<com.example.data.models.UnitModel?>(null) }
    var showCheckInSheet by remember { mutableStateOf(false) }
    var showCheckOutSheet by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showCleaningDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshCalendar() {
        coroutineScope.launch {
            isLoading = true
            val endDateMillis = startDateMillis + (7 * 86400000L)
            reservations = viewModel.getReservationsForCalendar(startDateMillis, endDateMillis).first()
            isLoading = false
        }
    }

    LaunchedEffect(startDateMillis) {
        refreshCalendar()
    }

    LaunchedEffect(Unit) {
        com.example.utils.EventBus.events.collect { event ->
            refreshCalendar()
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshCalendar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val numDays = 7
    val formatDayOfWeek = SimpleDateFormat("EEE", Locale.getDefault())
    val formatDayOfMonth = SimpleDateFormat("dd", Locale.getDefault())

    val dateList = (0 until numDays).map { i ->
        startDateMillis + (i * 86400000L)
    }

    if (showCheckInSheet && selectedReservation != null) {
        ModalBottomSheet(onDismissRequest = { showCheckInSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tamu ${selectedReservation?.guest_name ?: "Unknown"} check-in hari ini", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    viewModel.updateReservationStatus(selectedReservation!!.reservation_id, "RS003")
                    showCheckInSheet = false
                    refreshCalendar()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Check-In Now")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCheckOutSheet && selectedReservation != null) {
        ModalBottomSheet(onDismissRequest = { showCheckOutSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedReservation!!.status_code == "RS004") {
                    Text("Tamu ${selectedReservation?.guest_name ?: "Unknown"} sudah check-out", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reservasi ini telah selesai.", style = MaterialTheme.typography.bodyMedium, color = TextSubtitle)
                } else {
                    Text("Tamu ${selectedReservation?.guest_name ?: "Unknown"} check-out hari ini", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.updateReservationStatus(selectedReservation!!.reservation_id, "RS004")
                        showCheckOutSheet = false
                        refreshCalendar()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Check-Out Now")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showBlockedDialog && selectedUnit != null) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Kamar Diblok") },
            text = { Text("Kamar ini sedang diblok. Buka blok sekarang?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateUnitStatus(selectedUnit!!.unitCode, "RM001") // Available
                    showBlockedDialog = false
                }) { Text("Buka Blok") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showCleaningDialog && selectedUnit != null) {
        AlertDialog(
            onDismissRequest = { showCleaningDialog = false },
            title = { Text("Membersihkan Kamar") },
            text = { Text("Kamar sedang dibersihkan. Selesai bersihkan?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateUnitStatus(selectedUnit!!.unitCode, "RM001") // Available
                    showCleaningDialog = false
                }) { Text("Selesai Bersihkan") }
            },
            dismissButton = {
                TextButton(onClick = { showCleaningDialog = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = OnBackgroundLight
                )
            )
        },
        containerColor = BackgroundLight,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { startDateMillis -= (7 * 86400000L) }) {
                        Icon(Icons.Filled.ChevronLeft, "Previous Week")
                    }
                    Text(
                        text = "${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(startDateMillis))} - ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(startDateMillis + 6 * 86400000L))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { startDateMillis += (7 * 86400000L) }) {
                        Icon(Icons.Filled.ChevronRight, "Next Week")
                    }
                }

                // Calendar Matrix Header
                Row(modifier = Modifier.fillMaxWidth().padding(start = 80.dp)) {
                    dateList.forEach { dateInMillis ->
                        val date = Date(dateInMillis)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = formatDayOfWeek.format(date), fontSize = 12.sp, color = TextSubtitle)
                            Text(text = formatDayOfMonth.format(date), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider()

                // Calendar Data
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allUnits) { unit ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unit Name Col
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(unit.unitName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }

                                // Day Cols
                                dateList.forEach { dateInMillis ->
                                    val cellData = getCalendarCellData(unit, dateInMillis, reservations)
                                    val status = cellData.status
                                    val reservation = cellData.reservation
                                    val backgroundColor = when (status) {
                                        CalendarStatus.AVAILABLE -> Color(0xFF4CAF50)
                                        CalendarStatus.CHECK_IN -> Color(0xFFFF9800)
                                        CalendarStatus.OCCUPIED -> Color(0xFFF44336)
                                        CalendarStatus.CHECK_OUT -> Color(0xFF2196F3)
                                        CalendarStatus.BLOCKED -> Color(0xFF424242)
                                        CalendarStatus.CLEANING -> Color(0xFFFFEB3B)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .padding(2.dp)
                                            .background(backgroundColor, RoundedCornerShape(4.dp))
                                            .clickable {
                                                when (status) {
                                                    CalendarStatus.AVAILABLE -> navController.navigate("form?unit=${unit.unitCode}&date=${dateInMillis}")
                                                    CalendarStatus.CHECK_IN -> {
                                                        selectedReservation = reservation
                                                        showCheckInSheet = true
                                                    }
                                                    CalendarStatus.OCCUPIED -> {
                                                        if (reservation != null) {
                                                            navController.navigate("detail/${reservation.reservation_id}")
                                                        }
                                                    }
                                                    CalendarStatus.CHECK_OUT -> {
                                                        selectedReservation = reservation
                                                        showCheckOutSheet = true
                                                    }
                                                    CalendarStatus.BLOCKED -> {
                                                        selectedUnit = unit
                                                        showBlockedDialog = true
                                                    }
                                                    CalendarStatus.CLEANING -> {
                                                        selectedUnit = unit
                                                        showCleaningDialog = true
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (status) {
                                            CalendarStatus.CHECK_IN -> Text("IN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            CalendarStatus.CHECK_OUT -> Text("OUT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            CalendarStatus.BLOCKED -> Text("X", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = OutlineVariant)
                        }
                    }
                }
                CalendarLegend()
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable(enabled = false) {}, // Scrim background
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

fun getStartOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
