package com.husnain.authy.data.room
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntityTotp::class], version = 1, exportSchema = false)
abstract class TotpDatabase : RoomDatabase() {
    abstract fun daoTotp(): DaoTotp
}