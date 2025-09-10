package com.farmer.helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE mobile = :mobile AND password = :password")
    suspend fun login(mobile: String, password: String): User?

    @Query("SELECT * FROM users WHERE mobile = :mobile")
    suspend fun getUser(mobile: String): User?
}
