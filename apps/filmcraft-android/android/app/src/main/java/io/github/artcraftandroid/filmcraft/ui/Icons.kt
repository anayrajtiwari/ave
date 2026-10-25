package io.github.artcraftandroid.filmcraft.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The stroke weight FilmCraft draws Lucide's 24-unit icons with: lighter than Lucide's 2. */
const val ICON_STROKE = 1.6f

/** A Lucide icon from its SVG path strings (see `LucideIconsTask` in app/build.gradle.kts). */
internal fun lucide(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    for (d in paths) {
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

/** An icon: a Lucide vector or one of FilmCraft's own drawn glyphs. */
sealed interface Glyph {
    data class Vector(val image: ImageVector) : Glyph
    data class Drawn(val draw: DrawScope.(Color) -> Unit) : Glyph
}

fun ImageVector.glyph(): Glyph = Glyph.Vector(this)

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, description: String? = null) {
    when (glyph) {
        is Glyph.Vector -> Icon(glyph.image, contentDescription = description, tint = tint, modifier = modifier.size(size))
        is Glyph.Drawn -> Canvas(modifier.size(size)) { glyph.draw(this, tint) }
    }
}

/** FilmCraft's own glyphs, drawn on a 24-unit grid: shapes Lucide has no stroke-only form of. */
object Glyphs {
    private fun DrawScope.u(v: Float) = v * size.minDimension / 24f

    /** A filled play triangle. */
    val Play = Glyph.Drawn { c ->
        val p = Path().apply {
            moveTo(u(7f), u(4.5f)); lineTo(u(19.5f), u(12f)); lineTo(u(7f), u(19.5f)); close()
        }
        drawPath(p, c)
    }

    /** Two filled bars. */
    val Pause = Glyph.Drawn { c ->
        drawRoundRect(c, Offset(u(6f), u(4.5f)), Size(u(4f), u(15f)), CornerRadius(u(1f)))
        drawRoundRect(c, Offset(u(14f), u(4.5f)), Size(u(4f), u(15f)), CornerRadius(u(1f)))
    }

    private fun DrawScope.step(c: Color, forward: Boolean) {
        val w = Stroke(width = u(1.6f), join = StrokeJoin.Round, cap = StrokeCap.Round)
        val x0 = if (forward) u(4f) else u(20f)
        val dir = if (forward) 1f else -1f
        // a bar, then an outlined triangle
        drawLine(c, Offset(x0, u(6f)), Offset(x0, u(18f)), u(1.6f), StrokeCap.Round)
        drawLine(c, Offset(x0 + dir * u(3.5f), u(6f)), Offset(x0 + dir * u(3.5f), u(18f)), u(1.6f), StrokeCap.Round)
        val p = Path().apply {
            moveTo(x0 + dir * u(7.5f), u(6f)); lineTo(x0 + dir * u(16f), u(12f)); lineTo(x0 + dir * u(7.5f), u(18f)); close()
        }
        drawPath(p, c, style = w)
    }

    /** Previous / next frame. */
    val StepBack = Glyph.Drawn { c -> step(c, forward = false) }
    val StepForward = Glyph.Drawn { c -> step(c, forward = true) }

    /** A checkerboard square (opacity). */
    val Checker = Glyph.Drawn { c ->
        val s = u(16f)
        val o = Offset(u(4f), u(4f))
        drawRoundRect(c, o, Size(s, s), CornerRadius(u(2f)), style = Stroke(u(1.6f)))
        val cell = s / 4f
        for (i in 0 until 4) for (j in 0 until 4) {
            if ((i + j) % 2 == 0) drawRect(c, Offset(o.x + i * cell, o.y + j * cell), Size(cell, cell))
        }
    }

    /** A filled record dot. */
    val Record = Glyph.Drawn { c -> drawCircle(c, u(8f), Offset(u(12f), u(12f))) }

    /** A filled stop square. */
    val Stop = Glyph.Drawn { c -> drawRoundRect(c, Offset(u(6.5f), u(6.5f)), Size(u(11f), u(11f)), CornerRadius(u(2f))) }

    /** The empty-transition mark: a single bar. */
    val NoTransition = Glyph.Drawn { c -> drawLine(c, Offset(u(12f), u(5f)), Offset(u(12f), u(19f)), u(2.4f), StrokeCap.Round) }

    /** A transition: two overlapping squares, the front one filled. */
    val Transition = Glyph.Drawn { c ->
        drawRoundRect(c, Offset(u(3f), u(6f)), Size(u(11f), u(12f)), CornerRadius(u(2f)), style = Stroke(u(1.8f)))
        drawRoundRect(c, Offset(u(10f), u(6f)), Size(u(11f), u(12f)), CornerRadius(u(2f)))
    }

    /** A colour wheel ring. */
    val ColorWheel = Glyph.Drawn { _ ->
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
        drawCircle(androidx.compose.ui.graphics.Brush.sweepGradient(colors), u(9f), Offset(u(12f), u(12f)), style = Stroke(u(3.5f)))
    }

    /** Palette for DesignCraft & VectorCraft. */
    val Palette = Glyph.Drawn { c ->
        drawCircle(c, u(8.5f), Offset(u(12f), u(12f)), style = Stroke(u(1.8f)))
        drawCircle(c, u(1.2f), Offset(u(9f), u(9f)))
        drawCircle(c, u(1.2f), Offset(u(14f), u(8.5f)))
        drawCircle(c, u(1.2f), Offset(u(16f), u(13f)))
        drawCircle(c, u(1.8f), Offset(u(10f), u(15f)), style = Stroke(u(1.2f)))
    }

    /** Kinetic Move for EffectCraft. */
    val Move = Glyph.Drawn { c ->
        val s = Stroke(width = u(1.8f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(c, Offset(u(12f), u(4f)), Offset(u(12f), u(20f)), u(1.8f), StrokeCap.Round)
        drawLine(c, Offset(u(4f), u(12f)), Offset(u(20f), u(12f)), u(1.8f), StrokeCap.Round)
        val p = Path().apply {
            moveTo(u(9f), u(7f)); lineTo(u(12f), u(4f)); lineTo(u(15f), u(7f))
            moveTo(u(9f), u(17f)); lineTo(u(12f), u(20f)); lineTo(u(15f), u(17f))
            moveTo(u(7f), u(9f)); lineTo(u(4f), u(12f)); lineTo(u(7f), u(15f))
            moveTo(u(17f), u(9f)); lineTo(u(20f), u(12f)); lineTo(u(17f), u(15f))
        }
        drawPath(p, c, style = s)
    }

    /** Graduation Cap for Craft Academy. */
    val GraduationCap = Glyph.Drawn { c ->
        val s = Stroke(width = u(1.8f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(u(12f), u(5f)); lineTo(u(21f), u(9.5f)); lineTo(u(12f), u(14f)); lineTo(u(3f), u(9.5f)); close()
            moveTo(u(6f), u(11f)); lineTo(u(6f), u(16.5f)); quadraticTo(u(12f), u(20f), u(18f), u(16.5f)); lineTo(u(18f), u(11f))
        }
        drawPath(p, c, style = s)
    }

    /** Overlapping Shapes. */
    val Shapes = Glyph.Drawn { c ->
        val s = Stroke(width = u(1.6f))
        drawCircle(c, u(5f), Offset(u(8.5f), u(9.5f)), style = s)
        drawRect(c, Offset(u(9.5f), u(8.5f)), Size(u(9f), u(9f)), style = s)
    }

    /** Layout Template. */
    val Template = Glyph.Drawn { c ->
        val s = Stroke(width = u(1.6f))
        drawRoundRect(c, Offset(u(3f), u(4f)), Size(u(18f), u(16f)), CornerRadius(u(2f)), style = s)
        drawLine(c, Offset(u(3f), u(9f)), Offset(u(21f), u(9f)), u(1.6f))
        drawLine(c, Offset(u(9f), u(9f)), Offset(u(9f), u(20f)), u(1.6f))
    }
}
