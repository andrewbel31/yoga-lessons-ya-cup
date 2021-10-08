package com.andreibelous.yogalessons.recording

import mr.go.sgfilter.SGFilter
import kotlin.math.abs

class Processor {

    private val filter = SGFilter(N, N)
    private val coefficients = SGFilter.computeSGCoefficients(N, N, 5)

    fun process(amplitude: List<Int>, times: List<Long>): ProcessedResult {
        val withoutZeros = amplitude.removeZeros().map { it.toDouble() }
        val smoothed = filter.smooth(withoutZeros.toDoubleArray(), coefficients)
        val noiseLevel = findNoiseLevel(smoothed)
        val breathe = mutableListOf<Int>()
        val resultTimes = mutableListOf<Int>()

        for (ampl in smoothed) {
            breathe.add(if (ampl > noiseLevel) 1 else 0)
        }

        var prev: Boolean? = null
        for (i in DELTA until breathe.size - DELTA) {
            val sumNext = breathe.subList(i + 1, i + 1 + DELTA).sum()
            val sumPrev = breathe.subList(i - DELTA, i).sum()

            if (abs(sumNext - sumPrev).toFloat() / DELTA > 0.6) {
                val cur = sumNext > sumPrev
                if (cur != prev) {
                    prev = cur
                    resultTimes.add(i)
                }
            }

        }

        return ProcessedResult(
            noiseLevel, smoothed.toList(), resultTimes,
        )
    }

    private fun determinePhases(data: List<Double>, intervals: List<Int>) {
        if (intervals.size % 2 != 0) throw Exception("must be even number")

        val breathe = mutableListOf<Breathe>()
        for (i in intervals.indices step 2) {
            val start = intervals[i]
            val end = intervals[i + 1]
//            breathe.add(data.subList(start, end))
            
        }
    }

    data class Breathe(
        val first: List<Double>, val second: List<Double>
    )

    private fun List<Int>.removeZeros(): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until size) {
            if (this[i] == 0) {
                val prev = getOrElse(i - 1) { 0 }
                val next = getOrElse(i + 1) { 0 }
                val avg = (prev + next) / 2
                result.add(avg)
            } else {
                result.add(this[i])
            }
        }

        return result
    }

    private fun findNoiseLevel(smoothed: DoubleArray): Double {
        val sorted = smoothed.sorted()
        val min = sorted[(smoothed.size * 0.05).toInt()]
        val max = sorted[(smoothed.size * 0.95).toInt()]

        return min + (max - min) * NOISE_LEVEL_PERCENT
    }


    companion object {
        private const val N = 10
        private const val DELTA = 20
        private const val NOISE_LEVEL_PERCENT = 0.15
    }
}

sealed interface Phase {

    data class Pause(val time: Long) : Phase
    data class Inhale(val time: Long) : Phase
    data class Exhale(val time: Long) : Phase
}

data class ProcessedResult(
    val noiseLevel: Double,
    val amplitude: List<Double>,
    val times: List<Int>
)