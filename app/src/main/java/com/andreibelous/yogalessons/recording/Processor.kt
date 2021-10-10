package com.andreibelous.yogalessons.recording

import mr.go.sgfilter.SGFilter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class Processor {

    private val filter = SGFilter(N, N)
    private val coefficients = SGFilter.computeSGCoefficients(N, N, 5)
    private val sdf = SimpleDateFormat("hh:mm:ss.SSS", Locale.getDefault())
    private val sdfDuration = SimpleDateFormat("ss:SS", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    fun process(amplitude: List<Int>, times: List<Long>): ProcessedResult {
        val smoothed = prepareData(amplitude)
        val noiseLevel = findNoiseLevel(smoothed, NOISE_LEVEL_PERCENT)
        val switchTimesIndices = calculateSwitchTimes(noiseLevel, smoothed)
        val phases =
            determinePhases(
                data = smoothed.toList(),
                times = times,
                intervals = switchTimesIndices.makeEvenElementCount()
            )
        return ProcessedResult(
            noiseLevel = noiseLevel,
            amplitude = smoothed.toList(),
            rawAmplitude = amplitude.map { it.toDouble() },
            times = switchTimesIndices,
            phases = phases
        )
    }

    private fun prepareData(amplitude: List<Int>): DoubleArray {
        // step 1 remove zeros
        val withoutZeros = amplitude.removeZeros().map { it.toDouble() }.toMutableList()
        val max = withoutZeros.maxOrNull()!!
        val min = withoutZeros.minOrNull()!!
        val halfHeight = (max - min) / 2
        // step 2 rough noise level
        val noiseLevel = findNoiseLevel(withoutZeros.toDoubleArray(), 0.25f)
        // step 3 increase amplitude
        withoutZeros.forEachIndexed { index, d ->
            if (d > noiseLevel) {
                withoutZeros[index] = d + halfHeight
            } else {
                // nothing
            }
        }

        return filter.smooth(withoutZeros.toDoubleArray(), coefficients)
    }

    private fun calculateSwitchTimes(noiseLevel: Double, data: DoubleArray): List<Int> {
        // 0 if not breathing 1 if breathing
        val breathe = data.map { if (it > noiseLevel) 1 else 0 }

        // indices of times in which switch pause -> breathe or breathe -> pause more likely happens
        val switchTimes = mutableListOf<Int>()

        var prev = false
        for (i in DELTA until breathe.size - DELTA) {
            val sumNext = breathe.subList(i + 1, i + 1 + DELTA).sum()
            val sumPrev = breathe.subList(i - DELTA, i).sum()

            if (abs(sumNext - sumPrev).toFloat() / DELTA > 0.75) {
                val cur = sumNext > sumPrev
                // sumNext > sumPrev -> probably start
                if (cur != prev) {
                    prev = cur
                    switchTimes.add(i)
                }
            }
        }

        return switchTimes
    }

    private fun determinePhases(
        data: List<Double>,
        times: List<Long>,
        intervals: List<Int>
    ): List<Phase> {

        if (!intervals.size.isEven()) throw Exception("must be even number")
        val maximums = mutableListOf<Double>()
        for (i in intervals.indices step 2) {
            val start = intervals[i]
            val end = intervals[i + 1]
            val br = data.subList(start, end)
            val max = br.maxOrNull()!!
            maximums += max
        }

        var sum = 0
        for (i in 0 until maximums.size step 2) {
            val first = maximums[i]
            val second = maximums.getOrNull(i + 1) ?: first

            if (second > first) {
                sum += 1
            }
        }

        val inhaleFirst = sum / 2f >= 0.5f

        // i in intervals is the time index in which this interval started
        val phases = mutableListOf<Phase>().apply {
            val start = times.first()
            val end = times[intervals.first()]
            add(
                Phase(
                    start = start,
                    end = end,
                    type = Phase.Type.Pause,
                    startStr = start.formatEndPoints(),
                    endStr = end.formatEndPoints(),
                    durationStr = (end - start).formatDuration()
                )
            )
        }

        var lastAddedType: Phase.Type =
            if (inhaleFirst) {
                Phase.Type.Exhale
            } else {
                Phase.Type.Exhale
            }
        for (i in intervals.indices step 2) {
            val typeToAdd =
                if (lastAddedType == Phase.Type.Inhale) Phase.Type.Exhale else Phase.Type.Inhale

            val start = times[intervals[i]]
            val end = times[intervals[i + 1]]

            phases.add(
                Phase(
                    start = start,
                    end = end,
                    type = typeToAdd,
                    startStr = start.formatEndPoints(),
                    endStr = end.formatEndPoints(),
                    durationStr = (end - start).formatDuration()
                )
            )

            lastAddedType = typeToAdd

            val pauseStart = times[intervals[i + 1]]
            val pauseEnd = times[intervals.getOrNull(i + 2) ?: times.lastIndex]
            phases.add(
                Phase(
                    start = pauseStart,
                    end = pauseEnd,
                    type = Phase.Type.Pause,
                    startStr = pauseStart.formatEndPoints(),
                    endStr = pauseEnd.formatEndPoints(),
                    durationStr = (pauseEnd - pauseStart).formatDuration()
                )
            )
        }

        return phases
    }

    private fun <T> List<T>.makeEvenElementCount() =
        if (size.isEven()) this else this.subList(0, size - 1)

    private fun Long.formatEndPoints(): String {
        calendar.timeInMillis = this
        return sdf.format(calendar.time)
    }

    private fun Long.formatDuration(): String {
        calendar.timeInMillis = this
        return sdfDuration.format(calendar.time)
    }

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

    private fun findNoiseLevel(smoothed: DoubleArray, percent: Float): Double {
        val sorted = smoothed.sorted()
        val min = sorted[(smoothed.size * 0.05).toInt()]
        val max = sorted[(smoothed.size * 0.97).toInt()]

        return min + (max - min) * percent
    }

    private fun Int.isEven() = this % 2 == 0

    companion object {
        private const val N = 7
        private const val DELTA = 30
        private const val NOISE_LEVEL_PERCENT = 0.16f
    }
}

data class Phase(
    val start: Long,
    val end: Long,
    val startStr: String,
    val endStr: String,
    val durationStr: String,
    val type: Type
) {

    sealed interface Type {

        object Pause : Type
        object Inhale : Type
        object Exhale : Type
    }

}

data class ProcessedResult(
    val noiseLevel: Double,
    val amplitude: List<Double>,
    val rawAmplitude: List<Double>,
    val times: List<Int>,
    val phases: List<Phase>
)