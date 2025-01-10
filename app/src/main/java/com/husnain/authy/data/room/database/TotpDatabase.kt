package com.husnain.authy.data.room.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted

@Database(entities = [EntityTotp::class, RecentlyDeleted::class], version = 1, exportSchema = false)
abstract class TotpDatabase : RoomDatabase() {
    abstract fun daoTotp(): DaoTotp
    abstract fun daoRecentlyDeleted(): DaoRecentlyDeleted
}
