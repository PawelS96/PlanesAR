package com.pollub.samoloty.database

object Repository {

    private lateinit var planeDAO: PlaneDAO

    fun create(database: PlaneDatabase){
        planeDAO = database.planeDAO()
    }

    fun getAll() = planeDAO.findAll()
}