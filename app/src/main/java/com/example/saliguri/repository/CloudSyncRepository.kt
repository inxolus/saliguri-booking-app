package com.example.saliguri.repository

import android.content.Context
import androidx.room.*
import com.example.data.local.AppDatabase
import com.example.network.RetrofitClient
import com.example.network.SyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ============================================
// ENTITY (Sesuai Google Sheets)
// ============================================

@Entity(tableName = "sheet_reservations")
data class SyncReservation(
    @PrimaryKey val id: String,
    val guestName: String,
    val phone: String,
    val guestType: String,           // regular / corporate / family
    val roomNumbers: List<String>,
    val extraBedCount: Int,          // 0 kalau tidak ada
    val checkIn: Date,
    val checkOut: Date,
    val roomTotal: Double,
    val totalAmount: Double,
    val paid: Double,
    val dpAmount: Double,
    val paymentMethod: String,       // bca_transfer / mandiri_transfer / down_payment / cash_onsite
    val paymentProofUrl: String?,    // null kalau cash
    val bookingSource: String,       // website / android_app
    val createdAt: String,
    val updatedAt: String,
    val isSynced: Boolean = false
)

@Entity(tableName = "sheet_rooms")
data class SyncRoom(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val basePrice: Double,
    val maxGuests: Int,
    val isActive: Boolean,
    val allowExtraBed: Boolean = true  // false untuk A001 (Aula)
)

@Entity(tableName = "sheet_guests")
data class SyncGuest(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val idCardNumber: String,
    val type: String
)

// ============================================
// TYPE CONVERTERS
// ============================================

class SyncTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String> = 
        value?.takeIf { it.isNotBlank() }?.split(",")?.map { it.trim() } ?: emptyList()

    @TypeConverter
    fun fromDate(value: Date?): Long? = value?.time

    @TypeConverter
    fun toDate(value: Long?): Date? = value?.let { Date(it) }
}

// ============================================
// DAO
// ============================================

@Dao
interface ReservationDao {
    @Query("SELECT * FROM sheet_reservations WHERE isSynced = 0")
    suspend fun getUnsynced(): List<SyncReservation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reservation: SyncReservation)

    @Query("UPDATE sheet_reservations SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM sheet_reservations ORDER BY createdAt DESC")
    suspend fun getAll(): List<SyncReservation>

    @Query("SELECT * FROM sheet_reservations WHERE id = :id")
    suspend fun getById(id: String): SyncReservation?
}

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<SyncRoom>)

    @Query("SELECT * FROM sheet_rooms WHERE isActive = 1")
    suspend fun getActive(): List<SyncRoom>

    @Query("SELECT * FROM sheet_rooms WHERE id = :id")
    suspend fun getById(id: String): SyncRoom?
    
    @Query("SELECT * FROM sheet_rooms")
    suspend fun getAll(): List<SyncRoom>
}

@Dao
interface SyncGuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests: List<SyncGuest>)

    @Query("SELECT * FROM sheet_guests")
    suspend fun getAll(): List<SyncGuest>
}

// ============================================
// RESULT CLASS
// ============================================

data class SyncResult(
    val downloaded: Int,
    val uploaded: Int,
    val errors: Int
)

// ============================================
// REPOSITORY
// ============================================

