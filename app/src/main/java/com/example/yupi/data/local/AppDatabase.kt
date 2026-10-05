package com.example.yupi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WordLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordLogDao(): WordLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "yupi_word_tracker_db"
                        ).build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}