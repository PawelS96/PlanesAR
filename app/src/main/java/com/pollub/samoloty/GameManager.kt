package com.pollub.samoloty

import com.pollub.samoloty.database.Plane

class GameManager {

    enum class GameMode {
        SORT_YEAR
    }

    fun isOrderCorrect(planes: List<Plane>, mode: GameMode): Boolean {

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