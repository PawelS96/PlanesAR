package com.pollub.samoloty.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Plane::class], version = 1, exportSchema = false)
abstract class DatabaseAccess : RoomDatabase() {

    abstract fun planeDAO(): PlaneDAO

    fun populate() {

        planeDAO().clear()

        planeDAO().insert(

                //model OK tekstura OK
                Plane("p39.obj", "p39.png", "P39", 1940)

                //do sprawdzenia
                //jak się nie wyświetla albo telepie można kombinowac z size w PlanesDatabase.xml
                //większy size daje mniejszy model i mniej telepania
           /*
           Plane("arsenal.obj", "p39.png", "Arsenal_VG_33", 1939),
                Plane("b17.obj", "p39.png", "b17", 1939),
                Plane("t18.obj", "p39.png", "t18", 1919),
                Plane("dorand.obj", "dorand.jpg", "Dorand_AR1", 1917),
                Plane("ju87.obj", "p39.png", "ju87", 1937),
                Plane("t6.obj", "p39.png", "North_American_T-6_Texan", 1935),
                Plane("spitfire.obj", "spitfire.png", "Spitfire", 1938),
                Plane("camel.obj", "p39.png", "f1camel", 1917),
                Plane("thunderbolt.obj", "p39.png", "Republic_P-47_Thunderbolt", 1941)
                */
                )
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

