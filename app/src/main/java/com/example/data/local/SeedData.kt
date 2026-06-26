package com.example.data.local

import com.example.data.models.*
import com.example.utils.IdGenerator

object SeedData {
    val businessRules = listOf(
        BusinessRule("BR001", "Extra Bed Price", "100000", "Price per extra bed per night", System.currentTimeMillis()),
        BusinessRule("BR002", "Check-in Time", "14:00", "Standard check-in time", System.currentTimeMillis()),
        BusinessRule("BR003", "Check-out Time", "12:00", "Standard check-out time", System.currentTimeMillis())
    )

    val guestTypes = listOf(
        GuestType("GT001", "Regular", "Regular Guest", 1),
        GuestType("GT002", "VIP", "VIP Guest", 1),
        GuestType("GT003", "Wedding", "Wedding Event Guest", 1),
        GuestType("GT004", "Corporate", "Corporate Guest", 1),
        GuestType("GT005", "Family", "Family Guest", 1)
    )

    val paymentMethods = listOf(
        PaymentMethod("PM001", "Cash", "Cash payment", 1),
        PaymentMethod("PM002", "Bank Transfer (BCA)", "1234567890", 1),
        PaymentMethod("PM003", "Bank Transfer (Mandiri)", "0987654321", 1),
        PaymentMethod("PM004", "Credit Card", "EDC Machine", 1),
        PaymentMethod("PM005", "QRIS", "QRIS Barcode", 1)
    )

    val reservationStatuses = listOf(
        ReservationStatus("RS001", "Pending", 1, 1),
        ReservationStatus("RS002", "Confirmed", 2, 1),
        ReservationStatus("RS003", "Checked In", 3, 1),
        ReservationStatus("RS004", "Checked Out", 4, 1),
        ReservationStatus("RS005", "Cancelled", 5, 1),
        ReservationStatus("RS006", "No Show", 6, 1)
    )

    val roomStatuses = listOf(
        RoomStatus("RM001", "Available", "#00FF00", 1),
        RoomStatus("RM002", "Occupied", "#FF0000", 1),
        RoomStatus("RM003", "Cleaning", "#0000FF", 1),
        RoomStatus("RM004", "Maintenance", "#FFFF00", 1)
    )

    val units = listOf(
        UnitModel("V001", "Villa 1", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V002", "Villa 2", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V003", "Villa 3", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V004", "Villa 4", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V005", "Villa 5", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V006", "Villa 6", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V007", "Villa 7", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V008", "Villa 8", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V009", "Villa 9", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("V010", "Villa 10", "Villa", 500000L, 2, 1, "Standard Villa", "RM001", 1),
        UnitModel("A001", "Aula", "Hall", 2000000L, 50, 0, "Main Hall", "RM001", 1)
    )

    val services = listOf(
        ServiceModel("EV001", "Intimate Wedding", 7500000L, "Package", "Complete wedding setup", 1),
        ServiceModel("SV002", "Extra Bed", 100000L, "Night", "Additional bed with breakfast", 1)
    )

    val foodPackages = listOf(
        FoodPackage("FP001", "Breakfast Package A", "Nasi Goreng + Tea", 25000L, 1),
        FoodPackage("FP002", "Breakfast Package B", "Mie Goreng + Coffee", 25000L, 1),
        FoodPackage("FP003", "Lunch Package A", "Ayam Bakar + Rice", 35000L, 1),
        FoodPackage("FP004", "Lunch Package B", "Ikan Bakar + Rice", 35000L, 1),
        FoodPackage("FP005", "Dinner Package A", "Sate Ayam + Rice", 30000L, 1),
        FoodPackage("FP006", "Dinner Package B", "Soto Ayam + Rice", 30000L, 1),
        FoodPackage("FP007", "Snack Package", "Pisang Goreng + Tea", 15000L, 1),
        FoodPackage("FP008", "BBQ Package", "Mixed BBQ", 100000L, 1)
    )

    val defaultUser = User(
        id = "U001",
        username = "admin", 
        pinHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", // SHA-256 for "1234"
        fullName = "Manager", 
        role = "Admin", 
        isActive = 1, 
        createdAt = System.currentTimeMillis()
    )
}
