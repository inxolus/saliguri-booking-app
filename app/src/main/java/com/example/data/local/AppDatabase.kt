package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.*
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusinessRule::class,
        GuestType::class,
        PaymentMethod::class,
        ReservationStatus::class,
        RoomStatus::class,
        UnitModel::class,
        ServiceModel::class,
        FoodPackage::class,
        User::class,
        Guest::class,
        Reservation::class,
        ReservationUnit::class,
        ReservationService::class,
        ReservationFood::class,
        Payment::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saliguri_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val dao = getDatabase(context).appDao()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            dao.insertBusinessRules(SeedData.businessRules)
                            dao.insertGuestTypes(SeedData.guestTypes)
                            dao.insertPaymentMethods(SeedData.paymentMethods)
                            dao.insertReservationStatuses(SeedData.reservationStatuses)
                            dao.insertRoomStatuses(SeedData.roomStatuses)
                            dao.insertUnits(SeedData.units)
                            dao.insertServices(SeedData.services)
                            dao.insertFoodPackages(SeedData.foodPackages)
                            dao.insertUser(SeedData.defaultUser)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
