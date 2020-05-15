package com.pollub.samoloty

import android.util.Log
import com.pollub.samoloty.database.Plane

class GameManager() {

    private lateinit var targetToPlaneMap: Map<String, Plane>
    var gameMode: GameMode = GameMode.SORT_COUNTRY

    fun setPlanes(planes: List<Plane>) {
        targetToPlaneMap = planes.associateBy { it.targetName }
    }

    fun isOrderCorrect(targets: List<String>): Boolean {

        return targets.containsAll(targetToPlaneMap.keys) && isOrderCorrect(getPlanesForTargets(targets), gameMode)
    }

    private fun getPlanesForTargets(targets: List<String>): List<Plane> = targets.map { targetToPlaneMap[it]!! }

    private fun isOrderCorrect(planes: List<Plane>, mode: GameMode): Boolean {

        when (mode) {
            GameMode.SORT_YEAR -> {

                Log.d("samoloty", planes.map { it.targetName + ": " + it.productionYear }.toString())

                planes.forEachIndexed { index, plane ->

                    if (index > 0 && planes[index - 1].productionYear >= plane.productionYear) {
                        return false
                    }
                }
            }

            GameMode.SORT_SPEED -> {
                Log.d("samoloty", planes.map { it.targetName + ": " + it.topSpeed }.toString())

                planes.forEachIndexed { index, plane ->
                    if (index > 0 && planes[index - 1].topSpeed >= plane.topSpeed) {
                        return false
                    }
                }
            }

            GameMode.SORT_COUNTRY -> {
                Log.d("samoloty", planes.map { it.targetName + ": " + it.country }.toString())

                val countries = planes.map { it.country }
                return countries == countries.sorted()
            }
        }

        return true
    }

    fun getObjective() = gameMode.objective()
}

enum class GameMode {
    SORT_YEAR, SORT_SPEED, SORT_COUNTRY
}

fun GameMode.displayName() : String {

    return when(this){
        GameMode.SORT_YEAR -> "Według roku początku produkcji"
        GameMode.SORT_SPEED -> "Według prędkości maksymalnej"
        GameMode.SORT_COUNTRY -> "Według kraju pochodzenia"
    }
}

fun GameMode.objective() : String {

    return when(this){
        GameMode.SORT_YEAR -> "Według roku początku produkcji"
        GameMode.SORT_SPEED -> "Według prędkości maksymalnej"
        GameMode.SORT_COUNTRY -> "Według kraju pochodzenia"
    }
}