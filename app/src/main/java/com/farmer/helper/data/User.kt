package com.farmer.helper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val mobile: String,   // Mobile number is unique ID
    val name: String,
    val address: String,
    val password: String
)
