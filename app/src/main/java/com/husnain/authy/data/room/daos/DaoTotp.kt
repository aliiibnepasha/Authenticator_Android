package com.husnain.authy.data.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.husnain.authy.data.room.tables.EntityTotp

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

    @Query("DELETE FROM table_totp WHERE secretKey = :secret")
    suspend fun deleteTotpById(secret: String)

    @Query("UPDATE table_totp SET serviceName = :newServiceName WHERE secretKey = :secretKey")
    suspend fun updateServiceNameBySecretKey(secretKey: String, newServiceName: String)
}