class CloudSyncRepository(private val context: Context) {
    private val api = RetrofitClient.api
    private val db = AppDatabase.getDatabase(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ============================================
    // DOWNLOAD: Cloud → Room
    // ============================================

    suspend fun syncReservationsFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAll(RetrofitClient.API_KEY, "getAll", "Reservations")
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))

            val apiResponse = response.body()
            if (apiResponse?.success != true || apiResponse.data == null) {
                return@withContext Result.failure(Exception(apiResponse?.error ?: "No data"))
            }

            val reservations = apiResponse.data.map { mapToReservation(it) }
            reservations.forEach { db.reservationDao().upsert(it) }

            Result.success(reservations.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncRoomsFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAll(RetrofitClient.API_KEY, "getAll", "Rooms")
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))

            val apiResponse = response.body()
            if (apiResponse?.success != true || apiResponse.data == null) {
                return@withContext Result.failure(Exception(apiResponse?.error ?: "No data"))
            }

            val rooms = apiResponse.data.map { mapToRoom(it) }
            db.roomDao().insertAll(rooms)

            Result.success(rooms.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncGuestsFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAll(RetrofitClient.API_KEY, "getAll", "Guests")
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))

            val apiResponse = response.body()
            if (apiResponse?.success != true || apiResponse.data == null) {
                return@withContext Result.failure(Exception(apiResponse?.error ?: "No data"))
            }

            val guests = apiResponse.data.map { mapToGuest(it) }
            db.syncGuestDao().insertAll(guests)

            Result.success(guests.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // UPLOAD: Room → Cloud
    // ============================================

    suspend fun pushReservationToCloud(reservation: SyncReservation): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = SyncRequest(
                action = "insert",
                sheet = "Reservations",
                data = mapOf(
                    "reservation_id" to reservation.id,
                    "guest_name" to reservation.guestName,
                    "phone" to reservation.phone,
                    "guest_type" to reservation.guestType,
                    "room_numbers" to reservation.roomNumbers.joinToString(","),
                    "extra_bed_count" to reservation.extraBedCount.toString(),
                    "check_in" to formatDate(reservation.checkIn),
                    "check_out" to formatDate(reservation.checkOut),
                    "room_total" to reservation.roomTotal.toString(),
                    "total_amount" to reservation.totalAmount.toString(),
                    "paid" to reservation.paid.toString(),
                    "dp_amount" to reservation.dpAmount.toString(),
                    "payment_method" to reservation.paymentMethod,
                    "payment_proof_url" to (reservation.paymentProofUrl ?: ""),
                    "booking_source" to reservation.bookingSource,
                    "created_at" to reservation.createdAt,
                    "updated_at" to reservation.updatedAt
                )
            )

            val response = api.postData(request)
            if (response.isSuccessful && response.body()?.success == true) {
                db.reservationDao().markAsSynced(reservation.id)
                Result.success(reservation.id)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Push failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // FULL SYNC
    // ============================================

    suspend fun performFullSync(): SyncResult = withContext(Dispatchers.IO) {
        var downloaded = 0
        var uploaded = 0
        var errors = 0

        // Download dari cloud
        syncReservationsFromCloud()
            .onSuccess { downloaded += it }
            .onFailure { errors++ }

        syncRoomsFromCloud()
            .onSuccess { downloaded += it }
            .onFailure { errors++ }

        syncGuestsFromCloud()
            .onSuccess { downloaded += it }
            .onFailure { errors++ }

        // Upload yang belum sync
        val unsynced = db.reservationDao().getUnsynced()
        for (res in unsynced) {
            pushReservationToCloud(res)
                .onSuccess { uploaded++ }
                .onFailure { errors++ }
        }

        SyncResult(downloaded, uploaded, errors)
    }

    // ============================================
    // AVAILABILITY CHECK
    // ============================================

    suspend fun getAvailableRooms(checkIn: String, checkOut: String): Result<List<SyncRoom>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAvailableRooms(
                RetrofitClient.API_KEY,
                "getAvailableRooms",
                checkIn,
                checkOut
            )

            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))

            val apiResponse = response.body()
            if (apiResponse?.success != true || apiResponse.data == null) {
                return@withContext Result.failure(Exception(apiResponse?.error ?: "No data"))
            }

            // Map dari Map<String, String> ke SyncRoom entity
            val rooms = apiResponse.data.map { mapToRoom(it) }
            Result.success(rooms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // PRIVATE MAPPERS
    // ============================================

    private fun mapToReservation(map: Map<String, String>): SyncReservation {
        return SyncReservation(
            id = map["reservation_id"] ?: "",
            guestName = map["guest_name"] ?: "",
            phone = map["phone"] ?: "",
            guestType = map["guest_type"] ?: "regular",
            roomNumbers = map["room_numbers"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            extraBedCount = map["extra_bed_count"]?.toIntOrNull() ?: 0,
            checkIn = parseDate(map["check_in"]),
            checkOut = parseDate(map["check_out"]),
            roomTotal = map["room_total"]?.toDoubleOrNull() ?: 0.0,
            totalAmount = map["total_amount"]?.toDoubleOrNull() ?: 0.0,
            paid = map["paid"]?.toDoubleOrNull() ?: 0.0,
            dpAmount = map["dp_amount"]?.toDoubleOrNull() ?: 0.0,
            paymentMethod = map["payment_method"] ?: "",
            paymentProofUrl = map["payment_proof_url"]?.takeIf { it.isNotBlank() },
            bookingSource = map["booking_source"] ?: "website",
            createdAt = map["created_at"] ?: "",
            updatedAt = map["updated_at"] ?: "",
            isSynced = true
        )
    }

    private fun mapToRoom(map: Map<String, String>): SyncRoom {
        return SyncRoom(
            id = map["room_id"] ?: "",
            name = map["room_name"] ?: "",
            type = map["room_type"] ?: "",
            basePrice = map["base_price"]?.toDoubleOrNull() ?: 0.0,
            maxGuests = map["max_guests"]?.toIntOrNull() ?: 1,
            isActive = map["is_active"]?.equals("TRUE", ignoreCase = true) == true,
            allowExtraBed = map["allow_extra_bed"]?.equals("TRUE", ignoreCase = true) != false
        )
    }

    private fun mapToGuest(map: Map<String, String>): SyncGuest {
        return SyncGuest(
            id = map["guest_id"] ?: "",
            name = map["name"] ?: "",
            phone = map["phone"] ?: "",
            email = map["email"] ?: "",
            idCardNumber = map["id_card_number"] ?: "",
            type = map["type"] ?: "regular"
        )
    }

    private fun parseDate(dateStr: String?): Date {
        return try {
            dateFormat.parse(dateStr ?: "") ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
}
