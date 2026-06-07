package de.quati.grbl_laser

enum class Align { START, CENTER, END }
enum class FontStyle { BOLD, ITALIC }

@JvmInline
value class GCode(val content: String)

data class GeneratorSettings(
    val fontName: String,
    val fontSize: Float,
    val fontStyles: Set<FontStyle>,
    val hAlign: Align,
    val vAlign: Align,
    val laserPower: UInt,
    val laserSpeed: UInt,
)

interface Generator {
    val settings: GeneratorSettings
    val tolerance get() = settings.fontSize.toDouble() / 96
    fun generateShape(data: String): ShapeLinear
    fun generateGCode(data: String): GCode = generateShape(data).toGcode(
        tolerance = tolerance,
        power = settings.laserPower,
        speed = settings.laserSpeed,
    )
}

expect fun GeneratorSettings.toGenerator(): Generator
