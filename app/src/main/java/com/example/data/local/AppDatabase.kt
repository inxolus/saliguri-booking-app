package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.saliguri.repository.SyncTypeConverters
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
        Payment::class,
        com.example.saliguri.repository.SyncReservation::class,
        com.example.saliguri.repository.SyncRoom::class,
        com.example.saliguri.repository.SyncGuest::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(SyncTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun reportDao(): ReportDao
    abstract fun reservationDao(): com.example.saliguri.repository.ReservationDao
    abstract fun roomDao(): com.example.saliguri.repository.RoomDao
    abstract fun syncGuestDao(): com.example.saliguri.repository.SyncGuestDao

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
