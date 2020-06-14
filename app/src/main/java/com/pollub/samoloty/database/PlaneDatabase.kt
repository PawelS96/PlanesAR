package com.pollub.samoloty.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Plane::class], version = 8, exportSchema = false)
abstract class PlaneDatabase : RoomDatabase() {

    abstract fun planeDAO(): PlaneDAO

    fun populate() {

        if (planeDAO().count() == 0)

        planeDAO().insert(

                Plane("p39.obj", "p39.png", "P39",
                        "Bell P-39 \n Airacobra", 1940, 605,
                        "USA", 1, 3350, scale = 1.8f),

                Plane("spitfire.obj", "spitfire.png", "Spitfire",
                        "Supermarine \n Spitfire", 1938, 582,
                        "Wielka Brytania", 1, 2624),

                Plane("t6.obj", "t6.png", "North_American_T-6_Texan",
                        "North American \n T-6 Texan", 1935, 335,
                        "USA", 2, 2548, scale = 1.5f),

                Plane("arsenal.obj", "arsenal.png", "Arsenal_VG_33",
                        "Arsenal \n VG 33", 1939, 558,
                        "Francja", 1, 2896),

                Plane("ju87.obj", "ju87.jpg", "ju87",
                        "Junkers Ju 87 \n Stuka", 1937, 380,
                        "III Rzesza", 2, 4250, scale = 1.5f)
        )
    }

    companion object {

        fun getDatabase(context: Context): PlaneDatabase {

            return Room.databaseBuilder(context.applicationContext, PlaneDatabase::class.java, "db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .apply { populate() }
        }
    }
}

