package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Formatters {
    private val localeId = Locale("id", "ID")
    
    fun currency(amount: Long): String {
        return NumberFormat.getCurrencyInstance(localeId).apply {
            maximumFractionDigits = 0
        }.format(amount)
    }

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun dbDate(date: Date): String = dbDateFormat.format(date)
    fun displayDateOnly(date: Date): String = displayDateFormat.format(date)
    fun displayDateTime(date: Date): String = displayDateTimeFormat.format(date)
    
    fun displayDateOnly(timestamp: Long): String = displayDateFormat.format(Date(timestamp))
    fun displayDateTime(timestamp: Long): String = displayDateTimeFormat.format(Date(timestamp))
    
    fun parseDbDate(dateString: String): Date? = try { dbDateFormat.parse(dateString) } catch (e: Exception) { null }
}

object IdGenerator {
    fun generate(prefix: String): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..99999).random()
        return "${prefix}_${timestamp}_$random"
    }
}
