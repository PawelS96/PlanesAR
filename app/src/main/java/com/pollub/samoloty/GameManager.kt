package com.pollub.samoloty

import com.pollub.samoloty.database.Plane
import java.io.Serializable
import kotlin.random.Random

object GameManager {

    private lateinit var targetToPlaneMap: Map<String, Plane>
    var sortMode: SortMode = SortMode.SORT_COUNTRY

    fun setPlanes(planes: List<Plane>) {
        targetToPlaneMap = planes.associateBy { it.targetName }
    }

    private var correctOrder : List<Plane>? = null

    fun setCorrectOrder(targets: List<String>){
        correctOrder = targets.map { targetToPlaneMap[it]!! }
    }

    fun getCorrectOrder() : List<Plane> = correctOrder!!

    private var randomizedModes = mutableListOf<SortMode>()

    fun setRandomSortMode() : SortMode {

        val selection: MutableList<SortMode>

        if (randomizedModes.size == SortMode.values().size){
            selection = randomizedModes.take(randomizedModes.size - 2).toMutableList()
            randomizedModes.clear()
        }

        else {
            selection = SortMode.values().toMutableList().apply { removeAll(randomizedModes) }
        }

        sortMode = selection[Random.nextInt(selection.size - 1)]
        randomizedModes.add(sortMode)
        return sortMode
    }

    fun isOrderCorrect(targets: List<String>): Boolean {

        if (!targets.containsAll(targetToPlaneMap.keys) || targetToPlaneMap.isEmpty())
            return false

        val planes = targets.map { targetToPlaneMap[it]!! }

        return when (sortMode) {
            SortMode.SORT_COUNTRY -> {
                val countries = planes.map { it.country }
                countries == countries.sorted()
            }
            else -> checkOrderByNumber(planes, sortMode)
        }
    }

    private fun checkOrderByNumber(planes: List<Plane>, mode: SortMode): Boolean {

        planes.forEachIndexed { index, plane ->

            if (index > 0) {

                val firstPlaneInfo = planes[index - 1].getFieldForSorting<Int>(mode)
                val secondPlaneInfo = plane.getFieldForSorting<Int>(mode)

                if (firstPlaneInfo > secondPlaneInfo)
                    return false
            }
        }

        return true
    }

    fun getObjective() = sortMode.objective()
}

enum class SortMode {
    SORT_YEAR, SORT_SPEED, SORT_COUNTRY, SORT_CREW, SORT_WEIGHT
}

enum class GameMode : Serializable {
    MODE_FREE, MODE_LEVELS
}

inline fun <reified T> Plane.getFieldForSorting(sortMode: SortMode): T {

    return when (sortMode) {
        SortMode.SORT_YEAR -> productionYear as T
        SortMode.SORT_SPEED -> topSpeed as T
        SortMode.SORT_COUNTRY -> country as T
        SortMode.SORT_CREW -> crew as T
        SortMode.SORT_WEIGHT -> weight as T
    }
}

fun SortMode.displayName(): String {

    return when (this) {
        SortMode.SORT_YEAR -> "Według roku początku produkcji"
        SortMode.SORT_SPEED -> "Według prędkości maksymalnej"
        SortMode.SORT_COUNTRY -> "Według kraju pochodzenia"
        SortMode.SORT_CREW -> "Według liczby załogi"
        SortMode.SORT_WEIGHT -> "Według masy startowej"
    }
}

fun SortMode.objective(): String {

    return when (this) {
        SortMode.SORT_YEAR -> "Ustaw samoloty według roku początku produkcji"
        SortMode.SORT_SPEED -> "Ustaw samoloty według prędkości maksymalnej"
        SortMode.SORT_COUNTRY -> "Ustaw samoloty według kraju pochodzenia"
        SortMode.SORT_CREW -> "Ustaw samoloty według liczby załogi"
        SortMode.SORT_WEIGHT -> "Ustaw samoloty według masy startowej"
    }
}