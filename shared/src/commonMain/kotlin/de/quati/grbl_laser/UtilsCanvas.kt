package de.quati.grbl_laser

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong


data class GridStep(
    val pos: Double,
    val type: Type,
) {
    enum class Type { NORMAL, PRIMARY }
}

data class CanvasCoordMapper(
    private val canvasSize: Size,
    private val viewRect: Rectangle2D
) {
    private val s = minOf(canvasSize.width / viewRect.w, canvasSize.height / viewRect.h)
    private val offsetX = (canvasSize.width - viewRect.w * s) / 2
    private val offsetY = (canvasSize.height - viewRect.h * s) / 2
    val mappedWorldX0 = 0f
    val mappedWorldX1 = canvasSize.width
    val mappedWorldY0 = 0f
    val mappedWorldY1 = canvasSize.height
    val worldX0 = reverseX(mappedWorldX0)
    val worldX1 = reverseX(mappedWorldX1)
    val worldY0 = reverseY(mappedWorldY0)
    val worldY1 = reverseY(mappedWorldY1)
    fun x(x: Double) = ((x - viewRect.x0) * s + offsetX).toFloat()
    fun y(y: Double) = ((y - viewRect.y0) * s + offsetY).toFloat()

    private fun reverseX(x: Float) = (x - offsetX) / s + viewRect.x0
    private fun reverseY(y: Float) = (y - offsetY) / s + viewRect.y0

    val gridStepsX get() = gridSteps(p0 = worldX0, p1 = worldX1)
    val gridStepsY get() = gridSteps(p0 = worldY0, p1 = worldY1)

    private fun gridSteps(p0: Double, p1: Double): Sequence<GridStep> {
        val stepSize = niceGridStep
        val i0 = ceil(p0 / stepSize).roundToLong()
        val i1 = floor(p1 / stepSize).roundToLong()
        return (i0..i1).asSequence().map {
            GridStep(
                pos = it * stepSize,
                type = if (it == 0L) GridStep.Type.PRIMARY else GridStep.Type.NORMAL,
            )
        }
    }

    private val niceGridStep = max(
        niceGridStep(worldX1 - worldX0),
        niceGridStep(worldY1 - worldY0),
    )

    private fun niceGridStep(size: Double, targetLines: Int = 7): Double {
        val size = size.coerceAtLeast(0e-4)
        val rough = size / targetLines
        val magnitude = 10.0.pow(floor(log10(rough)))
        val normalized = rough / magnitude
        val nice = when {
            normalized < 1.5 -> 1.0
            normalized < 3.5 -> 2.0
            normalized < 7.5 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }
}

@Composable
fun ShapeCanvas(
    shape: ShapeLinear?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(color = Color.White, size = size)

        if (shape == null) return@Canvas
        fun GridStep.Type.strokeWidth() = when (this) {
            GridStep.Type.PRIMARY -> 2f
            GridStep.Type.NORMAL -> 0.5f
        }

        fun GridStep.Type.strokeColor(isX: Boolean) = when (this) {
            GridStep.Type.PRIMARY -> when (isX) {
                true -> Color(red = 92, green = 201, blue = 59)
                false -> Color(red = 187, green = 39, blue = 26)
            }

            GridStep.Type.NORMAL -> Color.Gray
        }

        val mapper = CanvasCoordMapper(
            canvasSize = size,
            viewRect = shape.bounds(borderRelative = 0.1)
        )

        // coord grid
        mapper.gridStepsX.forEach { g ->
            val x = mapper.x(g.pos)
            drawLine(
                color = g.type.strokeColor(isX = true),
                start = Offset(x, mapper.mappedWorldY1),
                end = Offset(x, mapper.mappedWorldY0),
                strokeWidth = g.type.strokeWidth(),
            )
        }
        mapper.gridStepsY.forEach { g ->
            val y = mapper.y(g.pos)
            drawLine(
                color = g.type.strokeColor(isX = false),
                start = Offset(mapper.mappedWorldX0, y),
                end = Offset(mapper.mappedWorldX1, y),
                strokeWidth = g.type.strokeWidth(),
            )
        }

        val path = shape.toComposePath(mapper)
        drawPath(path, color = Color.Blue, style = Stroke(width = 2f))
    }
}