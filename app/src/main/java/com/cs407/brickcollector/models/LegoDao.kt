package com.cs407.brickcollector.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import androidx.room.Transaction

@Dao
interface LegoDao {

    @Query("SELECT * FROM LegoSet WHERE setId = :id")
    suspend fun getSetById(id: Int): LegoSet

    @Insert(entity = LegoSet :: class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(legoSet: LegoSet)

    @Insert(entity = LegoSet :: class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(legoSets: List<LegoSet>)

    @Transaction
    suspend fun insertWantListSet(userId: Int, set: LegoSet) {
        insertSet(set)
        val crossRef = UserSetCrossRef(userId = userId, setId = set.setId, listType = "WANT_LIST")
        addUserSetCrossRef(crossRef)
    }

    @Transaction
    suspend fun insertSellListSet(userId: Int, set: LegoSet) {
        insertSet(set)
        val crossRef = UserSetCrossRef(userId = userId, setId = set.setId, listType = "SELL_LIST")
        addUserSetCrossRef(crossRef)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUserSetCrossRef(crossRef: UserSetCrossRef)

    /**
     * Get all sets from a user's WantList
     */
    @Transaction
    @Query("""
        SELECT * FROM LegoSet
        INNER JOIN UserSetCrossRef
        ON LegoSet.setId = UserSetCrossRef.setId
        WHERE UserSetCrossRef.userId = :userId AND UserSetCrossRef.listType = 'WANT_LIST'
    """)
    suspend fun getWantListSets(userId: Int): List<LegoSet>

    /**
     * Get all sets from a user's SellList
     */
    @Transaction
    @Query("""
        SELECT * FROM LegoSet
        INNER JOIN UserSetCrossRef
        ON LegoSet.setId = UserSetCrossRef.setId
        WHERE UserSetCrossRef.userId = :userId AND UserSetCrossRef.listType = 'SELL_LIST'
    """)
    suspend fun getSellListSets(userId: Int): List<LegoSet>
}