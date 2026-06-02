package de.quati.grbl_laser

import java.awt.geom.CubicCurve2D
import java.awt.geom.Point2D
import java.awt.geom.QuadCurve2D


sealed interface PathSegment {
    sealed interface Linear
    sealed interface Curve {
        fun toCurve(p0: Point2D.Double): Curve2d
    }

    val p1: Point2D.Double

    object EndOfPath : PathSegment, Linear {
        override val p1 get() = error("EndOfPath segment does not have a point")
    }

    data class MoveTo(override val p1: Point2D.Double) : PathSegment, Linear
    data class LineTo(override val p1: Point2D.Double) : PathSegment, Linear
    data class QuadTo(val ctrl: Point2D.Double, override val p1: Point2D.Double) : PathSegment, Curve {
        override fun toCurve(p0: Point2D.Double) = Curve2d.Quad(p0, ctrl, p1)
        fun toLines(p0: Point2D.Double, tolerance: Double) = sequence { subdivide(p0, tolerance = tolerance) }
    }

    data class CubicTo(val ctrl1: Point2D.Double, val ctrl2: Point2D.Double, override val p1: Point2D.Double) :
        PathSegment, Curve {
        override fun toCurve(p0: Point2D.Double) = Curve2d.Cubic(p0, ctrl1, ctrl2, p1)

        fun toLines(p0: Point2D.Double, tolerance: Double) = sequence { subdivide(p0, tolerance = tolerance) }
    }
}

sealed interface Curve2d {
    val p0: Point2D.Double
    val p1: Point2D.Double
    val flatness: Double
    val segment: PathSegment.Curve
    fun subdivide(): Pair<Curve2d, Curve2d>

    @JvmInline
    value class Quad(val curve: QuadCurve2D.Double) : Curve2d {
        constructor(p0: Point2D.Double, ctrl: Point2D.Double, p1: Point2D.Double)
                : this(QuadCurve2D.Double(p0.x, p0.y, ctrl.x, ctrl.y, p1.x, p1.y))

        override val p0 get() = Point2D.Double(curve.x1, curve.y1)
        val ctrl get() = Point2D.Double(curve.ctrlx, curve.ctrly)
        override val p1 get() = Point2D.Double(curve.x2, curve.y2)
        override val flatness: Double get() = curve.flatness

        override val segment get() = PathSegment.QuadTo(ctrl, p1)
        override fun subdivide(): Pair<Curve2d, Curve2d> {
            val left = QuadCurve2D.Double()
            val right = QuadCurve2D.Double()
            curve.subdivide(left, right)
            return Quad(left) to Quad(right)
        }
    }

    @JvmInline
    value class Cubic(val curve: CubicCurve2D.Double) : Curve2d {
        constructor(p0: Point2D.Double, ctrl1: Point2D.Double, ctrl2: Point2D.Double, p1: Point2D.Double)
                : this(CubicCurve2D.Double(p0.x, p0.y, ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, p1.x, p1.y))

        override val p0 get() = Point2D.Double(curve.x1, curve.y1)
        val ctrl1 get() = Point2D.Double(curve.ctrlx1, curve.ctrly1)
        val ctrl2 get() = Point2D.Double(curve.ctrlx2, curve.ctrly2)
        override val p1 get() = Point2D.Double(curve.x2, curve.y2)
        override val flatness: Double get() = curve.flatness

        override val segment get() = PathSegment.CubicTo(ctrl1, ctrl2, p1)
        override fun subdivide(): Pair<Curve2d, Curve2d> {
            val left = CubicCurve2D.Double()
            val right = CubicCurve2D.Double()
            curve.subdivide(left, right)
            return Cubic(left) to Cubic(right)
        }
    }
}

fun Sequence<PathSegment>.toLines(tolerance: Double): Sequence<PathSegment.Linear> = sequence {
    var prev: PathSegment? = null
    this@toLines.forEach { current ->
        when (current) {
            is PathSegment.MoveTo -> yield(current)
            is PathSegment.LineTo -> yield(current)
            is PathSegment.QuadTo -> yieldAll(current.toLines(p0 = (prev ?: current).p1, tolerance = tolerance))
            is PathSegment.CubicTo -> yieldAll(current.toLines(p0 = (prev ?: current).p1, tolerance = tolerance))
            PathSegment.EndOfPath -> yield(PathSegment.EndOfPath)
        }
        prev = current
    }
}

context(out: SequenceScope<PathSegment.LineTo>)
suspend fun PathSegment.Curve.subdivide(p0: Point2D.Double, tolerance: Double) {
    val curve = toCurve(p0)
    if (curve.flatness <= tolerance) {
        out.yield(PathSegment.LineTo(curve.p1)) // flat enough — emit a single line to the endpoint
        return
    }
    val (left, right) = curve.subdivide()
    left.segment.subdivide(left.p0, tolerance = tolerance)
    right.segment.subdivide(right.p0, tolerance = tolerance)
}
