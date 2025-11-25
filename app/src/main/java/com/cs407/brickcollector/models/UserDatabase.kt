package com.cs407.brickcollector.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cs407.brickcollector.R

@Database(entities = [User::class, LegoSet::class, UserSetCrossRef::class], version = 1)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun legoDao(): LegoDao
    companion object {
        // Singleton prevents multiple instances of database
        // opening at the same time.
        @Volatile
        private var INSTANCE: UserDatabase? = null
        fun getDatabase(context: Context): UserDatabase {
            // If INSTANCE is not null, return it,
            // otherwise create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    context.getString(R.string.user_database),
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}