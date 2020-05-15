package com.pollub.samoloty.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Plane::class], version = 7, exportSchema = false)
abstract class PlaneDatabase : RoomDatabase() {

    abstract fun planeDAO(): PlaneDAO

    fun populate() {

        planeDAO().clear()

        planeDAO().insert(

                //jak się nie wyświetla albo telepie można kombinowac z size w PlanesDatabase.xml
                //większy size daje mniejszy model i mniej telepania

                //model OK, ma teksturę
                Plane("p39.obj", "p39.png", "P39", 1940, 605, "USA", 1, 3350, scale = 1.5f)
                , Plane("spitfire.obj", "spitfire.png", "Spitfire", 1938, 582, "Wielka Brytania", 1, 2624)
                , Plane("t6.obj", "t6.png", "North_American_T-6_Texan", 1935, 335, "USA",2, 2548)
                ,   Plane("arsenal.obj", "arsenal.png", "Arsenal_VG_33", 1939, 558, "Francja",1,2896 )
                ,   Plane("ju87.obj", "ju87.jpg", "ju87", 1937, 380, "III Rzesza", 2, 4250 )
             //   ,   Plane("thunderbolt2.obj", "thunder.jpg", "Republic_P-47_Thunderbolt", 1941, 770, "USA", 1, 4196,  multiplier = 8f)

                //  ,
                //model OK, bez własnej tekstury
              //  ,Plane("dorand.obj", "spitfire.png", "Dorand_AR1", 1917, 0, "")
           //   ,  Plane("b17.obj", "p39.png", "b17", 1939, 486, "USA", scale = 1.5f, multiplier = 9f)

                //trzeba naciagnac texture w blenderze
                //  Plane("ww1.obj", "p39.png", "P39", 1941, 0, "")

                //dziurawe tekstury
                //piper_pa18

                //dziurawy i tekstury nie widac
                // Plane("t18.obj", "p39.png", "t18", 1919, 300, "sfd")
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

