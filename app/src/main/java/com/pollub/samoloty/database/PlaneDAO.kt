package com.pollub.samoloty.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaneDAO {

    @Query("SELECT * FROM Plane")
    fun findAll() : List<Plane>

    @Insert
    fun insert(vararg planes: Plane)

    @Query("SELECT count(*) FROM Plane")
    fun count(): Int
}
