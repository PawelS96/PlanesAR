package com.pollub.samoloty.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Plane(

        val modelFilename: String,
        val textureFilename: String,
        val targetName: String,

        val productionYear: Int,
        val topSpeed: Int,
        val country: String,
        val crew : Int,
        val weight: Int,

        val scale: Float = 1f,
        val rotation: Int = 0,
        val multiplier: Float = 5f
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
