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
    fun generateGCode(data: String): GCode
}

expect fun GeneratorSettings.toGenerator(): Generator
