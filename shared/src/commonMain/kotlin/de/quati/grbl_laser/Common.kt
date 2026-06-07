package de.quati.grbl_laser

import androidx.compose.ui.graphics.Path


sealed interface GenerateStatus {
    object NotStarted : GenerateStatus
    object InProgress : GenerateStatus
    data class Error(val message: String) : GenerateStatus
    data class Success(val message: String) : GenerateStatus
}

data class Point2D(val x: Double, val y: Double) {
    constructor() : this(0.0, 0.0)
}

sealed interface ShapeLinear {
    val bounds: Rectangle2D
    fun bounds(borderRelative: Double): Rectangle2D {
        val b = bounds
        val dx = bounds.w * borderRelative
        val dy = bounds.h * borderRelative
        return Rectangle2D(
            x0 = b.x0 - dx,
            y0 = b.y0 - dy,
            w = b.w + 2 * dx,
            h = b.h + 2 * dy,
        )
    }

    fun segmentIterator(): Iterator<PathSegmentLinear>

    fun toStatic() = when (this) {
        is Static -> this
        is Stream -> Static(bounds = bounds, data = data.toList())
    }

    fun toComposePath(mapper: CanvasCoordMapper): Path {
        val path = Path()
        segmentIterator().forEach {
            when (it) {
                is PathSegmentLinear.MoveTo -> path.moveTo(mapper.x(it.p1.x), mapper.y(it.p1.y))
                is PathSegmentLinear.LineTo -> path.lineTo(mapper.x(it.p1.x), mapper.y(it.p1.y))
            }
        }
        return path
    }

    class Stream(
        override val bounds: Rectangle2D,
        val data: Sequence<PathSegmentLinear>,
    ) : ShapeLinear {
        override fun segmentIterator(): Iterator<PathSegmentLinear> = data.iterator()
    }

    data class Static(
        override val bounds: Rectangle2D,
        val data: List<PathSegmentLinear>,
    ) : ShapeLinear {
        override fun segmentIterator(): Iterator<PathSegmentLinear> = data.iterator()
    }
}


data class Rectangle2D(
    val x0: Double,
    val y0: Double,
    val w: Double,
    val h: Double
) {
    val x1 get() = (x0 + w)
    val y1 get() = (y0 + h)
}

sealed interface PathSegmentLinear {
    val p1: Point2D

    data class MoveTo(override val p1: Point2D) : PathSegmentLinear
    data class LineTo(override val p1: Point2D) : PathSegmentLinear
}
