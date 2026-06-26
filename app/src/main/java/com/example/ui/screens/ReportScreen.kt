package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodels.ReportViewModel
import com.example.utils.Formatters
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.local.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalBookings by viewModel.totalBookings.collectAsState()
    val totalNights by viewModel.totalNights.collectAsState()
    val totalGuests by viewModel.totalGuests.collectAsState()
    val cancelledBookings by viewModel.cancelledBookings.collectAsState()
    val allBookingsCount by viewModel.allBookingsCount.collectAsState()
    
    val roomRevenue by viewModel.roomRevenue.collectAsState()
    val extraBedRevenue by viewModel.extraBedRevenue.collectAsState()
    val servicesRevenue by viewModel.servicesRevenue.collectAsState()
    val foodsRevenue by viewModel.foodsRevenue.collectAsState()
    val aulaRevenue by viewModel.aulaRevenue.collectAsState()

    val dailyDetails by viewModel.dailyDetails.collectAsState()
    val unitOccupancy by viewModel.unitOccupancy.collectAsState()
    val guestTypeOccupancy by viewModel.guestTypeOccupancy.collectAsState()
    
    val totalPaid by viewModel.totalPaid.collectAsState()
    val totalUnpaid by viewModel.totalUnpaid.collectAsState()
    val totalDPMissing by viewModel.totalDPMissing.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val paymentStats by viewModel.paymentStats.collectAsState()
    
    val aulaBookingCount by viewModel.aulaBookingCount.collectAsState()
    val aulaEventStats by viewModel.aulaEventStats.collectAsState()
    
    val averageLeadTimeDays by viewModel.averageLeadTimeDays.collectAsState()
    val repeatGuestRate by viewModel.repeatGuestRate.collectAsState()
    val topGuests by viewModel.topGuests.collectAsState()

    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    
    val dfMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val dfFull = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val selectedGuestType by viewModel.selectedGuestType.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = OnBackgroundLight
                ),
                actions = {
                    IconButton(onClick = { 
                        exportToPdfAndShare(
                            context = context, 
                            periodStr = "${dfFull.format(Date(startDate))} - ${dfFull.format(Date(endDate))}",
                            viewModel = viewModel,
                            dailyDetails = dailyDetails,
                            unitOccupancy = unitOccupancy,
                            onError = { errorMsg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        ) 
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = Primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BackgroundLight,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Periode: ${dfFull.format(Date(startDate))} - ${dfFull.format(Date(endDate))}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        val filters = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Bulan Lalu", "Tahun Ini")
                        items(filters.size) { i ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.setQuickFilter(filters[i]) },
                                label = { Text(filters[i]) }
                            )
                        }
                    }
                    
                    // Simple dropdowns for Unit, Guest, Status could be here, but for brevity just showing state
                    Text("Unit: $selectedUnit | Guest: $selectedGuestType | Status: $selectedStatus", style = MaterialTheme.typography.bodySmall, color = TextSubtitle)
                }
            }
            
            // F2: Summary KPI Cards
            item {
                Text("Summary KPI", style = MaterialTheme.typography.titleMedium, color = Primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Days in period
                val daysInPeriod = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
                val totalAvailableNights = 10 * daysInPeriod // 10 rooms
                
                val occupancyRate = if (totalAvailableNights > 0) (totalNights.toDouble() / totalAvailableNights) * 100 else 0.0
                val roomRevTotal = roomRevenue + extraBedRevenue
                val adr = if (totalNights > 0) roomRevTotal / totalNights else 0L
                val revPAR = if (totalAvailableNights > 0) roomRevTotal / totalAvailableNights else 0L
                val cancelRate = if (allBookingsCount > 0) (cancelledBookings.toDouble() / allBookingsCount) * 100 else 0.0

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { KPICard("Pendapatan", Formatters.currency(totalRevenue)) }
                    item { KPICard("Booking", totalBookings.toString()) }
                    item { KPICard("Total Kamar", totalNights.toString()) }
                    item { KPICard("Occupancy", String.format(Locale.US, "%.1f%%", occupancyRate)) }
                    item { KPICard("ADR", Formatters.currency(adr)) }
                    item { KPICard("RevPAR", Formatters.currency(revPAR)) }
                    item { KPICard("Tamu", totalGuests.toString()) }
                    item { KPICard("Cancel Rate", String.format(Locale.US, "%.1f%%", cancelRate)) }
                }
            }
            
            // F3: Revenue Breakdown
            item {
                Text("Revenue Breakdown", style = MaterialTheme.typography.titleMedium, color = Primary)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RevenueRow("Kamar", roomRevenue)
                        RevenueRow("Extra Bed", extraBedRevenue)
                        RevenueRow("Layanan", servicesRevenue)
                        RevenueRow("Makanan", foodsRevenue)
                        RevenueRow("Aula", aulaRevenue)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RevenueRow("Total", totalRevenue, isBold = true)
                    }
                }
            }

            // F5: Occupancy Report
            item {
                Text("Occupancy Per Unit", style = MaterialTheme.typography.titleMedium, color = Primary)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (unitOccupancy.isEmpty()) {
                            Text("No data")
                        } else {
                            unitOccupancy.forEach { u ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val name = u.unit_name ?: u.unit_code
                                    Text("$name (${u.unit_code})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${u.total_nights} nights - ${Formatters.currency(u.revenue)}", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
            
            // F6: Payment Report
            item {
                Text("Payment Report", style = MaterialTheme.typography.titleMedium, color = Primary)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RevenueRow("Total Lunas", totalPaid)
                        RevenueRow("Belum Lunas", totalUnpaid)
                        RevenueRow("DP Kurang", totalDPMissing)
                        Text("Overdue: $overdueCount bookings", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // F7: Guest Analytics
            item {
                Text("Guest Analytics", style = MaterialTheme.typography.titleMedium, color = Primary)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Repeat Rate: ${String.format(Locale.US, "%.1f%%", repeatGuestRate * 100)}")
                        Text("Avg Lead Time: ${String.format(Locale.US, "%.1f", averageLeadTimeDays)} days")
                    }
                }
            }
            
            // F8: Aula Section
            item {
                Text("Aula (A001) Special Report", style = MaterialTheme.typography.titleMedium, color = Primary)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Booking: $aulaBookingCount")
                        Text("Revenue: ${Formatters.currency(aulaRevenue)}")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun KPICard(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSubtitle)
        }
    }
}

@Composable
fun RevenueRow(label: String, amount: Long, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(Formatters.currency(amount), fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

fun exportToPdfAndShare(
    context: Context, 
    periodStr: String, 
    viewModel: ReportViewModel,
    dailyDetails: List<DailyDetailView>,
    unitOccupancy: List<UnitOccupancyView>,
    onError: (String) -> Unit
) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        
        // Calculations
        val startMillis = viewModel.startDate.value
        val endMillis = viewModel.endDate.value
        val daysInPeriod = ((endMillis - startMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
        val totalAvailableNights = 10 * daysInPeriod
        
        val totalRevenueVal = viewModel.totalRevenue.value
        val totalBookingsVal = viewModel.totalBookings.value
        val totalNightsVal = viewModel.totalNights.value
        val totalGuestsVal = viewModel.totalGuests.value
        val cancelledBookingsVal = viewModel.cancelledBookings.value
        val allBookingsCountVal = viewModel.allBookingsCount.value
        
        val occupancyRate = if (totalAvailableNights > 0) (totalNightsVal.toDouble() / totalAvailableNights) * 100 else 0.0
        
        val roomRevenueVal = viewModel.roomRevenue.value
        val extraBedRevenueVal = viewModel.extraBedRevenue.value
        val servicesRevenueVal = viewModel.servicesRevenue.value
        val foodsRevenueVal = viewModel.foodsRevenue.value
        val aulaRevenueVal = viewModel.aulaRevenue.value
        
        val roomRevTotal = roomRevenueVal + extraBedRevenueVal
        val adr = if (totalNightsVal > 0) roomRevTotal / totalNightsVal else 0L
        val revPAR = if (totalAvailableNights > 0) roomRevTotal / totalAvailableNights else 0L
        val cancelRate = if (allBookingsCountVal > 0) (cancelledBookingsVal.toDouble() / allBookingsCountVal) * 100 else 0.0
        
        val paymentStatsVal = viewModel.paymentStats.value
        
        // Paint definitions
        val paintFillYellow = Paint().apply { color = android.graphics.Color.rgb(255, 255, 0); style = Paint.Style.FILL }
        val paintFillGreen = Paint().apply { color = android.graphics.Color.rgb(0, 255, 0); style = Paint.Style.FILL }
        val paintFillWhite = Paint().apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
        val paintStroke = Paint().apply { color = android.graphics.Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        
        val paintTextTitle = Paint().apply { color = android.graphics.Color.BLACK; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintTextHeader = Paint().apply { color = android.graphics.Color.BLACK; textSize = 9f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintTextSubheader = Paint().apply { color = android.graphics.Color.BLACK; textSize = 8f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintTextSubheaderItalic = Paint().apply { color = android.graphics.Color.BLACK; textSize = 8f; isFakeBoldText = true; textSkewX = -0.25f; textAlign = Paint.Align.CENTER }
        
        val paintTextNormal = Paint().apply { color = android.graphics.Color.BLACK; textSize = 8f; textAlign = Paint.Align.LEFT }
        val paintTextNormalCenter = Paint().apply { color = android.graphics.Color.BLACK; textSize = 8f; textAlign = Paint.Align.CENTER }
        val paintTextNormalRight = Paint().apply { color = android.graphics.Color.BLACK; textSize = 8f; textAlign = Paint.Align.RIGHT }
        val paintFooter = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 7.5f; textAlign = Paint.Align.LEFT }
        
        val tsDf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val df = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        
        // Title Banner (Page 1)
        drawRectWithBorder(canvas, 30f, 40f, 565f, 80f, paintFillYellow, paintStroke)
        drawTextCellCentered(canvas, "LAPORAN BULANAN SALIGURI COTTAGE HARAU - ${periodStr.uppercase(Locale.getDefault())}", 30f, 565f, 40f, 80f, paintTextTitle)
        
        // SUMMARY Table Title
        drawRectWithBorder(canvas, 30f, 95f, 565f, 115f, paintFillYellow, paintStroke)
        drawTextCellCentered(canvas, "SUMMARY", 30f, 565f, 95f, 115f, paintTextHeader)
        
        // SUMMARY Table Headers
        drawRectWithBorder(canvas, 30f, 115f, 565f, 135f, paintFillGreen, paintStroke)
        val summaryHeaders = listOf("Pendapatan", "Booking", "Total Kamar", "Occupancy", "ADR", "RevPAR", "Jumlah Tamu", "Cancel Rate")
        val xCoordsSummary = (0..8).map { 30f + it * 66.875f }
        for (i in 0 until summaryHeaders.size) {
            drawTextCellCentered(canvas, summaryHeaders[i], xCoordsSummary[i], xCoordsSummary[i+1], 115f, 135f, paintTextSubheader)
        }
        
        // SUMMARY Table Data
        drawRectWithBorder(canvas, 30f, 135f, 565f, 155f, paintFillWhite, paintStroke)
        for (i in 1 until xCoordsSummary.size) {
            canvas.drawLine(xCoordsSummary[i], 135f, xCoordsSummary[i], 155f, paintStroke)
        }
        drawTextCellCentered(canvas, formatPdfValue(totalRevenueVal), xCoordsSummary[0], xCoordsSummary[1], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, totalBookingsVal.toString(), xCoordsSummary[1], xCoordsSummary[2], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, totalNightsVal.toString(), xCoordsSummary[2], xCoordsSummary[3], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, String.format(Locale.US, "%.2f%%", occupancyRate), xCoordsSummary[3], xCoordsSummary[4], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(adr), xCoordsSummary[4], xCoordsSummary[5], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(revPAR), xCoordsSummary[5], xCoordsSummary[6], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, totalGuestsVal.toString(), xCoordsSummary[6], xCoordsSummary[7], 135f, 155f, paintTextNormalCenter)
        drawTextCellCentered(canvas, String.format(Locale.US, "%.2f%%", cancelRate), xCoordsSummary[7], xCoordsSummary[8], 135f, 155f, paintTextNormalCenter)
        
        // REVENUE BREAKDOWN Table Title
        drawRectWithBorder(canvas, 30f, 175f, 565f, 195f, paintFillYellow, paintStroke)
        drawTextCellCentered(canvas, "REVENUE BREAKDOWN", 30f, 565f, 175f, 195f, paintTextHeader)
        
        // REVENUE BREAKDOWN Table Headers
        drawRectWithBorder(canvas, 30f, 195f, 565f, 215f, paintFillGreen, paintStroke)
        val breakdownHeaders = listOf("Kamar", "Extra Bed", "Layanan", "Makanan", "Aula", "Total")
        val xCoordsBreakdown = (0..6).map { 30f + it * 89.167f }
        for (i in 0 until breakdownHeaders.size) {
            drawTextCellCentered(canvas, breakdownHeaders[i], xCoordsBreakdown[i], xCoordsBreakdown[i+1], 195f, 215f, paintTextSubheader)
        }
        
        // REVENUE BREAKDOWN Table Data
        drawRectWithBorder(canvas, 30f, 215f, 565f, 235f, paintFillWhite, paintStroke)
        for (i in 1 until xCoordsBreakdown.size) {
            canvas.drawLine(xCoordsBreakdown[i], 215f, xCoordsBreakdown[i], 235f, paintStroke)
        }
        drawTextCellCentered(canvas, formatPdfValue(roomRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[0], xCoordsBreakdown[1], 215f, 235f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(extraBedRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[1], xCoordsBreakdown[2], 215f, 235f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(servicesRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[2], xCoordsBreakdown[3], 215f, 235f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(foodsRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[3], xCoordsBreakdown[4], 215f, 235f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(aulaRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[4], xCoordsBreakdown[5], 215f, 235f, paintTextNormalCenter)
        drawTextCellCentered(canvas, formatPdfValue(totalRevenueVal, useZeroAsRaw = true), xCoordsBreakdown[5], xCoordsBreakdown[6], 215f, 235f, paintTextNormalCenter)
        
        // Side-by-Side Tables
        // Left: Occupancy Per Unit Table
        val xCoordsOcc = listOf(30f, 130f, 175f, 260f)
        
        // Occupancy Per Unit Headers (Height 40f)
        drawRectWithBorder(canvas, 30f, 255f, 260f, 295f, paintFillYellow, paintStroke)
        canvas.drawLine(130f, 255f, 130f, 295f, paintStroke)
        canvas.drawLine(175f, 255f, 175f, 295f, paintStroke)
        
        val midYHeader = (255f + 295f) / 2
        canvas.drawText("OccupancyPer", (30f + 130f)/2, midYHeader - 3f, paintTextSubheader)
        canvas.drawText("Unit", (30f + 130f)/2, midYHeader + 7f, paintTextSubheader)
        
        canvas.drawText("Jumlah", (130f + 175f)/2, midYHeader - 3f, paintTextSubheader)
        canvas.drawText("Malam", (130f + 175f)/2, midYHeader + 7f, paintTextSubheader)
        
        drawTextCellCentered(canvas, "Total Harga", 175f, 260f, 255f, 295f, paintTextSubheader)
        
        // Left Occupancy Per Unit Rows
        val occMap = unitOccupancy.associateBy { it.unit_code }
        val allUnitCodesAndNames = listOf(
            "V001" to "Room 1",
            "V002" to "Room 2",
            "V003" to "Room 3",
            "V004" to "Room 4",
            "V005" to "Room 5",
            "V006" to "Room 6",
            "V007" to "Room 7",
            "V008" to "Room 8",
            "V009" to "Villa 9",
            "V010" to "Villa 10",
            "A001" to "Aula"
        )
        
        allUnitCodesAndNames.forEachIndexed { index, pair ->
            val code = pair.first
            val name = pair.second
            val occ = occMap[code]
            val nights = occ?.total_nights ?: 0
            val rev = occ?.revenue ?: 0L
            
            val rowTop = 295f + index * 15f
            val rowBottom = rowTop + 15f
            
            drawRectWithBorder(canvas, 30f, rowTop, 260f, rowBottom, paintFillWhite, paintStroke)
            canvas.drawLine(130f, rowTop, 130f, rowBottom, paintStroke)
            canvas.drawLine(175f, rowTop, 175f, rowBottom, paintStroke)
            
            drawTextCellLeft(canvas, name, 30f, 130f, rowTop, rowBottom, 5f, paintTextNormal)
            drawTextCellCentered(canvas, nights.toString(), 130f, 175f, rowTop, rowBottom, paintTextNormalCenter)
            drawTextCellRight(canvas, formatPdfValue(rev), 175f, 260f, rowTop, rowBottom, 5f, paintTextNormalRight)
        }
        
        // Right: Payment Report Table
        val xCoordsPay = listOf(290f, 415f, 565f)
        
        // Title header row
        drawRectWithBorder(canvas, 290f, 255f, 565f, 275f, paintFillYellow, paintStroke)
        drawTextCellCentered(canvas, "Payment Report", 290f, 565f, 255f, 275f, paintTextHeader)
        
        // Column headers row
        drawRectWithBorder(canvas, 290f, 275f, 565f, 300f, paintFillGreen, paintStroke)
        canvas.drawLine(415f, 275f, 415f, 300f, paintStroke)
        
        val midYPayHeader = (275f + 300f) / 2
        canvas.drawText("Metode", (290f + 415f)/2, midYPayHeader - 3f, paintTextSubheaderItalic)
        canvas.drawText("Pembayaran", (290f + 415f)/2, midYPayHeader + 7f, paintTextSubheaderItalic)
        
        drawTextCellCentered(canvas, "Total Lunas", 415f, 565f, 275f, 300f, paintTextSubheaderItalic)
        
        val activePayments = paymentStatsVal.filter { it.amount > 0 }
        activePayments.forEachIndexed { index, stat ->
            val rowTop = 300f + index * 15f
            val rowBottom = rowTop + 15f
            
            drawRectWithBorder(canvas, 290f, rowTop, 565f, rowBottom, paintFillWhite, paintStroke)
            canvas.drawLine(415f, rowTop, 415f, rowBottom, paintStroke)
            
            drawTextCellLeft(canvas, getPaymentMethodNameByCode(stat.method), 290f, 415f, rowTop, rowBottom, 5f, paintTextNormal)
            drawTextCellRight(canvas, formatPdfValue(stat.amount), 415f, 565f, rowTop, rowBottom, 5f, paintTextNormalRight)
        }
        
        // DAILY REPORT Table Title & Headers (Page 1)
        val xCoordsDaily = listOf(30f, 105f, 185f, 265f, 315f, 395f, 475f, 565f)
        
        drawRectWithBorder(canvas, 30f, 480f, 565f, 500f, paintFillYellow, paintStroke)
        drawTextCellCentered(canvas, "DAILY REPORT", 30f, 565f, 480f, 500f, paintTextHeader)
        
        drawRectWithBorder(canvas, 30f, 500f, 565f, 520f, paintFillGreen, paintStroke)
        val dailyHeaders = listOf("Tanggal", "Kamar", "Harga", "Extrabed", "Harga", "Total Harga", "Keterangan")
        for (i in 0 until dailyHeaders.size) {
            drawTextCellCentered(canvas, dailyHeaders[i], xCoordsDaily[i], xCoordsDaily[i+1], 500f, 520f, paintTextSubheader)
        }
        
        // Compute total pages
        val totalItems = dailyDetails.size
        val totalPages = if (totalItems <= 18) 1 else 1 + ((totalItems - 18 + 44) / 45)
        var currentPage = 1
        
        // Draw up to 18 rows on Page 1
        for (rowIndex in 0 until 18) {
            val rowTop = 520f + rowIndex * 15f
            val rowBottom = rowTop + 15f
            
            drawRectWithBorder(canvas, 30f, rowTop, 565f, rowBottom, paintFillWhite, paintStroke)
            for (i in 1 until xCoordsDaily.size) {
                canvas.drawLine(xCoordsDaily[i], rowTop, xCoordsDaily[i], rowBottom, paintStroke)
            }
            
            if (rowIndex < totalItems) {
                val detail = dailyDetails[rowIndex]
                
                drawTextCellCentered(canvas, df.format(Date(detail.date)), xCoordsDaily[0], xCoordsDaily[1], rowTop, rowBottom, paintTextNormalCenter)
                drawTextCellCentered(canvas, getUnitNameByCode(detail.unit_code), xCoordsDaily[1], xCoordsDaily[2], rowTop, rowBottom, paintTextNormalCenter)
                drawTextCellCentered(canvas, formatPdfValue(detail.nightly_rate), xCoordsDaily[2], xCoordsDaily[3], rowTop, rowBottom, paintTextNormalCenter)
                drawTextCellCentered(canvas, detail.extra_bed_count.toString(), xCoordsDaily[3], xCoordsDaily[4], rowTop, rowBottom, paintTextNormalCenter)
                drawTextCellCentered(canvas, formatPdfValue(detail.extra_bed_price), xCoordsDaily[4], xCoordsDaily[5], rowTop, rowBottom, paintTextNormalCenter)
                drawTextCellCentered(canvas, formatPdfValue(detail.total), xCoordsDaily[5], xCoordsDaily[6], rowTop, rowBottom, paintTextNormalCenter)
                
                var desc = detail.description
                if (desc.length > 25) desc = desc.substring(0, 22) + "..."
                drawTextCellLeft(canvas, desc, xCoordsDaily[6], xCoordsDaily[7], rowTop, rowBottom, 5f, paintTextNormal)
            }
        }
        
        // Draw Page 1 Footer
        canvas.drawText("Dicetak: ${tsDf.format(Date())} | Halaman 1 dari $totalPages", 30f, 810f, paintFooter)
        document.finishPage(page)
        
        // Draw subsequent pages if there are more daily details
        while (currentPage < totalPages) {
            currentPage++
            page = document.startPage(pageInfo)
            canvas = page.canvas
            
            // Draw DAILY REPORT (Lanjutan) Title Box
            drawRectWithBorder(canvas, 30f, 50f, 565f, 70f, paintFillYellow, paintStroke)
            drawTextCellCentered(canvas, "DAILY REPORT (Lanjutan)", 30f, 565f, 50f, 70f, paintTextHeader)
            
            // Draw Subheaders row
            drawRectWithBorder(canvas, 30f, 70f, 565f, 90f, paintFillGreen, paintStroke)
            for (i in 0 until dailyHeaders.size) {
                drawTextCellCentered(canvas, dailyHeaders[i], xCoordsDaily[i], xCoordsDaily[i+1], 70f, 90f, paintTextSubheader)
            }
            
            // Draw up to 45 rows on this subsequent page
            for (rowIndex in 0 until 45) {
                val realItemIndex = 18 + (currentPage - 2) * 45 + rowIndex
                val rowTop = 90f + rowIndex * 15f
                val rowBottom = rowTop + 15f
                
                drawRectWithBorder(canvas, 30f, rowTop, 565f, rowBottom, paintFillWhite, paintStroke)
                for (i in 1 until xCoordsDaily.size) {
                    canvas.drawLine(xCoordsDaily[i], rowTop, xCoordsDaily[i], rowBottom, paintStroke)
                }
                
                if (realItemIndex < totalItems) {
                    val detail = dailyDetails[realItemIndex]
                    
                    drawTextCellCentered(canvas, df.format(Date(detail.date)), xCoordsDaily[0], xCoordsDaily[1], rowTop, rowBottom, paintTextNormalCenter)
                    drawTextCellCentered(canvas, getUnitNameByCode(detail.unit_code), xCoordsDaily[1], xCoordsDaily[2], rowTop, rowBottom, paintTextNormalCenter)
                    drawTextCellCentered(canvas, formatPdfValue(detail.nightly_rate), xCoordsDaily[2], xCoordsDaily[3], rowTop, rowBottom, paintTextNormalCenter)
                    drawTextCellCentered(canvas, detail.extra_bed_count.toString(), xCoordsDaily[3], xCoordsDaily[4], rowTop, rowBottom, paintTextNormalCenter)
                    drawTextCellCentered(canvas, formatPdfValue(detail.extra_bed_price), xCoordsDaily[4], xCoordsDaily[5], rowTop, rowBottom, paintTextNormalCenter)
                    drawTextCellCentered(canvas, formatPdfValue(detail.total), xCoordsDaily[5], xCoordsDaily[6], rowTop, rowBottom, paintTextNormalCenter)
                    
                    var desc = detail.description
                    if (desc.length > 25) desc = desc.substring(0, 22) + "..."
                    drawTextCellLeft(canvas, desc, xCoordsDaily[6], xCoordsDaily[7], rowTop, rowBottom, 5f, paintTextNormal)
                }
            }
            
            // Draw subsequent Page Footer
            canvas.drawText("Dicetak: ${tsDf.format(Date())} | Halaman $currentPage dari $totalPages", 30f, 810f, paintFooter)
            document.finishPage(page)
        }
        
        val fileName = "Saliguri_Report_${periodStr.replace("/", "-").replace(" ", "")}.pdf"
        val file = File(context.cacheDir, fileName)
        val out = FileOutputStream(file)
        document.writeTo(out)
        document.close()
        out.close()
 
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Saliguri - $periodStr")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Gagal membuat PDF")
    }
}

private fun drawRectWithBorder(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, fillPaint: Paint, strokePaint: Paint) {
    canvas.drawRect(left, top, right, bottom, fillPaint)
    canvas.drawRect(left, top, right, bottom, strokePaint)
}

private fun drawTextCellCentered(canvas: Canvas, text: String, left: Float, right: Float, top: Float, bottom: Float, paint: Paint) {
    val x = (left + right) / 2
    val textHeight = paint.descent() - paint.ascent()
    val textOffset = textHeight / 2 - paint.descent()
    val y = (top + bottom) / 2 + textOffset
    val oldAlign = paint.textAlign
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(text, x, y, paint)
    paint.textAlign = oldAlign
}

private fun drawTextCellLeft(canvas: Canvas, text: String, left: Float, right: Float, top: Float, bottom: Float, padding: Float, paint: Paint) {
    val x = left + padding
    val textHeight = paint.descent() - paint.ascent()
    val textOffset = textHeight / 2 - paint.descent()
    val y = (top + bottom) / 2 + textOffset
    val oldAlign = paint.textAlign
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText(text, x, y, paint)
    paint.textAlign = oldAlign
}

private fun drawTextCellRight(canvas: Canvas, text: String, left: Float, right: Float, top: Float, bottom: Float, padding: Float, paint: Paint) {
    val x = right - padding
    val textHeight = paint.descent() - paint.ascent()
    val textOffset = textHeight / 2 - paint.descent()
    val y = (top + bottom) / 2 + textOffset
    val oldAlign = paint.textAlign
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(text, x, y, paint)
    paint.textAlign = oldAlign
}

private fun formatPdfValue(amount: Long, useZeroAsRaw: Boolean = false): String {
    if (amount == 0L && useZeroAsRaw) return "0"
    val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount)
    return "Rp$formatted"
}

private fun getUnitNameByCode(code: String): String {
    return when (code) {
        "V001" -> "Room 1"
        "V002" -> "Room 2"
        "V003" -> "Room 3"
        "V004" -> "Room 4"
        "V005" -> "Room 5"
        "V006" -> "Room 6"
        "V007" -> "Room 7"
        "V008" -> "Room 8"
        "V009" -> "Villa 9"
        "V010" -> "Villa 10"
        "A001" -> "Aula"
        else -> code
    }
}

private fun getPaymentMethodNameByCode(code: String): String {
    return when (code) {
        "PM001" -> "Cash"
        "PM002" -> "Transfer BCA"
        "PM003" -> "Transfer BRI"
        "PM004" -> "Transfer Mandiri"
        "PM005" -> "QRIS"
        "Bank Transfer (BCA)" -> "Transfer BCA"
        "Bank Transfer (Mandiri)" -> "Transfer Mandiri"
        "Credit Card" -> "Credit Card"
        else -> code
    }
}

