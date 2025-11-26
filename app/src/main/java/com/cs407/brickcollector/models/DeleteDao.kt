package com.cs407.brickcollector.models

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DeleteDao {
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

}