package com.example.utils

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

object AppConstants {
    const val DB_NAME = "saliguri_db"

    // Status Codes
    const val STATUS_PENDING = "RS001"
    const val STATUS_CONFIRMED = "RS002"
    const val STATUS_CHECKED_IN = "RS003"
    const val STATUS_CHECKED_OUT = "RS004"
    const val STATUS_CANCELLED = "RS005"
    const val STATUS_NO_SHOW = "RS006"

    // Guest Types
    const val GUEST_WEDDING = "GT003"
    
    // Auto-Units for Wedding
    val WEDDING_AUTO_UNITS = listOf("A001", "V007", "V008")
    const val WEDDING_AUTO_SERVICE = "EV001"
}

object StatusConfig {
    fun getBackgroundColor(statusCode: String): Color {
        return when (statusCode) {
            AppConstants.STATUS_PENDING -> ErrorContainer
            AppConstants.STATUS_CONFIRMED -> TertiaryContainer
            AppConstants.STATUS_CHECKED_IN -> SecondaryContainer
            AppConstants.STATUS_CHECKED_OUT, AppConstants.STATUS_CANCELLED, AppConstants.STATUS_NO_SHOW -> OutlineVariant
            else -> OutlineVariant
        }
    }

    fun getTextColor(statusCode: String): Color {
        return when (statusCode) {
            AppConstants.STATUS_PENDING -> OnErrorContainer
            AppConstants.STATUS_CONFIRMED -> OnTertiaryContainer
            AppConstants.STATUS_CHECKED_IN -> OnSecondaryContainer
            AppConstants.STATUS_CHECKED_OUT, AppConstants.STATUS_CANCELLED, AppConstants.STATUS_NO_SHOW -> TextMuted
            else -> TextMuted
        }
    }

    fun getDisplayName(statusCode: String): String {
        return when (statusCode) {
            AppConstants.STATUS_PENDING -> "Pending"
            AppConstants.STATUS_CONFIRMED -> "Confirmed"
            AppConstants.STATUS_CHECKED_IN -> "Checked In"
            AppConstants.STATUS_CHECKED_OUT -> "Checked Out"
            AppConstants.STATUS_CANCELLED -> "Cancelled"
            AppConstants.STATUS_NO_SHOW -> "No Show"
            else -> "Unknown"
        }
    }
}
