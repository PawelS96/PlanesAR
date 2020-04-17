package com.pollub.samoloty.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Plane(

        val modelFilename: String,
        val textureFilename: String,
        val targetName: String,
        val productionYear: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
