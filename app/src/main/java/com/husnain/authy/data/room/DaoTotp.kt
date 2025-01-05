package com.husnain.authy.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DaoTotp {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTotpData(data: EntityTotp)

    @Query("SELECT * FROM table_totp WHERE secretKey = :secretKey")
    suspend fun getTotpBySecretKey(secretKey: String): EntityTotp?

    suspend fun insertOrReplaceTotpData(data: EntityTotp) {
        val existingTotp = getTotpBySecretKey(data.secretKey ?: "")
        if (existingTotp != null) {
            if (existingTotp.secretKey != data.secretKey) {
                insertTotpData(data)
            }
        }else{
            insertTotpData(data)
        }
    }

    @Query("SELECT * FROM table_totp")
    suspend fun getAllTotpData(): List<EntityTotp>
}