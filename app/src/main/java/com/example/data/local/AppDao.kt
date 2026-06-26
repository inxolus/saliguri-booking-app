package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Master Tables ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessRules(rules: List<BusinessRule>)

    @Query("SELECT * FROM business_rules")
    fun getBusinessRulesFlow(): Flow<List<BusinessRule>>
    
    @Query("SELECT * FROM business_rules WHERE rule_code = :code LIMIT 1")
    suspend fun getBusinessRule(code: String): BusinessRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuestTypes(types: List<GuestType>)

    @Query("SELECT * FROM guest_types WHERE is_active = 1")
    fun getGuestTypesFlow(): Flow<List<GuestType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(methods: List<PaymentMethod>)

    @Query("SELECT * FROM payment_methods WHERE is_active = 1")
    fun getPaymentMethodsFlow(): Flow<List<PaymentMethod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationStatuses(statuses: List<ReservationStatus>)

    @Query("SELECT * FROM reservation_statuses ORDER BY display_order")
    fun getReservationStatusesFlow(): Flow<List<ReservationStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomStatuses(statuses: List<RoomStatus>)

    @Query("SELECT * FROM room_statuses")
    fun getRoomStatusesFlow(): Flow<List<RoomStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitModel>)

    @Query("UPDATE units SET status_code = :statusCode WHERE unit_code = :unitCode")
    suspend fun updateUnitStatus(unitCode: String, statusCode: String)

    @Query("SELECT * FROM units WHERE is_active = 1")
    fun getUnitsFlow(): Flow<List<UnitModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceModel>)

    @Query("SELECT * FROM services WHERE is_active = 1")
    fun getServicesFlow(): Flow<List<ServiceModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodPackages(packages: List<FoodPackage>)

    @Query("SELECT * FROM food_packages WHERE is_active = 1")
    fun getFoodPackagesFlow(): Flow<List<FoodPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE pin_hash = :pinHash LIMIT 1")
    suspend fun getUserByPin(pinHash: String): User?

    // --- Transactions ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuest(guest: Guest)

    @Update
    suspend fun updateGuest(guest: Guest)

    @Query("SELECT * FROM guests WHERE phone = :phone LIMIT 1")
    suspend fun getGuestByPhone(phone: String): Guest?
    
    @Query("SELECT * FROM guests WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    suspend fun searchGuests(query: String): List<Guest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: Reservation)
    
    @Update
    suspend fun updateReservation(reservation: Reservation)
    
    @Query("DELETE FROM reservation_units WHERE reservation_id = :reservationId")
    suspend fun deleteReservationUnits(reservationId: String)
    @Query("DELETE FROM reservation_services WHERE reservation_id = :reservationId")
    suspend fun deleteReservationServices(reservationId: String)
    @Query("DELETE FROM reservation_foods WHERE reservation_id = :reservationId")
    suspend fun deleteReservationFoods(reservationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationUnits(units: List<ReservationUnit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationServices(services: List<ReservationService>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservationFoods(foods: List<ReservationFood>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Transaction
    suspend fun addPaymentToReservation(reservationId: String, payment: Payment) {
        insertPayment(payment)
        // Optionally update the total paid amount on the reservation if we track it there.
        // Actually, the prompt says "downPayment" is used in UI. Let's update it to sum of payments.
        updateReservationPaidAmount(reservationId)
    }

    @Query("""
        UPDATE reservations 
        SET paid_amount = (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE reservation_id = :reservationId),
            payment_status = CASE 
                WHEN (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE reservation_id = :reservationId) >= total_amount THEN 'PAID'
                WHEN (SELECT COUNT(*) FROM payments WHERE reservation_id = :reservationId AND payment_type = 'DOWN_PAYMENT') > 0 
                     AND (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE reservation_id = :reservationId AND payment_type = 'INSTALLMENT') = 0 THEN 'DP'
                WHEN (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE reservation_id = :reservationId) > 0 THEN 'PARTIAL'
                ELSE 'PENDING'
            END
        WHERE id = :reservationId
    """)
    suspend fun updateReservationPaidAmount(reservationId: String)

    @Transaction
    suspend fun saveFullReservation(
        reservation: Reservation,
        units: List<ReservationUnit>,
        services: List<ReservationService>,
        foods: List<ReservationFood>,
        payment: Payment? = null
    ) {
        insertReservation(reservation)
        deleteReservationUnits(reservation.id)
        deleteReservationServices(reservation.id)
        deleteReservationFoods(reservation.id)
        insertReservationUnits(units)
        if(services.isNotEmpty()) insertReservationServices(services)
        if(foods.isNotEmpty()) insertReservationFoods(foods)
        if(payment != null) insertPayment(payment)
    }

    // Join Queries & Aggregations
    @Query("""
        SELECT r.*, g.name as guest_name, g.phone as guest_phone 
        FROM reservations r 
        INNER JOIN guests g ON r.guest_id = g.id 
        ORDER BY r.check_in_date DESC LIMIT 100
    """)
    fun getRecentReservationsFlow(): Flow<List<ReservationWithGuestName>>

    @Query("""
        SELECT COUNT(*) FROM reservations 
        WHERE check_in_date >= :startOfDay AND check_in_date <= :endOfDay AND status_code IN ('RS002', 'RS003')
    """)
    fun getCheckInCountTodayFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM reservations 
        WHERE check_out_date >= :startOfDay AND check_out_date <= :endOfDay AND status_code IN ('RS003', 'RS004')
    """)
    fun getCheckOutCountTodayFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT unit_code) FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code = 'RS003' AND ru.check_in_date <= :now AND ru.check_out_date >= :now
    """)
    fun getOccupiedUnitsCountFlow(now: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM reservations WHERE status_code = 'RS001'
    """)
    fun getPendingCountFlow(): Flow<Int>
    
    @Query("""
        SELECT ru.* FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code NOT IN ('RS005','RS006') 
        AND ru.check_out_date > :startDate AND ru.check_in_date < :endDate
    """)
    suspend fun getOverlappingUnits(startDate: Long, endDate: Long): List<ReservationUnit>

    @Query("""
        SELECT 
            ru.unit_code,
            ru.check_in_date,
            ru.check_out_date,
            r.status_code,
            r.id as reservation_id,
            g.name as guest_name
        FROM reservation_units ru
        JOIN reservations r ON ru.reservation_id = r.id
        LEFT JOIN guests g ON r.guest_id = g.id
        WHERE r.status_code IN ('RS002', 'RS003', 'RS004') 
        AND (
            (ru.check_in_date <= :endDate AND ru.check_out_date >= :startDate)
        )
    """)
    fun getReservationsForCalendar(startDate: Long, endDate: Long): Flow<List<CalendarReservation>>

    @Query("""
        SELECT SUM(total_amount) FROM reservations WHERE status_code != 'RS005' AND check_in_date >= :start AND check_in_date <= :end
    """)
    fun getRevenueTotalFlow(start: Long, end: Long): Flow<Long?>

    @Query("""
        SELECT guest_type_code, COUNT(*) as count FROM reservations 
        WHERE check_in_date >= :start AND check_in_date <= :end 
        GROUP BY guest_type_code
    """)
    fun getGuestTypeDistributionFlow(start: Long, end: Long): Flow<List<GuestTypeDistribution>>

    @Query("""
        UPDATE reservations 
        SET status_code = :newStatus, 
            updated_at = :now,
            actual_check_in = CASE WHEN :newStatus = 'RS003' THEN :now ELSE actual_check_in END,
            actual_check_out = CASE WHEN :newStatus = 'RS004' THEN :now ELSE actual_check_out END
        WHERE id = :id
    """)
    suspend fun updateReservationStatus(id: String, newStatus: String, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteFullReservation(id: String) {
        deleteReservationUnits(id)
        deleteReservationServices(id)
        deleteReservationFoods(id)
        deleteReservationPayments(id)
        deleteReservation(id)
    }

    @Query("DELETE FROM payments WHERE reservation_id = :reservationId")
    suspend fun deleteReservationPayments(reservationId: String)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)

    @Query("DELETE FROM reservations WHERE id = :id")
    suspend fun deleteReservation(id: String)
    
    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    suspend fun getReservationById(id: String): Reservation?
    
    @Query("SELECT * FROM guests WHERE id = :guestId LIMIT 1")
    suspend fun getGuestById(guestId: String): Guest?
    
    @Query("SELECT * FROM reservation_units WHERE reservation_id = :reservationId")
    suspend fun getReservationUnits(reservationId: String): List<ReservationUnit>

    @Query("SELECT * FROM reservation_services WHERE reservation_id = :reservationId")
    suspend fun getReservationServices(reservationId: String): List<ReservationService>

    @Query("SELECT * FROM reservation_foods WHERE reservation_id = :reservationId")
    suspend fun getReservationFoods(reservationId: String): List<ReservationFood>

    @Query("SELECT * FROM payments WHERE reservation_id = :reservationId ORDER BY payment_date DESC")
    suspend fun getReservationPayments(reservationId: String): List<Payment>
}

data class GuestTypeDistribution(
    val guest_type_code: String,
    val count: Int
)
