package de.quati.grbl_laser

import java.awt.geom.CubicCurve2D
import java.awt.geom.QuadCurve2D

sealed interface PathSegmentCurve {
    fun toLines(p0: Point2D, tolerance: Double): Sequence<PathSegmentLinear>
    sealed interface Bezier {
        fun toCurve(p0: Point2D): Curve2d
    }

    val p1: Point2D

    data class MoveTo(override val p1: Point2D) : PathSegmentCurve {
        override fun toLines(p0: Point2D, tolerance: Double): Sequence<PathSegmentLinear> =
            sequenceOf(PathSegmentLinear.MoveTo(p1))

    }

    data class LineTo(override val p1: Point2D) : PathSegmentCurve {
        override fun toLines(p0: Point2D, tolerance: Double): Sequence<PathSegmentLinear> =
            sequenceOf(PathSegmentLinear.LineTo(p1))

    }
    data class QuadTo(val ctrl: Point2D, override val p1: Point2D) : PathSegmentCurve, Bezier {
        override fun toCurve(p0: Point2D) = Curve2d.Quad(p0, ctrl, p1)
        override fun toLines(p0: Point2D, tolerance: Double) = sequence { subdivide(p0, tolerance = tolerance) }
    }

    data class CubicTo(val ctrl1: Point2D, val ctrl2: Point2D, override val p1: Point2D) :
        PathSegmentCurve, Bezier {
        override fun toCurve(p0: Point2D) = Curve2d.Cubic(p0, ctrl1, ctrl2, p1)

        override  fun toLines(p0: Point2D, tolerance: Double) = sequence { subdivide(p0, tolerance = tolerance) }
    }
}

sealed interface Curve2d {
    val p0: Point2D
    val p1: Point2D
    val flatness: Double
    val segment: PathSegmentCurve.Bezier
    fun subdivide(): Pair<Curve2d, Curve2d>

    @JvmInline
    value class Quad(val curve: QuadCurve2D.Double) : Curve2d {
        constructor(p0: Point2D, ctrl: Point2D, p1: Point2D)
                : this(QuadCurve2D.Double(p0.x, p0.y, ctrl.x, ctrl.y, p1.x, p1.y))

        override val p0 get() = Point2D(curve.x1, curve.y1)
        val ctrl get() = Point2D(curve.ctrlx, curve.ctrly)
        override val p1 get() = Point2D(curve.x2, curve.y2)
        override val flatness: Double get() = curve.flatness

        override val segment get() = PathSegmentCurve.QuadTo(ctrl, p1)
        override fun subdivide(): Pair<Curve2d, Curve2d> {
            val left = QuadCurve2D.Double()
            val right = QuadCurve2D.Double()
            curve.subdivide(left, right)
            return Quad(left) to Quad(right)
        }
    }

    @JvmInline
    value class Cubic(val curve: CubicCurve2D.Double) : Curve2d {
        constructor(p0: Point2D, ctrl1: Point2D, ctrl2: Point2D, p1: Point2D)
                : this(CubicCurve2D.Double(p0.x, p0.y, ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, p1.x, p1.y))

        override val p0 get() = Point2D(curve.x1, curve.y1)
        val ctrl1 get() = Point2D(curve.ctrlx1, curve.ctrly1)
        val ctrl2 get() = Point2D(curve.ctrlx2, curve.ctrly2)
        override val p1 get() = Point2D(curve.x2, curve.y2)
        override val flatness: Double get() = curve.flatness

        override val segment get() = PathSegmentCurve.CubicTo(ctrl1, ctrl2, p1)
        override fun subdivide(): Pair<Curve2d, Curve2d> {
            val left = CubicCurve2D.Double()
            val right = CubicCurve2D.Double()
            curve.subdivide(left, right)
            return Cubic(left) to Cubic(right)
        }
    }
}

fun Sequence<PathSegmentCurve>.toLines(tolerance: Double): Sequence<PathSegmentLinear> = sequence {
    var prev: PathSegmentCurve? = null
    this@toLines.forEach { current ->
        yieldAll(current.toLines(p0 = (prev ?: current).p1, tolerance = tolerance))
        prev = current
    }
}

context(out: SequenceScope<PathSegmentLinear>)
suspend fun PathSegmentCurve.Bezier.subdivide(p0: Point2D, tolerance: Double) {
    val curve = toCurve(p0)
    if (curve.flatness <= tolerance) {
        out.yield(PathSegmentLinear.LineTo(curve.p1)) // flat enough — emit a single line to the endpoint
        return
    }
    val (left, right) = curve.subdivide()
    left.segment.subdivide(left.p0, tolerance = tolerance)
    right.segment.subdivide(right.p0, tolerance = tolerance)
}
