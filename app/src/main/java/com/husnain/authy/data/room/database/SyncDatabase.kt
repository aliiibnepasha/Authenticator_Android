package com.husnain.authy.data.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted

@Database(entities = [EntityTotp::class, RecentlyDeleted::class], version = 1, exportSchema = false)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun daoTotp(): DaoTotp
    abstract fun daoRecentlyDeleted(): DaoRecentlyDeleted

    companion object {
        @Volatile
        private var INSTANCE: SyncDatabase? = null

        fun getDatabase(context: Context): SyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyncDatabase::class.java,
                    "totp_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
