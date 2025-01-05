package com.husnain.authy.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "table_totp")
data class EntityTotp(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    var serviceName: String,
    var secretKey: String = "",
)