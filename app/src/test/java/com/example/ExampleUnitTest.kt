package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  
  // Clean Helper function mimicking our overlap checking rule:
  private fun isOverlap(
      existingIn: Long,
      existingOut: Long,
      newIn: Long,
      newOut: Long
  ): Boolean {
      return newIn < existingOut && newOut > existingIn
  }

  @Test
  fun testBookingOverlaps() {
      val cal = Calendar.getInstance().apply {
          set(Calendar.YEAR, 2026)
          set(Calendar.MONTH, Calendar.JUNE)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
      }
      
      // 1 Juni
      cal.set(Calendar.DAY_OF_MONTH, 1)
      val june1 = cal.timeInMillis
      
      // 2 Juni
      cal.set(Calendar.DAY_OF_MONTH, 2)
      val june2 = cal.timeInMillis
      
      // 3 Juni
      cal.set(Calendar.DAY_OF_MONTH, 3)
      val june3 = cal.timeInMillis
      
      // 4 Juni
      cal.set(Calendar.DAY_OF_MONTH, 4)
      val june4 = cal.timeInMillis

      // Existing booking: Booking A (1 Juni - 2 Juni)
      val existingIn = june1
      val existingOut = june2

      // Booking B: 2 Juni - 3 Juni -> Expected: Bisa (not overlap) -> FALSE
      assertFalse(isOverlap(existingIn, existingOut, june2, june3))

      // Booking C: 1 Juni - 3 Juni -> Expected: Tidak Bisa (overlap) -> TRUE
      assertTrue(isOverlap(existingIn, existingOut, june1, june3))

      // Booking D: 3 Juni - 4 Juni -> Expected: Bisa (not overlap) -> FALSE
      assertFalse(isOverlap(existingIn, existingOut, june3, june4))
  }
}
