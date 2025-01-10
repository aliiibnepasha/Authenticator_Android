package com.husnain.authy.data.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.husnain.authy.data.room.tables.RecentlyDeleted

@Dao
interface DaoRecentlyDeleted {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyDeletedData(data: RecentlyDeleted)

    @Query("SELECT * FROM table_recently_deleted WHERE secret = :secretKey")
    suspend fun getRecentlyDeletedBySecretKey(secretKey: String): RecentlyDeleted?

    suspend fun insertOrReplaceRecentlyDeletedData(data: RecentlyDeleted) {
        val existingData = getRecentlyDeletedBySecretKey(data.secret)
        if (existingData != null) {
            if (existingData != data) {
                insertRecentlyDeletedData(data)
            }
        } else {
            insertRecentlyDeletedData(data)
        }
    }

    @Query("SELECT * FROM table_recently_deleted")
    suspend fun getAllRecentlyDeletedData(): List<RecentlyDeleted>
}
