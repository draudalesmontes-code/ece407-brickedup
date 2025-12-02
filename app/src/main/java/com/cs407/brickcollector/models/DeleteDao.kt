package com.cs407.brickcollector.models

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DeleteDao {
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    // Delete from specific list type
    @Query("DELETE FROM UserSetCrossRef WHERE userId = :userId AND setId IN (:setIds) AND listType = :listType")
    suspend fun deleteSetsFromUserByListType(setIds: List<Int>, userId: Int, listType: String)

    // Delete from all lists (if needed)
    @Query("DELETE FROM UserSetCrossRef WHERE userId = :userId AND setId IN (:setIds)")
    suspend fun deleteSetsFromUser(setIds: List<Int>, userId: Int)

    // Helper functions for specific list types
    suspend fun deleteFromMyList(setIds: List<Int>, userId: Int) {
        deleteSetsFromUserByListType(setIds, userId, "MY_LIST")
    }

    suspend fun deleteFromWantList(setIds: List<Int>, userId: Int) {
        deleteSetsFromUserByListType(setIds, userId, "WANT_LIST")
    }

    suspend fun deleteFromSellList(setIds: List<Int>, userId: Int) {
        deleteSetsFromUserByListType(setIds, userId, "SELL_LIST")
    }
}