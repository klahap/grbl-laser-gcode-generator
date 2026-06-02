package de.quati.grbl_laser

import java.awt.Font
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.PathIterator
import java.awt.geom.Point2D

fun Font.generateShape(text: String) = layoutGlyphVector(
    FontRenderContext(
        null,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        RenderingHints.VALUE_FRACTIONALMETRICS_ON
    ),
    text.toCharArray(),
    0,
    text.length,
    Font.LAYOUT_LEFT_TO_RIGHT
)?.outline ?: error("Failed to generate shape for text: $text")

fun Shape.align(anchor: Point2D = Point2D.Double(), hAlign: Align, vAlign: Align): Shape {
    val bounds = this.bounds2D
    val tx = when (hAlign) {
        Align.START -> anchor.x - bounds.x
        Align.CENTER -> anchor.x - bounds.x - bounds.width / 2.0
        Align.END -> anchor.x - bounds.x - bounds.width
    }
    val ty = when (vAlign) {
        Align.START -> anchor.y - bounds.y
        Align.CENTER -> anchor.y - bounds.y - bounds.height / 2.0
        Align.END -> anchor.y - bounds.y - bounds.height
    }
    return AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(this)
}

fun Shape.asSegmentSequence() = sequence {
    val pi = getPathIterator(null)
    val c = DoubleArray(6)
    fun p0() = Point2D.Double(c[0], c[1])
    fun p1() = Point2D.Double(c[2], c[3])
    fun p2() = Point2D.Double(c[4], c[5])
    while (!pi.isDone) {
        when (pi.currentSegment(c)) {
            PathIterator.SEG_MOVETO -> PathSegment.MoveTo(p0())
            PathIterator.SEG_LINETO -> PathSegment.LineTo(p0())
            PathIterator.SEG_QUADTO -> PathSegment.QuadTo(p0(), p1())
            PathIterator.SEG_CUBICTO -> PathSegment.CubicTo(p0(), p1(), p2())
            PathIterator.SEG_CLOSE -> PathSegment.EndOfPath
            else -> null
        }?.let { yield(it) }
        pi.next()
    }
}
