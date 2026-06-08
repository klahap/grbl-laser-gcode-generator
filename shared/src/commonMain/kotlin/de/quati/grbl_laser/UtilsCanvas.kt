package de.quati.grbl_laser

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
    enum class Type { NORMAL, PRIMARY, ORIGIN_X, ORIGIN_Y }
}

data class CanvasCoordMapper(
    private val canvasSize: Size,
    private val viewRect: Rectangle2D
) {
    val mappedWorldX0 = 0f
    val mappedWorldX1 = canvasSize.width.coerceAtLeast(1f)
    val mappedWorldY0 = 0f
    val mappedWorldY1 = canvasSize.height.coerceAtLeast(1f)
    private val s = minOf(mappedWorldX1 / viewRect.w, mappedWorldY1 / viewRect.h)
    private val offsetX = (mappedWorldX1 - viewRect.w * s) / 2
    private val offsetY = (mappedWorldY1 - viewRect.h * s) / 2
    fun x(x: Double) = ((x - viewRect.x0) * s + offsetX).toFloat()
    fun y(y: Double) = ((y - viewRect.y0) * s + offsetY).toFloat()
    private fun reverseX(x: Float) = (x - offsetX) / s + viewRect.x0
    private fun reverseY(y: Float) = (y - offsetY) / s + viewRect.y0
    val worldX0 = reverseX(mappedWorldX0)
    val worldX1 = reverseX(mappedWorldX1)
    val worldY0 = reverseY(mappedWorldY0)
    val worldY1 = reverseY(mappedWorldY1)
    private val niceGridStep = max(
        niceGridStep(worldX1 - worldX0),
        niceGridStep(worldY1 - worldY0),
    )
    private val niceGridStepWorld = max(
        x(niceGridStep(worldX1 - worldX0)),
        y(niceGridStep(worldY1 - worldY0)),
    )
    val coordFontSize = (niceGridStepWorld * 0.025).sp
    val gridStepsX get() = gridSteps(p0 = worldX0, p1 = worldX1, isX = true)
    val gridStepsY get() = gridSteps(p0 = worldY0, p1 = worldY1, isX = false)

    private fun gridSteps(p0: Double, p1: Double, isX: Boolean): Sequence<GridStep> {
        val innerLines = 5
        val stepSize = niceGridStep / innerLines
        val i0 = ceil(p0 / stepSize).roundToLong()
        val i1 = floor(p1 / stepSize).roundToLong()
        return (i0..i1).asSequence().map {
            GridStep(
                pos = it * stepSize,
                type = when {
                    it == 0L && isX -> GridStep.Type.ORIGIN_X
                    it == 0L && !isX -> GridStep.Type.ORIGIN_Y
                    it % innerLines == 0L -> GridStep.Type.PRIMARY
                    else -> GridStep.Type.NORMAL
                },
            )
        }
    }

    private fun niceGridStep(size: Double, targetLines: Int = 7): Double {
        val size = size.coerceAtLeast(1e-4)
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

private fun GridStep.Type.strokeWidth() = when (this) {
    GridStep.Type.ORIGIN_X, GridStep.Type.ORIGIN_Y -> 2f
    GridStep.Type.PRIMARY -> 2f
    GridStep.Type.NORMAL -> 0.5f
}

private fun GridStep.Type.strokeColor() = when (this) {
    GridStep.Type.ORIGIN_X -> Color(red = 92, green = 201, blue = 59)
    GridStep.Type.ORIGIN_Y -> Color(red = 187, green = 39, blue = 26)
    GridStep.Type.PRIMARY -> Color.Gray
    GridStep.Type.NORMAL -> Color.Gray
}

@Composable
fun ShapeCanvas(
    shape: ShapeLinear?,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        drawRect(color = Color.White, size = size)

        if (shape == null) return@Canvas
        val mapper = CanvasCoordMapper(
            canvasSize = size,
            viewRect = shape.bounds(borderRelative = 0.1)
                .takeIf { !it.isEmpty() } ?: return@Canvas
        )

        // coord grid
        val gridStepsX = mapper.gridStepsX.toList()
        val gridStepsY = mapper.gridStepsY.toList()
        val hasXOrigin = (gridStepsX + gridStepsY).any { it.type == GridStep.Type.ORIGIN_X }
        val hasYOrigin = (gridStepsX + gridStepsY).any { it.type == GridStep.Type.ORIGIN_Y }
        val coordFontWeight = FontWeight.Light

        gridStepsX.forEach { g ->
            val x = mapper.x(g.pos)
            drawLine(
                color = g.type.strokeColor(),
                start = Offset(x, mapper.mappedWorldY1),
                end = Offset(x, mapper.mappedWorldY0),
                strokeWidth = g.type.strokeWidth(),
            )
            if (hasXOrigin && g.type == GridStep.Type.PRIMARY) {
                val style = TextStyle(
                    color = GridStep.Type.ORIGIN_Y.strokeColor(),
                    fontSize = mapper.coordFontSize,
                    fontWeight = coordFontWeight,
                )
                val text = g.pos.toBigDecimal().stripTrailingZeros().toPlainString()
                val measured = textMeasurer.measure(text, style)
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    topLeft = Offset(x - measured.size.width / 2f, mapper.y(.0)),
                    style = style,
                )
            }
        }
        gridStepsY.forEach { g ->
            val y = mapper.y(g.pos)
            drawLine(
                color = g.type.strokeColor(),
                start = Offset(mapper.mappedWorldX0, y),
                end = Offset(mapper.mappedWorldX1, y),
                strokeWidth = g.type.strokeWidth(),
            )
            if (hasYOrigin && g.type == GridStep.Type.PRIMARY) {
                val style = TextStyle(
                    color = GridStep.Type.ORIGIN_X.strokeColor(),
                    fontSize = mapper.coordFontSize,
                    fontWeight = coordFontWeight,
                )
                val text = (-g.pos).toBigDecimal().stripTrailingZeros().toPlainString()
                val measured = textMeasurer.measure(text, style)
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    topLeft = Offset(mapper.x(.0) - measured.size.width * 1.1f, y - measured.size.height / 2f),
                    style = style,
                )
            }
        }

        val path = shape.toComposePath(mapper)
        drawPath(path, color = Color.Blue, style = Stroke(width = 2f))
    }
}