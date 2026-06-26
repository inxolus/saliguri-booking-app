package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val reportDao = db.reportDao()
    private val appDao = db.appDao()

    val startDate = MutableStateFlow(getStartOfMonthMillis())
    val endDate = MutableStateFlow(getEndOfMonthMillis())
    
    val selectedUnit = MutableStateFlow("ALL")
    val selectedGuestType = MutableStateFlow("ALL")
    val selectedStatus = MutableStateFlow("ALL")

    val totalRevenue = MutableStateFlow(0L)
    val totalBookings = MutableStateFlow(0)
    val totalNights = MutableStateFlow(0)
    val totalGuests = MutableStateFlow(0)
    val cancelledBookings = MutableStateFlow(0)
    val allBookingsCount = MutableStateFlow(0)
    
    val roomRevenue = MutableStateFlow(0L)
    val extraBedRevenue = MutableStateFlow(0L)
    val servicesRevenue = MutableStateFlow(0L)
    val foodsRevenue = MutableStateFlow(0L)
    val aulaRevenue = MutableStateFlow(0L)

    val dailyDetails = MutableStateFlow<List<DailyDetailView>>(emptyList())
    val unitOccupancy = MutableStateFlow<List<UnitOccupancyView>>(emptyList())
    val guestTypeOccupancy = MutableStateFlow<List<GuestTypeOccupancyView>>(emptyList())
    
    val totalPaid = MutableStateFlow(0L)
    val totalUnpaid = MutableStateFlow(0L)
    val totalDPMissing = MutableStateFlow(0L)
    val overdueCount = MutableStateFlow(0)
    val paymentStats = MutableStateFlow<List<PaymentMethodStatView>>(emptyList())
    
    val aulaBookingCount = MutableStateFlow(0)
    val aulaEventStats = MutableStateFlow<List<AulaEventStatView>>(emptyList())
    
    val averageLeadTimeDays = MutableStateFlow(0.0)
    val repeatGuestRate = MutableStateFlow(0.0)
    val topGuests = MutableStateFlow<List<TopGuestView>>(emptyList())

    init {
        viewModelScope.launch {
            combine(startDate, endDate, selectedUnit, selectedGuestType, selectedStatus) { s, e, u, gt, st ->
                loadReportData(s, e, u, gt, st)
            }.collect()
        }
    }

    fun setQuickFilter(type: String) {
        val cal = Calendar.getInstance()
        when (type) {
            "Hari Ini" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                startDate.value = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                endDate.value = cal.timeInMillis
            }
            "Minggu Ini" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                startDate.value = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                endDate.value = cal.timeInMillis
            }
            "Bulan Ini" -> {
                startDate.value = getStartOfMonthMillis()
                endDate.value = getEndOfMonthMillis()
            }
            "Bulan Lalu" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                startDate.value = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                endDate.value = cal.timeInMillis
            }
            "Tahun Ini" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                startDate.value = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                endDate.value = cal.timeInMillis
            }
        }
    }

    private fun loadReportData(start: Long, end: Long, unit: String, guestType: String, status: String) {
        viewModelScope.launch {
            reportDao.getTotalRevenue(start, end, unit, guestType, status).collect { totalRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getTotalBookings(start, end, unit, guestType, status).collect { totalBookings.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getTotalNights(start, end, unit, guestType, status).collect { totalNights.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getTotalGuests(start, end, unit, guestType, status).collect { totalGuests.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getCancelledBookings(start, end, unit, guestType, status).collect { cancelledBookings.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getAllBookingsCount(start, end, unit, guestType, status).collect { allBookingsCount.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getRoomRevenue(start, end, unit, guestType, status).collect { roomRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getExtraBedRevenue(start, end, unit, guestType, status).collect { extraBedRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getServicesRevenue(start, end, unit, guestType, status).collect { servicesRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getFoodsRevenue(start, end, unit, guestType, status).collect { foodsRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getAulaRevenue(start, end, unit, guestType, status).collect { aulaRevenue.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getDailyDetails(start, end, unit, guestType, status).collect { dailyDetails.value = it }
        }
        viewModelScope.launch {
            reportDao.getUnitOccupancy(start, end, unit, guestType, status).collect { unitOccupancy.value = it }
        }
        viewModelScope.launch {
            reportDao.getGuestTypeOccupancy(start, end, unit, guestType, status).collect { guestTypeOccupancy.value = it }
        }
        viewModelScope.launch {
            reportDao.getTotalPaid(start, end, unit, guestType, status).collect { totalPaid.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getTotalUnpaid(start, end, unit, guestType, status).collect { totalUnpaid.value = it ?: 0L }
        }
        viewModelScope.launch {
            reportDao.getTotalDPMissing(start, end, unit, guestType, status).collect { totalDPMissing.value = it ?: 0L }
        }
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            reportDao.getOverdueCount(start, end, today, unit, guestType, status).collect { overdueCount.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getPaymentMethodStats(start, end, unit, guestType, status).collect { paymentStats.value = it }
        }
        viewModelScope.launch {
            reportDao.getAulaBookingCount(start, end, guestType, status).collect { aulaBookingCount.value = it ?: 0 }
        }
        viewModelScope.launch {
            reportDao.getAulaEventStats(start, end, guestType, status).collect { aulaEventStats.value = it }
        }
        viewModelScope.launch {
            reportDao.getAverageLeadTimeDays(start, end).collect { averageLeadTimeDays.value = it ?: 0.0 }
        }
        viewModelScope.launch {
            reportDao.getRepeatGuestRate().collect { repeatGuestRate.value = it ?: 0.0 }
        }
        viewModelScope.launch {
            reportDao.getTopGuests(start, end).collect { topGuests.value = it }
        }
    }

    private fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
