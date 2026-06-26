package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    // Dashboard Stats
    private val _checkInToday = MutableStateFlow("00")
    val checkInToday: StateFlow<String> = _checkInToday
    
    private val _checkOutToday = MutableStateFlow("00")
    val checkOutToday: StateFlow<String> = _checkOutToday

    private val _occupiedUnits = MutableStateFlow("00")
    val occupiedUnits: StateFlow<String> = _occupiedUnits

    private val _pendingReservations = MutableStateFlow("00")
    val pendingReservations: StateFlow<String> = _pendingReservations

    val recentReservations = dao.getRecentReservationsFlow()
    val units = dao.getUnitsFlow()
    val guestTypes = dao.getGuestTypesFlow()

    suspend fun getOverlappingUnits(start: Long, end: Long) = dao.getOverlappingUnits(start, end)
    
    fun getReservationsForCalendar(start: Long, end: Long): Flow<List<com.example.data.models.CalendarReservation>> = dao.getReservationsForCalendar(start, end)

    fun getRevenueTotalFlow(start: Long, end: Long) = dao.getRevenueTotalFlow(start, end)
    fun getGuestTypeDistributionFlow(start: Long, end: Long) = dao.getGuestTypeDistributionFlow(start, end)

    fun updateReservationStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            dao.updateReservationStatus(id, newStatus)
            refreshStats()
            com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_STATUS_CHANGED)
        }
    }

    fun updateUnitStatus(unitCode: String, newStatus: String) {
        viewModelScope.launch {
            dao.updateUnitStatus(unitCode, newStatus)
            com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_STATUS_CHANGED)
        }
    }

    fun deleteReservation(id: String) {
        viewModelScope.launch {
            dao.deleteFullReservation(id)
            refreshStats()
            com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_DELETED)
        }
    }

    suspend fun login(pin: String): com.example.data.models.User? {
        // In real app, hash the PIN and compare. Here we just compare raw for demonstration as seeded with 1234
        return dao.getUserByPin(pin)
    }

    init {
        viewModelScope.launch {
            // Very simple seeding mechanism: check if users exist
            val user = dao.getUserByPin("1234") // the default
            if (user == null) {
                DatabaseSeeder(dao).seedDatabase()
            }
            _isInitializing.value = false
            startObservingStats()
        }
    }

    private var statJobs: List<kotlinx.coroutines.Job> = emptyList()

    fun refreshStats() {
        statJobs.forEach { it.cancel() }
        startObservingStats()
    }

    private fun startObservingStats() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val now = System.currentTimeMillis()

        val job1 = viewModelScope.launch {
            dao.getCheckInCountTodayFlow(todayStart, todayEnd).collect { count ->
                _checkInToday.value = count.toString().padStart(2, '0')
            }
        }
        val job2 = viewModelScope.launch {
            dao.getCheckOutCountTodayFlow(todayStart, todayEnd).collect { count ->
                _checkOutToday.value = count.toString().padStart(2, '0')
            }
        }
        val job3 = viewModelScope.launch {
            dao.getOccupiedUnitsCountFlow(now).collect { count ->
                _occupiedUnits.value = count.toString().padStart(2, '0')
            }
        }
        val job4 = viewModelScope.launch {
            dao.getPendingCountFlow().collect { count ->
                _pendingReservations.value = count.toString().padStart(2, '0')
            }
        }
        statJobs = listOf(job1, job2, job3, job4)
    }
}
