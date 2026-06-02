package de.quati.grbl_laser

enum class Align { START, CENTER, END }

@JvmInline
value class GCode(val content: String)

data class GenerateGCodeData(
    val inputData: List<String>,
    val fontName: String,
    val fontSize: Float,
    val hAlign: Align,
    val vAlign: Align,
    val laserPower: UInt,
    val laserSpeed: UInt,
)

expect fun generate(data: GenerateGCodeData): Sequence<Pair<String, GCode>>
