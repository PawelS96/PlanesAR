package com.pollub.samoloty.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Plane::class], version = 1, exportSchema = false)
abstract class DatabaseAccess : RoomDatabase() {

    abstract fun planeDAO(): PlaneDAO

    fun populate() {

        if (planeDAO().count() == 0) {

            planeDAO().insert(
                    Plane(1, "plane3.obj", "planeTexture.png", "p-39", 1),
                    Plane(2, "plane3.obj", "planeTexture.png", "F4U1", 2)
            )
        }
    }

    companion object {

        fun getDatabase(context: Context): DatabaseAccess {

            return Room.databaseBuilder(context.applicationContext, DatabaseAccess::class.java, "db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .apply { populate() }
        }
    }
}

