package com.pollub.samoloty.database

import androidx.room.Entity
import androidx.room.PrimaryKey


        @Entity
        data class Plane(

          @PrimaryKey
          val id: Long,
          val modelFilepath: String,
          val textureFilepath: String,
          val targetName: String,
          val productionYear: Int
        )
