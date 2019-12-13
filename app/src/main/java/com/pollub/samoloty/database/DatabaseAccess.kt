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
                    Plane(1, "plane3.obj", "planeTexture.png", "p-39", 1)
            )
        }
    }

    companion object {

        fun getDatabase(context: Context): DatabaseAccess {

            val access = Room.databaseBuilder(context.applicationContext, DatabaseAccess::class.java, "db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            access.populate()

            return access
        }
    }
}

