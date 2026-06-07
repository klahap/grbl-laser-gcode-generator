package de.quati.grbl_laser


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
    fun segmentIterator(): Iterator<PathSegmentLinear>

    fun toStatic() = when(this) {
        is Static -> this
        is Stream -> Static(bounds = bounds, data = data.toList())
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
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

sealed interface PathSegmentLinear {
    val p1: Point2D

    data class MoveTo(override val p1: Point2D) : PathSegmentLinear
    data class LineTo(override val p1: Point2D) : PathSegmentLinear
}
