package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.utils.AppConstants
import com.example.utils.IdGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class SelectedUnitState(
    val unit: UnitModel,
    val checkIn: Long,
    val checkOut: Long,
    val extraBedCount: Int = 0,
    val isWeddingIncluded: Boolean = false
)

data class SelectedServiceState(
    val service: ServiceModel,
    val quantity: Int
)

data class SelectedFoodState(
    val food: FoodPackage,
    val quantity: Int
)

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()

    val guestTypes = dao.getGuestTypesFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val paymentMethods = dao.getPaymentMethodsFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allUnits = dao.getUnitsFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val reservationStatuses = dao.getReservationStatusesFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allServices = dao.getServicesFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allFoodPackages = dao.getFoodPackagesFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private var rulesList: List<BusinessRule> = emptyList()

    var guestName = MutableStateFlow("")
    var guestPhone = MutableStateFlow("")
    var selectedGuestType = MutableStateFlow(AppConstants.GUEST_WEDDING) 

    var selectedUnits = MutableStateFlow<List<SelectedUnitState>>(emptyList())
    var selectedServices = MutableStateFlow<List<SelectedServiceState>>(emptyList())
    var selectedFoods = MutableStateFlow<List<SelectedFoodState>>(emptyList())

    var hasDownPayment = MutableStateFlow(false)
    var isDpPercentage = MutableStateFlow(false)
    var dpInputValue = MutableStateFlow("")

    var selectedStatus = MutableStateFlow(AppConstants.STATUS_PENDING)
    var selectedPaymentMethod = MutableStateFlow("PM001")
    var notes = MutableStateFlow("")

    val totalAmount = MutableStateFlow(0L)
    val checkInGlobal = MutableStateFlow(normalizeToStartOfDay(System.currentTimeMillis()))
    val checkOutGlobal = MutableStateFlow(normalizeToStartOfDay(System.currentTimeMillis()) + 86400000)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting
    val submissionSuccess = MutableSharedFlow<Boolean>()
    val errorMessage = MutableSharedFlow<String>()

    private var editingReservationId: String? = null

    var extraBedPrice = 100000L

    fun loadExistingReservation(id: String) {
        editingReservationId = id
        viewModelScope.launch {
            val res = dao.getReservationById(id)
            if (res != null) {
                notes.value = res.notes ?: ""
                selectedStatus.value = res.statusCode
                selectedPaymentMethod.value = res.paymentMethodCode ?: ""
                checkInGlobal.value = res.checkInDate
                checkOutGlobal.value = res.checkOutDate
                
                val g = dao.getGuestById(res.guestId)
                if (g != null) {
                    guestName.value = g.name
                    guestPhone.value = g.phone
                    selectedGuestType.value = g.guestTypeCode
                }
                
                if (res.downPayment > 0) {
                    hasDownPayment.value = true
                    isDpPercentage.value = false
                    dpInputValue.value = res.downPayment.toString()
                }

                val units = dao.getReservationUnits(id)
                val services = dao.getReservationServices(id)
                val foods = dao.getReservationFoods(id)

                val availableUnits = dao.getUnitsFlow().first()
                val availableServices = dao.getServicesFlow().first()
                val availableFoods = dao.getFoodPackagesFlow().first()

                selectedUnits.value = units.mapNotNull { ru ->
                    availableUnits.find { it.unitCode == ru.unitCode }?.let { u ->
                        SelectedUnitState(
                            unit = u,
                            checkIn = ru.checkInDate,
                            checkOut = ru.checkOutDate,
                            extraBedCount = ru.extraBedCount,
                            isWeddingIncluded = ru.nightlyRate == 0L
                        )
                    }
                }

                selectedServices.value = services.mapNotNull { rs ->
                    availableServices.find { it.code == rs.serviceCode }?.let { s ->
                        SelectedServiceState(s, rs.quantity)
                    }
                }

                selectedFoods.value = foods.mapNotNull { rf ->
                    availableFoods.find { it.code == rf.foodPackageCode }?.let { f ->
                        SelectedFoodState(f, rf.numberOfPeople)
                    }
                }

                recalculateTotal()
            }
        }
    }

    fun prefill(unitCode: String, dateString: String) {
        viewModelScope.launch {
            val dateMillis = dateString.toLongOrNull() ?: return@launch
            val normalizedDate = normalizeToStartOfDay(dateMillis)
            val availableUnits = dao.getUnitsFlow().first()
            val unit = availableUnits.find { it.unitCode == unitCode }
            if (unit != null) {
                checkInGlobal.value = normalizedDate
                checkOutGlobal.value = normalizedDate + 86400000L
                selectedUnits.value = listOf(SelectedUnitState(unit, normalizedDate, normalizedDate + 86400000L))
                recalculateTotal()
            }
        }
    }

    init {
        viewModelScope.launch {
            dao.getBusinessRulesFlow().collect { rules ->
                rulesList = rules
                val ebRule = rules.find { it.ruleCode == "BR001" }?.value?.toLongOrNull()
                if (ebRule != null) extraBedPrice = ebRule
            }
        }
    }

    fun onGuestTypeChanged(newType: String) {
        val oldType = selectedGuestType.value
        selectedGuestType.value = newType
        
        if (newType == AppConstants.GUEST_WEDDING && oldType != AppConstants.GUEST_WEDDING) {
            // Apply Wedding Logic
            val weddingUnits = allUnits.value.filter { AppConstants.WEDDING_AUTO_UNITS.contains(it.unitCode) }
            val addedUnits = weddingUnits.map { 
                SelectedUnitState(it, normalizeToStartOfDay(checkInGlobal.value), normalizeToStartOfDay(checkOutGlobal.value), 0, true)
            }
            selectedUnits.value = (selectedUnits.value.filter { !AppConstants.WEDDING_AUTO_UNITS.contains(it.unit.unitCode) } + addedUnits)

            val intimateWeddingService = allServices.value.find { it.code == AppConstants.WEDDING_AUTO_SERVICE }
            if (intimateWeddingService != null) {
                if (selectedServices.value.none { it.service.code == AppConstants.WEDDING_AUTO_SERVICE }) {
                    selectedServices.value = selectedServices.value + SelectedServiceState(intimateWeddingService, 1)
                }
            }
        } else if (newType != AppConstants.GUEST_WEDDING && oldType == AppConstants.GUEST_WEDDING) {
            // Remove Wedding Logic
            selectedUnits.value = selectedUnits.value.filter { !it.isWeddingIncluded }
            selectedServices.value = selectedServices.value.filter { it.service.code != AppConstants.WEDDING_AUTO_SERVICE }
        }
        recalculateTotal()
    }

    fun addUnit(unit: UnitModel) {
        if (selectedUnits.value.none { it.unit.unitCode == unit.unitCode }) {
            selectedUnits.value = selectedUnits.value + SelectedUnitState(unit, normalizeToStartOfDay(checkInGlobal.value), normalizeToStartOfDay(checkOutGlobal.value))
            recalculateTotal()
        }
    }

    fun removeUnit(unitCode: String) {
        selectedUnits.value = selectedUnits.value.filter { it.unit.unitCode != unitCode }
        recalculateTotal()
    }

    fun updateUnitDates(unitCode: String, checkIn: Long, checkOut: Long) {
        selectedUnits.value = selectedUnits.value.map {
            if (it.unit.unitCode == unitCode) it.copy(checkIn = normalizeToStartOfDay(checkIn), checkOut = normalizeToStartOfDay(checkOut)) else it
        }
        recalculateTotal()
    }

    fun updateUnitExtraBed(unitCode: String, count: Int) {
        selectedUnits.value = selectedUnits.value.map {
            if (it.unit.unitCode == unitCode) it.copy(extraBedCount = count) else it
        }
        recalculateTotal()
    }

    fun addService(service: ServiceModel) {
        if (selectedServices.value.none { it.service.code == service.code }) {
            selectedServices.value = selectedServices.value + SelectedServiceState(service, 1)
            recalculateTotal()
        }
    }

    fun removeService(serviceCode: String) {
        selectedServices.value = selectedServices.value.filter { it.service.code != serviceCode }
        recalculateTotal()
    }

    fun updateServiceQty(serviceCode: String, qty: Int) {
        selectedServices.value = selectedServices.value.map {
            if (it.service.code == serviceCode) it.copy(quantity = qty) else it
        }
        recalculateTotal()
    }

    fun addFood(food: FoodPackage) {
        if (selectedFoods.value.none { it.food.code == food.code }) {
            selectedFoods.value = selectedFoods.value + SelectedFoodState(food, 1)
            recalculateTotal()
        }
    }

    fun removeFood(foodCode: String) {
        selectedFoods.value = selectedFoods.value.filter { it.food.code != foodCode }
        recalculateTotal()
    }

    fun updateFoodQty(foodCode: String, qty: Int) {
        selectedFoods.value = selectedFoods.value.map {
            if (it.food.code == foodCode) it.copy(quantity = qty) else it
        }
        recalculateTotal()
    }

    fun recalculateTotal() {
        var total = 0L
        if (selectedUnits.value.isNotEmpty()) {
            checkInGlobal.value = selectedUnits.value.minOf { it.checkIn }
            checkOutGlobal.value = selectedUnits.value.maxOf { it.checkOut }
        }
        selectedUnits.value.forEach { state ->
            val calIn = java.util.Calendar.getInstance().apply { timeInMillis = state.checkIn; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
            val calOut = java.util.Calendar.getInstance().apply { timeInMillis = state.checkOut; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
            val nights = maxOf(1L, (calOut.timeInMillis - calIn.timeInMillis) / 86400000L)
            val unitPrice = if (state.isWeddingIncluded) 0L else state.unit.price
            total += (unitPrice * nights)
            total += (state.extraBedCount * extraBedPrice * nights)
        }
        selectedServices.value.forEach { state ->
            total += (state.service.price * state.quantity)
        }
        selectedFoods.value.forEach { state ->
            total += (state.food.pricePerPerson * state.quantity)
        }
        totalAmount.value = total
    }

    fun getCalculatedDp(): Long {
        if (!hasDownPayment.value) return 0L
        val input = dpInputValue.value.toLongOrNull() ?: 0L
        return if (isDpPercentage.value) {
            val percentage = input.coerceIn(0, 100)
            (totalAmount.value * percentage) / 100
        } else {
            input.coerceAtMost(totalAmount.value)
        }
    }

    fun submit() {
        viewModelScope.launch {
            if (guestName.value.isBlank()) {
                errorMessage.emit("Guest name is required")
                return@launch
            }
            if (selectedUnits.value.isEmpty()) {
                errorMessage.emit("Please select at least one unit")
                return@launch
            }

            val invalidDates = selectedUnits.value.any { it.checkOut <= it.checkIn }
            if (invalidDates || checkOutGlobal.value <= checkInGlobal.value) {
                errorMessage.emit("Check-out date must be after check-in date")
                return@launch
            }

            _isSubmitting.value = true
            try {
                val isEdit = editingReservationId != null
                // Check unit availability
                val start = selectedUnits.value.minOf { it.checkIn }
                val end = selectedUnits.value.maxOf { it.checkOut }
                var overlaps = dao.getOverlappingUnits(start, end)
                if (isEdit) {
                    overlaps = overlaps.filter { it.reservationId != editingReservationId }
                }
                
                val conflictingUnit = selectedUnits.value.find { reqUnit ->
                    overlaps.any { it.unitCode == reqUnit.unit.unitCode && 
                                  reqUnit.checkIn < it.checkOutDate && 
                                  reqUnit.checkOut > it.checkInDate }
                }
                
                if (conflictingUnit != null) {
                    errorMessage.emit("Unit ${conflictingUnit.unit.unitName} is not available for selected dates.")
                    _isSubmitting.value = false
                    return@launch
                }

                val res = if (isEdit) dao.getReservationById(editingReservationId!!) else null
                
                val guestId = res?.guestId ?: IdGenerator.generate("G")
                val resId = editingReservationId ?: IdGenerator.generate("R")
                
                val guest = Guest(
                    id = guestId,
                    name = guestName.value,
                    phone = guestPhone.value,
                    email = null,
                    guestTypeCode = selectedGuestType.value,
                    idNumber = null,
                    address = null,
                    notes = notes.value,
                    createdAt = res?.createdAt ?: System.currentTimeMillis()
                )
                if (isEdit) dao.updateGuest(guest) else dao.insertGuest(guest)
                
                val dpAmount = getCalculatedDp()
                val paymentStatus = if (dpAmount >= totalAmount.value) "PAID" else if (dpAmount > 0) "DP" else "PENDING"
                
                val resUnits = selectedUnits.value.map { state ->
                    val normalizedIn = normalizeToStartOfDay(state.checkIn)
                    val normalizedOut = normalizeToStartOfDay(state.checkOut)
                    val nights = maxOf(1, ((normalizedOut - normalizedIn) / 86400000L).toInt())
                    val unitPrice = if (state.isWeddingIncluded) 0L else state.unit.price
                    val ebTotal = state.extraBedCount * extraBedPrice
                    val subtotal = (unitPrice + ebTotal) * nights
                    
                    ReservationUnit(
                        id = IdGenerator.generate("RU"),
                        reservationId = resId,
                        unitCode = state.unit.unitCode,
                        checkInDate = normalizedIn,
                        checkOutDate = normalizedOut,
                        nightlyRate = unitPrice,
                        nightsCount = nights,
                        extraBedCount = state.extraBedCount,
                        extraBedPrice = extraBedPrice,
                        subtotal = subtotal
                    )
                }

                val resServices = selectedServices.value.map { state ->
                    ReservationService(
                        id = IdGenerator.generate("RSV"),
                        reservationId = resId,
                        serviceCode = state.service.code,
                        quantity = state.quantity,
                        unitPrice = state.service.price,
                        totalPrice = state.service.price * state.quantity
                    )
                }

                val resFoods = selectedFoods.value.map { state ->
                    ReservationFood(
                        id = IdGenerator.generate("RF"),
                        reservationId = resId,
                        foodPackageCode = state.food.code,
                        numberOfPeople = state.quantity,
                        pricePerPerson = state.food.pricePerPerson,
                        totalPrice = state.food.pricePerPerson * state.quantity
                    )
                }

                val reservation = Reservation(
                    id = resId,
                    guestId = guestId,
                    guestTypeCode = selectedGuestType.value,
                    statusCode = selectedStatus.value,
                    checkInDate = normalizeToStartOfDay(checkInGlobal.value),
                    checkOutDate = normalizeToStartOfDay(checkOutGlobal.value),
                    totalAmount = totalAmount.value,
                    downPayment = dpAmount,
                    paidAmount = dpAmount,
                    paymentStatus = paymentStatus,
                    paymentMethodCode = selectedPaymentMethod.value,
                    notes = notes.value,
                    createdBy = "U001",
                    createdAt = res?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    actualCheckIn = null,
                    actualCheckOut = null
                )

                val existingPayments = if (isEdit) dao.getReservationPayments(resId) else emptyList()
                val dpPayment = existingPayments.find { it.paymentType == "DOWN_PAYMENT" }

                val payment = if (dpAmount > 0) {
                    if (dpPayment != null) {
                        dpPayment.copy(
                            amount = dpAmount,
                            paymentMethodCode = selectedPaymentMethod.value
                        )
                    } else {
                        Payment(
                            id = IdGenerator.generate("PAY"),
                            reservationId = resId,
                            amount = dpAmount,
                            paymentMethodCode = selectedPaymentMethod.value,
                            paymentDate = System.currentTimeMillis(),
                            referenceNumber = null,
                            notes = "Initial Down Payment",
                            paymentType = "DOWN_PAYMENT"
                        )
                    }
                } else null

                // Transaction save
                dao.saveFullReservation(reservation, resUnits, resServices, resFoods, payment)

                if (isEdit && dpAmount == 0L && dpPayment != null) {
                    dao.deletePayment(dpPayment.id)
                }

                dao.updateReservationPaidAmount(resId)

                com.example.utils.EventBus.emit(com.example.utils.AppEvent.RESERVATION_CREATED)

                _isSubmitting.value = false
                submissionSuccess.emit(true)
            } catch (e: Exception) {
                _isSubmitting.value = false
                errorMessage.emit(e.message ?: "Unknown error occurred")
            }
        }
    }
}

fun normalizeToStartOfDay(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
