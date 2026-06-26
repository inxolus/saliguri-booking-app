package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.models.Guest
import com.example.data.models.Reservation
import com.example.data.models.ReservationService
import com.example.data.models.ReservationUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()

    private val _reservation = MutableStateFlow<Reservation?>(null)
    val reservation: StateFlow<Reservation?> = _reservation
    
    private val _guest = MutableStateFlow<Guest?>(null)
    val guest: StateFlow<Guest?> = _guest
    
    private val _units = MutableStateFlow<List<ReservationUnit>>(emptyList())
    val units: StateFlow<List<ReservationUnit>> = _units
    
    private val _services = MutableStateFlow<List<ReservationService>>(emptyList())
    val services: StateFlow<List<ReservationService>> = _services

    private val _foods = MutableStateFlow<List<com.example.data.models.ReservationFood>>(emptyList())
    val foods: StateFlow<List<com.example.data.models.ReservationFood>> = _foods

    private val _payments = MutableStateFlow<List<com.example.data.models.Payment>>(emptyList())
    val payments: StateFlow<List<com.example.data.models.Payment>> = _payments
    
    val allUnits: StateFlow<List<com.example.data.models.UnitModel>> = dao.getUnitsFlow().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    val allFoods: StateFlow<List<com.example.data.models.FoodPackage>> = dao.getFoodPackagesFlow().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    val paymentMethods: StateFlow<List<com.example.data.models.PaymentMethod>> = dao.getPaymentMethodsFlow().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    fun getUnitName(code: String): String {
        return allUnits.value.find { it.unitCode == code }?.unitName ?: code
    }

    fun getFoodPackageName(code: String): String {
        return allFoods.value.find { it.code == code }?.packageName ?: code
    }

    fun getPaymentMethodName(code: String): String {
        return paymentMethods.value.find { it.code == code }?.name ?: code
    }
    
    fun loadReservation(id: String) {
        viewModelScope.launch {
            val res = dao.getReservationById(id)
            if (res != null) {
                _reservation.value = res
                _guest.value = dao.getGuestById(res.guestId)
                _units.value = dao.getReservationUnits(id)
                _services.value = dao.getReservationServices(id)
                _foods.value = dao.getReservationFoods(id)
                _payments.value = dao.getReservationPayments(id)
            }
        }
    }

    fun updateReservationStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            dao.updateReservationStatus(id, newStatus)
            loadReservation(id)
            com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_STATUS_CHANGED)
        }
    }

    fun addPayment(reservationId: String, amount: Long, method: String, notes: String) {
        viewModelScope.launch {
            val res = reservation.value
            val isFirstPayment = (res?.paidAmount ?: 0L) == 0L
            val payment = com.example.data.models.Payment(
                id = com.example.utils.IdGenerator.generate("PAY"),
                reservationId = reservationId,
                amount = amount,
                paymentMethodCode = method,
                paymentDate = System.currentTimeMillis(),
                referenceNumber = null,
                notes = notes,
                paymentType = if (isFirstPayment) "DOWN_PAYMENT" else "INSTALLMENT"
            )
            dao.addPaymentToReservation(reservationId, payment)
            loadReservation(reservationId)
        }
    }

    fun deleteReservation(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            dao.deleteFullReservation(id)
            com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_DELETED)
            onDeleted()
        }
    }
}
