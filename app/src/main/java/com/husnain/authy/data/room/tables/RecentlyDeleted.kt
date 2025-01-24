package com.husnain.authy.data.room.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_recently_deleted")
data class RecentlyDeleted(
    val name: String,
    val secret: String,
    var firebaseDocId: String = "",
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)
