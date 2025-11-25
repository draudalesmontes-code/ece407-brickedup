package com.cs407.brickcollector.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class LegoSet(
    val name: String,
    @PrimaryKey val setId: Int,
    val price: Double,
    val imageId: Int = 0,
)

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,

    val userUID: String = "",

    val username: String = "",

    val email: String = "",

    val city: String? = null
)

@Entity(
    primaryKeys = ["userId", "setId"],
    indices = [Index(value = ["setId"])]
)
data class UserSetCrossRef(
    val userId: Int,
    val setId: Int,
    val listType: String // Either WANT_LIST or SELL_LIST
)
