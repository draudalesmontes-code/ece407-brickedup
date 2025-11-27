package com.cs407.brickcollector.models

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DeleteDao {
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    @Query("DELETE FROM UserSetCrossRef WHERE userId = :userId AND setId IN (:setIds)")
    suspend fun deleteSetsFromUser(setIds: List<Int>, userId: Int)
}