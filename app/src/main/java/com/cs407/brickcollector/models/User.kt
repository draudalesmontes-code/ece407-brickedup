package com.cs407.brickcollector.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// moved to LegoSet.kt
/**
@Entity(
    indices = [Index(
        value = ["userUID"],
        unique = true
    )]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,

    val userUID: String = "",

    val username: String = "",

    val email: String = "",

    val wishlist: List<LegoSet>
)
        **/