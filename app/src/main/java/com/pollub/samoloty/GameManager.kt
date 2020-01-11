package com.pollub.samoloty

import com.pollub.samoloty.database.Plane

class GameManager() {

    private lateinit var targetToPlaneMap: Map<String, Plane>
    var gameMode: GameMode = GameMode.SORT_YEAR

    fun setPlanes(planes: List<Plane>) {
        targetToPlaneMap = planes.associate { it.targetName to it }
    }

    enum class GameMode {
        SORT_YEAR
    }

    fun isOrderCorrect(targets: List<String>): Boolean {

        if (!targets.containsAll(targetToPlaneMap.keys)) return false

       // if (targets.size != targetToPlaneMap.size) return false

        return isOrderCorrect(getPlanesForTargets(targets), gameMode)
    }

    private fun getPlanesForTargets(targets: List<String>): List<Plane> = targets.map { targetToPlaneMap[it]!! }

    private fun isOrderCorrect(planes: List<Plane>, mode: GameMode): Boolean {

        when (mode) {
            GameMode.SORT_YEAR -> {

                planes.forEachIndexed { index, plane ->
                    if (index > 0 && planes[index - 1].productionYear >= plane.productionYear)
                        return false
                }
            }
        }

        return true
    }

}