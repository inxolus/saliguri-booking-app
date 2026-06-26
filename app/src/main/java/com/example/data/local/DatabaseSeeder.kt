package com.example.data.local

import android.util.Log
import com.example.data.models.*

class DatabaseSeeder(private val dao: AppDao) {
    suspend fun seedDatabase() {
        Log.d("DatabaseSeeder", "Seeding database...")
        // Business Rules
        val rules = listOf(
            BusinessRule("BR001", "EXTRA_BED_PRICE", "100000", "Price per extra bed", System.currentTimeMillis()),
            BusinessRule("BR002", "EXTRA_BED_INCLUDE_BREAKFAST", "TRUE", "Include breakfast", System.currentTimeMillis()),
            BusinessRule("BR003", "CHECK_IN_TIME", "14:00", "Check in time", System.currentTimeMillis()),
            BusinessRule("BR004", "CHECK_OUT_TIME", "12:00", "Check out time", System.currentTimeMillis()),
            BusinessRule("BR005", "MAX_RESERVATION_YEARS", "10", "Max reservation advance", System.currentTimeMillis()),
        )
        dao.insertBusinessRules(rules)

        // Guest Types
        val guestTypes = listOf(
            GuestType("GT001", "Regular", "Tamu umum"),
            GuestType("GT002", "Corporate", "Instansi atau perusahaan"),
            GuestType("GT003", "Wedding", "Tamu paket wedding"),
            GuestType("GT004", "Family", "Liburan keluarga"),
            GuestType("GT005", "Group", "Rombongan besar")
        )
        dao.insertGuestTypes(guestTypes)

        // Payment Methods
        val paymentMethods = listOf(
            PaymentMethod("PM001", "Cash", null),
            PaymentMethod("PM002", "Transfer BCA", null),
            PaymentMethod("PM003", "Transfer BRI", null),
            PaymentMethod("PM004", "Transfer Mandiri", null),
            PaymentMethod("PM005", "QRIS", null)
        )
        dao.insertPaymentMethods(paymentMethods)

        // Reservation Statuses
        val reservationStatuses = listOf(
            ReservationStatus("RS001", "Pending", 1),
            ReservationStatus("RS002", "Confirmed", 2),
            ReservationStatus("RS003", "Checked In", 3),
            ReservationStatus("RS004", "Checked Out", 4),
            ReservationStatus("RS005", "Cancelled", 5),
            ReservationStatus("RS006", "No Show", 6)
        )
        dao.insertReservationStatuses(reservationStatuses)

        // Room Statuses
        val roomStatuses = listOf(
            RoomStatus("RM001", "Available", "#BAF3DB"),
            RoomStatus("RM002", "Occupied", "#FFDAD6"),
            RoomStatus("RM003", "Maintenance", "#E1E2EC"),
            RoomStatus("RM004", "Blocked", "#191C1E")
        )
        dao.insertRoomStatuses(roomStatuses)

        // Units
        val units = listOf(
            UnitModel("V001", "Room 1", "ROOM", 600000, 2, 3, "Kapasitas normal 2 orang", "RM001"),
            UnitModel("V002", "Room 2", "ROOM", 600000, 2, 3, "Kapasitas normal 2 orang", "RM001"),
            UnitModel("V003", "Room 3", "ROOM", 500000, 2, 6, "Kapasitas normal 2 orang", "RM001"),
            UnitModel("V004", "Room 4", "ROOM", 500000, 2, 6, "Kapasitas normal 2 orang", "RM001"),
            UnitModel("V005", "Room 5", "ROOM", 900000, 4, 3, "Lantai utama & atas", "RM001"),
            UnitModel("V006", "Room 6", "ROOM", 900000, 4, 3, "Lantai utama & atas", "RM001"),
            UnitModel("V007", "Room 7", "ROOM", 600000, 2, 3, "Kapasitas normal 2 orang", "RM001"),
            UnitModel("V008", "Room 8", "ROOM", 1400000, 5, 4, "Lantai utama & atas", "RM001"),
            UnitModel("V009", "Villa 9", "VILLA", 2200000, 6, 9, "Lantai bawah & atas", "RM001"),
            UnitModel("V010", "Villa 10", "VILLA", 3300000, 8, 12, "Lantai bawah & atas", "RM001"),
            UnitModel("A001", "Aula", "HALL", 3000000, 90, 0, "Kapasitas 90 orang", "RM001")
        )
        dao.insertUnits(units)

        // Services
        val services = listOf(
            ServiceModel("EX001", "Extra Bed", 100000, "PER_UNIT", "Include breakfast"),
            ServiceModel("EV001", "Intimate Wedding", 7500000, "CUSTOM", "Paket wedding intimate")
        )
        dao.insertServices(services)

        // Foods
        val foods = listOf(
            FoodPackage("FOOD001", "Paket Prasmanan A", "2 lauk, sayur/gulai, sambal, kerupuk, buah, air mineral", 60000),
            FoodPackage("FOOD002", "Paket Prasmanan B", "1 lauk, sayur/gulai, sambal, kerupuk, buah, air mineral", 45000),
            FoodPackage("FOOD003", "Paket BBQ", "Paket BBQ lengkap", 60000),
            FoodPackage("FOOD004", "Nasi Kotak", "Nasi kotak lengkap", 40000),
            FoodPackage("FOOD005", "Nasi Bungkus", "Nasi bungkus", 25000),
            FoodPackage("FOOD006", "Coffee Break", "Coffee break dan snack", 15000)
        )
        dao.insertFoodPackages(foods)

        // Default Manager User
        val user = User("U001", "manager", "1234", "Manager", "MANAGER", 1, System.currentTimeMillis()) // Using plain 1234 just for demonstration
        dao.insertUser(user)
    }
}
