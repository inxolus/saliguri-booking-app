package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.ForeignKey
import java.util.Date

@Entity(tableName = "business_rules")
data class BusinessRule(
    @PrimaryKey
    @ColumnInfo(name = "rule_code") val ruleCode: String,
    @ColumnInfo(name = "rule_name") val ruleName: String,
    val value: String,
    val description: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "guest_types")
data class GuestType(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    val name: String,
    @ColumnInfo(name = "account_info") val accountInfo: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "reservation_statuses")
data class ReservationStatus(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    val name: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "room_statuses")
data class RoomStatus(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    val name: String,
    @ColumnInfo(name = "color_code") val colorCode: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "units")
data class UnitModel(
    @PrimaryKey
    @ColumnInfo(name = "unit_code") val unitCode: String,
    @ColumnInfo(name = "unit_name") val unitName: String,
    @ColumnInfo(name = "unit_type") val unitType: String?,
    val price: Long,
    val capacity: Int,
    @ColumnInfo(name = "max_extra_bed") val maxExtraBed: Int,
    val description: String?,
    @ColumnInfo(name = "status_code") val statusCode: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "services")
data class ServiceModel(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "service_name") val serviceName: String,
    val price: Long,
    val unit: String?,
    val description: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(tableName = "food_packages")
data class FoodPackage(
    @PrimaryKey
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    val description: String?,
    @ColumnInfo(name = "price_per_person") val pricePerPerson: Long,
    @ColumnInfo(name = "is_active") val isActive: Int = 1
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey
    val id: String,
    val username: String,
    @ColumnInfo(name = "pin_hash") val pinHash: String,
    @ColumnInfo(name = "full_name") val fullName: String?,
    val role: String?,
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "guests",
    indices = [Index(value = ["phone"])]
)
data class Guest(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    @ColumnInfo(name = "guest_type_code") val guestTypeCode: String,
    @ColumnInfo(name = "id_number") val idNumber: String?,
    val address: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "reservations",
    indices = [
        Index(value = ["guest_id"]),
        Index(value = ["check_in_date"]),
        Index(value = ["check_out_date"]),
        Index(value = ["status_code"])
    ],
    foreignKeys = [
        ForeignKey(entity = Guest::class, parentColumns = ["id"], childColumns = ["guest_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Reservation(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "guest_id") val guestId: String,
    @ColumnInfo(name = "guest_type_code") val guestTypeCode: String,
    @ColumnInfo(name = "status_code") val statusCode: String,
    @ColumnInfo(name = "check_in_date") val checkInDate: Long,
    @ColumnInfo(name = "check_out_date") val checkOutDate: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "down_payment") val downPayment: Long = 0,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "PENDING",
    @ColumnInfo(name = "payment_method_code") val paymentMethodCode: String?,
    val notes: String?,
    @ColumnInfo(name = "created_by") val createdBy: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "actual_check_in") val actualCheckIn: Long?,
    @ColumnInfo(name = "actual_check_out") val actualCheckOut: Long?
)

@Entity(
    tableName = "reservation_units",
    indices = [
        Index(value = ["reservation_id"]),
        Index(value = ["unit_code"])
    ],
    foreignKeys = [
        ForeignKey(entity = Reservation::class, parentColumns = ["id"], childColumns = ["reservation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UnitModel::class, parentColumns = ["unit_code"], childColumns = ["unit_code"])
    ]
)
data class ReservationUnit(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "reservation_id") val reservationId: String,
    @ColumnInfo(name = "unit_code") val unitCode: String,
    @ColumnInfo(name = "check_in_date") val checkInDate: Long,
    @ColumnInfo(name = "check_out_date") val checkOutDate: Long,
    @ColumnInfo(name = "nightly_rate") val nightlyRate: Long,
    @ColumnInfo(name = "nights_count") val nightsCount: Int,
    @ColumnInfo(name = "extra_bed_count") val extraBedCount: Int,
    @ColumnInfo(name = "extra_bed_price") val extraBedPrice: Long,
    val subtotal: Long
)

@Entity(
    tableName = "reservation_services",
    indices = [Index(value = ["reservation_id"])],
    foreignKeys = [
        ForeignKey(entity = Reservation::class, parentColumns = ["id"], childColumns = ["reservation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ServiceModel::class, parentColumns = ["code"], childColumns = ["service_code"])
    ]
)
data class ReservationService(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "reservation_id") val reservationId: String,
    @ColumnInfo(name = "service_code") val serviceCode: String,
    val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "total_price") val totalPrice: Long
)

@Entity(
    tableName = "reservation_foods",
    indices = [Index(value = ["reservation_id"])],
    foreignKeys = [
        ForeignKey(entity = Reservation::class, parentColumns = ["id"], childColumns = ["reservation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodPackage::class, parentColumns = ["code"], childColumns = ["food_package_code"])
    ]
)
data class ReservationFood(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "reservation_id") val reservationId: String,
    @ColumnInfo(name = "food_package_code") val foodPackageCode: String,
    @ColumnInfo(name = "number_of_people") val numberOfPeople: Int,
    @ColumnInfo(name = "price_per_person") val pricePerPerson: Long,
    @ColumnInfo(name = "total_price") val totalPrice: Long
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["reservation_id"])],
    foreignKeys = [
        ForeignKey(entity = Reservation::class, parentColumns = ["id"], childColumns = ["reservation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PaymentMethod::class, parentColumns = ["code"], childColumns = ["payment_method_code"])
    ]
)
data class Payment(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "reservation_id") val reservationId: String,
    val amount: Long,
    @ColumnInfo(name = "payment_method_code") val paymentMethodCode: String,
    @ColumnInfo(name = "payment_date") val paymentDate: Long,
    @ColumnInfo(name = "reference_number") val referenceNumber: String?,
    val notes: String?,
    @ColumnInfo(name = "payment_type") val paymentType: String = "INSTALLMENT"
)

data class CalendarReservation(
    val unit_code: String,
    val check_in_date: Long,
    val check_out_date: Long,
    val status_code: String,
    val reservation_id: String,
    val guest_name: String
)

data class ReservationWithGuestName(
    val id: String,
    @ColumnInfo(name = "guest_id") val guestId: String,
    @ColumnInfo(name = "guest_type_code") val guestTypeCode: String,
    @ColumnInfo(name = "status_code") val statusCode: String,
    @ColumnInfo(name = "check_in_date") val checkInDate: Long,
    @ColumnInfo(name = "check_out_date") val checkOutDate: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "down_payment") val downPayment: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "payment_status") val paymentStatus: String,
    val guest_name: String,
    val guest_phone: String
)
