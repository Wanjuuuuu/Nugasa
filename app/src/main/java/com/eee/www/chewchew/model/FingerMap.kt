package com.eee.www.chewchew.model

import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import com.eee.www.chewchew.utils.TAG

class FingerMap {
    companion object {
        private const val MAX_SIZE = 10
    }
    private val _map = mutableMapOf<Int, PointF>()
    val map
        get() = _map as Map<Int, PointF>
    val size
        get() = _map.size

    fun add(event: MotionEvent): Int {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        _map[pointerId] = PointF(event.getX(pointerIndex), event.getY(pointerIndex))
        return pointerId
    }

    fun move(event: MotionEvent) {
        for (pointerIndex in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(pointerIndex)
            _map[pointerId]?.apply {
                x = event.getX(pointerIndex)
                y = event.getY(pointerIndex)
            }
        }
    }

    fun remove(event: MotionEvent): Int {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        _map.remove(pointerId)
        return pointerId
    }

    fun isEmpty() = _map.isEmpty()

    fun isFull() = size == MAX_SIZE

    fun select(n: Int): List<Int> {
        val selected = mutableListOf<Int>()
        for (pointerId in _map.keys) {
            selected.add(pointerId)
        }
        selected.shuffle()
        for (i in 0 until (_map.size - n)) {
            selected.removeAt(0)
        }
        return selected
    }

    fun selectTeam(n: Int): MutableMap<Int, Int> {
        val tempList = mutableListOf<Int>() // to shuffle
        for (key in _map.keys) {
            tempList.add(key)
        }
        tempList.shuffle()

        var teamId: Int;
        val numOfOneTeam = _map.size / n;

        // 0 1 2 3 4 5 6 7 ,  3 -> 0 0 0 1 1 1 2 2(O) / 0 0 1 1 2 2 2 2(X)
        val teamMap = mutableMapOf<Int, Int>()
        tempList.forEach {
            teamId = it / numOfOneTeam
            teamMap.put(it, if (teamId < n) teamId else -1)
        }
        teamId = 0
        teamMap.forEach {
            if (teamMap.get(it.key) == -1) {
                teamMap.put(it.key, teamId++)
            }
        }
        return teamMap
    }

    fun print() {
        _map.forEach { point ->
            Log.d(TAG, "touchPoint:(${point.value.x},${point.value.y})")
        }
    }
}