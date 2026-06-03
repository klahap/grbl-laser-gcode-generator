package de.quati.grbl_laser

import java.awt.Shape
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.roundToInt


fun Shape.toGcode(
    tolerance: Double,
    power: UInt,
    speed: UInt,
) = buildString {
    val digits = ceil(-log10(tolerance)).roundToInt().coerceAtLeast(0)
    val ff = "%.${digits}f"
    var isStart = true
    appendLine("M5")
    asSegmentSequence().toLines(tolerance = tolerance).forEach {
        when (it) {
            is PathSegment.MoveTo -> {
                isStart = true
                "G0X${ff}Y${ff}".format(it.p1.x, -it.p1.y)
            }

            is PathSegment.LineTo -> {
                val postfix = if (isStart) {
                    isStart = false
                    "F${speed}S$power"
                } else ""
                "G1X${ff}Y${ff}$postfix".format(it.p1.x, -it.p1.y)
            }
        }.let(::appendLine)
    }
    appendLine("M5")
}.let(::GCode)