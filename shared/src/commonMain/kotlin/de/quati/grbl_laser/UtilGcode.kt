package de.quati.grbl_laser

import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.text.format

fun ShapeLinear.toGcode(tolerance: Double, power: UInt, speed: UInt) = buildString {
    val digits = ceil(-log10(tolerance)).roundToInt().coerceAtLeast(0)
    val ff = "%.${digits}f"
    var isStart = true
    segmentIterator().forEach {
        when (it) {
            is PathSegmentLinear.MoveTo -> {
                isStart = true
                appendLine("M5")
                appendLine("G0X${ff}Y${ff}".format(it.p1.x, -it.p1.y))
            }

            is PathSegmentLinear.LineTo -> {
                val postfix = if (isStart) {
                    isStart = false
                    appendLine("M3S$power")
                    "F${speed}"
                } else ""
                appendLine("G1X${ff}Y${ff}$postfix".format(it.p1.x, -it.p1.y))
            }
        }
    }
    appendLine("M5")
}.let(::GCode)