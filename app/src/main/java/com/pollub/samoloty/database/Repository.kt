package com.pollub.samoloty.database

import android.content.Context

object Repository {

    private var database: DatabaseAccess? = null

    operator fun invoke(context: Context) : Repository {
        if (database == null) database = DatabaseAccess.getDatabase(context)
        return this
    }

    fun getAll() = database!!.planeDAO().findAll()
}