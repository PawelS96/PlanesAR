package com.pollub.samoloty.database

object Repository {

    private lateinit var planeDAO: PlaneDAO

    fun create(database: DatabaseAccess){
        planeDAO = database.planeDAO()
    }

    fun getAll() = planeDAO.findAll()
}