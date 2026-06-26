package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("""
        SELECT SUM(r.total_amount) 
        FROM reservations r 
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT COUNT(r.id) 
        FROM reservations r 
        WHERE r.status_code NOT IN ('RS005', 'RS006') AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalBookings(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    // Total Malam -> Total Kamar
    @Query("""
        SELECT COUNT(ru.id) 
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code NOT IN ('RS005', 'RS006') 
        AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code != 'A001'
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalNights(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    // Total Tamu
    @Query("""
        SELECT SUM(u.capacity + ru.extra_bed_count) 
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        INNER JOIN units u ON ru.unit_code = u.unit_code
        WHERE r.status_code NOT IN ('RS005', 'RS006') 
        AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code != 'A001'
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalGuests(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    // Cancellation
    @Query("""
        SELECT COUNT(r.id) 
        FROM reservations r 
        WHERE r.status_code IN ('RS005', 'RS006') AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getCancelledBookings(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    @Query("""
        SELECT COUNT(r.id) 
        FROM reservations r 
        WHERE r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getAllBookingsCount(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    // Revenue Breakdown
    @Query("""
        SELECT SUM(ru.nightly_rate * ru.nights_count) 
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code != 'A001'
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getRoomRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT SUM(ru.extra_bed_count * 100000 * ru.nights_count) 
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code != 'A001'
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getExtraBedRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>
    
    @Query("""
        SELECT SUM(rs.total_price) 
        FROM reservation_services rs
        INNER JOIN reservations r ON rs.reservation_id = r.id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getServicesRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT SUM(rf.total_price) 
        FROM reservation_foods rf
        INNER JOIN reservations r ON rf.reservation_id = r.id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getFoodsRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT SUM(ru.subtotal) 
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code = 'A001'
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getAulaRevenue(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    // Daily Details for Export
    @Query("""
        SELECT ru.check_in_date as date, 
               COALESCE(u.unit_name, ru.unit_code) as unit_code, 
               ru.nightly_rate, ru.extra_bed_count, 100000 as extra_bed_price, 
               (ru.nightly_rate * ru.nights_count + ru.extra_bed_count * 100000 * ru.nights_count) as total,
               gt.name || ' — ' || st.name as description
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        INNER JOIN guest_types gt ON r.guest_type_code = gt.code
        INNER JOIN reservation_statuses st ON r.status_code = st.code
        LEFT JOIN units u ON ru.unit_code = u.unit_code
        WHERE r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
        ORDER BY ru.check_in_date ASC, ru.unit_code ASC
    """)
    fun getDailyDetails(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<List<DailyDetailView>>

    // Occupancy
    @Query("""
        SELECT ru.unit_code, u.unit_name, COUNT(ru.id) as booking_count, SUM(ru.nights_count) as total_nights, 
               SUM(ru.nightly_rate * ru.nights_count) as revenue
        FROM reservation_units ru
        INNER JOIN reservations r ON ru.reservation_id = r.id
        LEFT JOIN units u ON ru.unit_code = u.unit_code
        WHERE r.status_code NOT IN ('RS005', 'RS006') AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND ru.unit_code != 'A001'
        AND (:unitCode = 'ALL' OR ru.unit_code = :unitCode)
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
        GROUP BY ru.unit_code, u.unit_name
    """)
    fun getUnitOccupancy(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<List<UnitOccupancyView>>

    @Query("""
        SELECT gt.name as guest_type, COUNT(r.id) as count, SUM(r.total_amount) as revenue, AVG(ru.nights_count) as avg_nights
        FROM reservations r
        INNER JOIN guest_types gt ON r.guest_type_code = gt.code
        LEFT JOIN reservation_units ru ON r.id = ru.reservation_id
        WHERE r.status_code NOT IN ('RS005', 'RS006') AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
        GROUP BY gt.code, gt.name
    """)
    fun getGuestTypeOccupancy(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<List<GuestTypeOccupancyView>>

    // Payment Report
    @Query("""
        SELECT SUM(total_amount) FROM reservations r
        WHERE r.payment_status = 'PAID' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalPaid(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT SUM(total_amount - paid_amount) FROM reservations r
        WHERE r.payment_status != 'PAID' AND r.payment_status != 'CANCELLED' AND r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalUnpaid(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>
    
    @Query("""
        SELECT SUM(total_amount - paid_amount) FROM reservations r
        WHERE r.paid_amount > 0 AND r.paid_amount < r.total_amount AND r.status_code != 'RS005' AND r.payment_status != 'CANCELLED' AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getTotalDPMissing(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Long?>

    @Query("""
        SELECT COUNT(id) FROM reservations r
        WHERE r.payment_status != 'PAID' AND r.payment_status != 'CANCELLED' AND r.status_code NOT IN ('RS005', 'RS006') AND r.check_in_date <= :today AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getOverdueCount(start: Long, end: Long, today: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    @Query("""
        SELECT p.payment_method_code as method, COUNT(p.id) as count, SUM(p.amount) as amount
        FROM payments p
        INNER JOIN reservations r ON p.reservation_id = r.id
        WHERE r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:unitCode = 'ALL' OR r.id IN (SELECT reservation_id FROM reservation_units WHERE unit_code = :unitCode))
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
        GROUP BY p.payment_method_code
    """)
    fun getPaymentMethodStats(start: Long, end: Long, unitCode: String = "ALL", guestType: String = "ALL", status: String = "ALL"): Flow<List<PaymentMethodStatView>>

    // Aula Report
    @Query("""
        SELECT COUNT(r.id) FROM reservations r
        INNER JOIN reservation_units ru ON r.id = ru.reservation_id
        WHERE ru.unit_code = 'A001' AND r.status_code NOT IN ('RS005', 'RS006')
        AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
    """)
    fun getAulaBookingCount(start: Long, end: Long, guestType: String = "ALL", status: String = "ALL"): Flow<Int?>

    @Query("""
        SELECT gt.name as guest_type, COUNT(r.id) as count
        FROM reservations r
        INNER JOIN reservation_units ru ON r.id = ru.reservation_id
        INNER JOIN guest_types gt ON r.guest_type_code = gt.code
        WHERE ru.unit_code = 'A001' AND r.status_code NOT IN ('RS005', 'RS006')
        AND r.check_in_date >= :start AND r.check_in_date <= :end
        AND (:guestType = 'ALL' OR r.guest_type_code = :guestType)
        AND (:status = 'ALL' OR r.status_code = :status)
        GROUP BY gt.code, gt.name
    """)
    fun getAulaEventStats(start: Long, end: Long, guestType: String = "ALL", status: String = "ALL"): Flow<List<AulaEventStatView>>

    // Guest Analytics
    @Query("""
        SELECT AVG(r.check_in_date - r.created_at) / 86400000 FROM reservations r
        WHERE r.check_in_date >= :start AND r.check_in_date <= :end
    """)
    fun getAverageLeadTimeDays(start: Long, end: Long): Flow<Double?>
    
    @Query("""
        SELECT 
            (SELECT COUNT(DISTINCT guest_id) FROM reservations WHERE guest_id IN (SELECT guest_id FROM reservations GROUP BY guest_id HAVING COUNT(id) > 1)) * 1.0 
            / 
            (SELECT COUNT(DISTINCT guest_id) FROM reservations)
    """)
    fun getRepeatGuestRate(): Flow<Double?>

    @Query("""
        SELECT g.name, SUM(r.total_amount) as total_revenue
        FROM guests g
        INNER JOIN reservations r ON g.id = r.guest_id
        WHERE r.status_code != 'RS005' AND r.check_in_date >= :start AND r.check_in_date <= :end
        GROUP BY g.id, g.name
        ORDER BY total_revenue DESC
        LIMIT 10
    """)
    fun getTopGuests(start: Long, end: Long): Flow<List<TopGuestView>>
}

data class DailyDetailView(
    val date: Long,
    val unit_code: String,
    val nightly_rate: Long,
    val extra_bed_count: Int,
    val extra_bed_price: Long,
    val total: Long,
    val description: String
)

data class UnitOccupancyView(
    val unit_code: String,
    val unit_name: String?,
    val booking_count: Int,
    val total_nights: Int,
    val revenue: Long
)

data class GuestTypeOccupancyView(
    val guest_type: String,
    val count: Int,
    val revenue: Long,
    val avg_nights: Double
)

data class PaymentMethodStatView(
    val method: String,
    val count: Int,
    val amount: Long
)

data class AulaEventStatView(
    val guest_type: String,
    val count: Int
)

data class TopGuestView(
    val name: String,
    val total_revenue: Long
)
