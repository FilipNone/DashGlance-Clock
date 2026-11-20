package com.filipnone.dashglanceclock.clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withClip
import androidx.core.graphics.withSave
import kotlin.math.cos
import kotlin.math.sin

// Draws digits that are `height` tall, standing on the baseline. A slanted display leans past
// its width by the overhang, once per line. Text drawn in separate runs, such as hours and
// minutes, needs the gap between runs that a single string would have had.
internal interface GlyphPainter {

    fun width(text: String, height: Float): Float

    fun gap(height: Float): Float = 0f

    fun overhang(height: Float): Float = 0f

    fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int = Color.TRANSPARENT,
        glow: Glow? = null,
    )
}

internal class Glow(val radius: Float, @ColorInt val color: Int)

internal class GlyphPainters {

    private val painters = HashMap<Glyphs, GlyphPainter>()

    operator fun get(glyphs: Glyphs): GlyphPainter = painters.getOrPut(glyphs) {
        when (glyphs) {
            is Glyphs.Font -> FontPainter(glyphs)
            is Glyphs.Segments -> SegmentPainter(glyphs)
            is Glyphs.DotMatrix -> DotMatrixPainter(glyphs)
            is Glyphs.Nixie -> NixiePainter(glyphs, font(glyphs.font))
            is Glyphs.Blade -> BladePainter(glyphs)
        }
    }

    fun font(font: Glyphs.Font): FontPainter = get(font) as FontPainter
}

private fun Paint.setGlow(glow: Glow?) {
    if (glow != null && glow.radius > 0f && Color.alpha(glow.color) > 0) {
        setShadowLayer(glow.radius, 0f, 0f, glow.color)
    } else {
        clearShadowLayer()
    }
}

private fun isVisible(@ColorInt color: Int) = Color.alpha(color) > 0

// Font sizes are set so the digits come out at the requested height. That keeps a light and a
// bold font, or a font and a segment display, the same height on screen.
internal class FontPainter(private val font: Glyphs.Font) : GlyphPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(font.family, font.style)
        textScaleX = font.scaleX
        letterSpacing = font.letterSpacing
        fontFeatureSettings = "tnum"
    }

    private val outline = Paint(paint).apply { style = Paint.Style.STROKE }
    private val outlinePath = Path()
    private val capHeight: Float
    private val descentRatio: Float

    init {
        val bounds = Rect()
        paint.textSize = REFERENCE_HEIGHT
        paint.getTextBounds(CAP_SAMPLE, 0, CAP_SAMPLE.length, bounds)
        capHeight = (-bounds.top / REFERENCE_HEIGHT).takeIf { it > 0f } ?: FALLBACK_CAP_HEIGHT
        descentRatio = paint.descent() / REFERENCE_HEIGHT / capHeight
    }

    override fun width(text: String, height: Float): Float {
        paint.textSize = REFERENCE_HEIGHT / capHeight
        return paint.measureText(text) * height / REFERENCE_HEIGHT
    }

    override fun gap(height: Float): Float = font.gap * height

    fun descent(height: Float): Float = descentRatio * height

    override fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        if (!isVisible(color) || text.isEmpty()) return
        paint.textSize = height / capHeight
        paint.color = color
        paint.setGlow(glow)
        canvas.drawText(text, left, baseline, paint)
    }

    // The digits' outlines only, like the bent wire of an unlit nixie cathode.
    fun drawOutline(canvas: Canvas, text: String, left: Float, baseline: Float, height: Float, @ColorInt color: Int, line: Float) {
        if (!isVisible(color) || text.isEmpty()) return
        paint.textSize = height / capHeight
        paint.getTextPath(text, 0, text.length, left, baseline, outlinePath)
        outline.color = color
        outline.strokeWidth = line
        canvas.drawPath(outlinePath, outline)
    }

    private companion object {
        const val CAP_SAMPLE = "0123456789"
        const val FALLBACK_CAP_HEIGHT = 0.72f
    }
}

// A seven-segment display. Segments are hexagons with pointed ends, so neighbors meet at an
// angle the way they do on LCD and LED clocks.
internal class SegmentPainter(private val style: Glyphs.Segments) : GlyphPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val litPath = Path()
    private val ghostPath = Path()
    private val meshPath = Path()
    private val meshBounds = RectF()

    override fun width(text: String, height: Float): Float {
        if (text.isEmpty()) return 0f
        val advances = text.fold(0f) { sum, char -> sum + advance(char) }
        return (advances - style.spacing) * height
    }

    override fun gap(height: Float): Float = style.spacing * height

    override fun overhang(height: Float): Float = style.slant * height

    private fun advance(char: Char): Float = (if (char == ':') style.thickness else DIGIT_WIDTH) + style.spacing

    override fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        val lit = isVisible(color)
        val ghost = style.ghost && isVisible(unlit)
        if (!lit && !ghost) return

        litPath.reset()
        ghostPath.reset()
        var x = 0f
        for (char in text) {
            when (char) {
                ':' -> {
                    if (ghost) addColon(ghostPath, x, height)
                    if (lit) addColon(litPath, x, height)
                }
                in '0'..'9' -> {
                    val mask = DIGITS[char - '0']
                    if (ghost) addDigit(ghostPath, ALL_SEGMENTS and mask.inv(), x, height)
                    if (lit) addDigit(litPath, mask, x, height)
                }
            }
            x += advance(char) * height
        }

        canvas.withSave {
            translate(left, baseline)
            skew(-style.slant, 0f)
            if (ghost) {
                paint.color = unlit
                paint.setGlow(null)
                drawPath(ghostPath, paint)
            }
            if (lit && style.mesh) {
                // A dim pass carries the glow and fills the wires between the cells.
                paint.color = ColorUtils.setAlphaComponent(color, MESH_WIRE_ALPHA)
                paint.setGlow(glow)
                drawPath(litPath, paint)
                paint.color = color
                paint.setGlow(null)
                withClip(litPath) { drawPath(honeycomb(height), paint) }
            } else if (lit) {
                paint.color = color
                paint.setGlow(glow)
                drawPath(litPath, paint)
            }
        }
    }

    // Small hexagons covering the lit segments, like the grid in front of a VFD's segments.
    private fun honeycomb(height: Float): Path {
        litPath.computeBounds(meshBounds, true)
        val size = MESH_CELL * height
        val across = size * SQRT_3
        val down = size * 1.5f
        val cell = size * MESH_FILL
        meshPath.reset()
        var row = 0
        var y = meshBounds.top
        while (y <= meshBounds.bottom + size) {
            var x = meshBounds.left + if (row % 2 == 1) across / 2f else 0f
            while (x <= meshBounds.right + across) {
                meshPath.moveTo(x, y - cell)
                for (corner in 1..5) {
                    val angle = Math.toRadians(corner * 60.0)
                    meshPath.lineTo(x + cell * sin(angle).toFloat(), y - cell * cos(angle).toFloat())
                }
                meshPath.close()
                x += across
            }
            y += down
            row++
        }
        return meshPath
    }

    // Coordinates are relative to the baseline, so the digit spans y = -height to 0.
    private fun addDigit(path: Path, mask: Int, x: Float, height: Float) {
        val half = style.thickness * height / 2f
        val gap = style.gap * height
        val left = x + half
        val right = x + DIGIT_WIDTH * height - half
        val top = -height + half
        val middle = -height / 2f
        val bottom = -half
        if (mask and A != 0) addHorizontal(path, left, right, top, half, gap)
        if (mask and G != 0) addHorizontal(path, left, right, middle, half, gap)
        if (mask and D != 0) addHorizontal(path, left, right, bottom, half, gap)
        if (mask and F != 0) addVertical(path, left, top, middle, half, gap)
        if (mask and B != 0) addVertical(path, right, top, middle, half, gap)
        if (mask and E != 0) addVertical(path, left, middle, bottom, half, gap)
        if (mask and C != 0) addVertical(path, right, middle, bottom, half, gap)
    }

    private fun addHorizontal(path: Path, x0: Float, x1: Float, y: Float, half: Float, gap: Float) {
        val a = x0 + gap
        val b = x1 - gap
        path.moveTo(a, y)
        path.lineTo(a + half, y - half)
        path.lineTo(b - half, y - half)
        path.lineTo(b, y)
        path.lineTo(b - half, y + half)
        path.lineTo(a + half, y + half)
        path.close()
    }

    private fun addVertical(path: Path, x: Float, y0: Float, y1: Float, half: Float, gap: Float) {
        val a = y0 + gap
        val b = y1 - gap
        path.moveTo(x, a)
        path.lineTo(x + half, a + half)
        path.lineTo(x + half, b - half)
        path.lineTo(x, b)
        path.lineTo(x - half, b - half)
        path.lineTo(x - half, a + half)
        path.close()
    }

    private fun addColon(path: Path, x: Float, height: Float) {
        val half = style.thickness * height / 2f
        val centerX = x + half
        for (y in floatArrayOf(-height * 0.7f, -height * 0.3f)) {
            path.addCircle(centerX, y, half, Path.Direction.CW)
        }
    }

    private companion object {
        const val DIGIT_WIDTH = 0.56f
        const val MESH_CELL = 0.04f
        const val MESH_FILL = 0.8f
        const val MESH_WIRE_ALPHA = 0x50
        const val SQRT_3 = 1.7320508f

        const val A = 1
        const val B = 2
        const val C = 4
        const val D = 8
        const val E = 16
        const val F = 32
        const val G = 64
        const val ALL_SEGMENTS = 127

        val DIGITS = intArrayOf(
            A or B or C or D or E or F,
            B or C,
            A or B or D or E or G,
            A or B or C or D or G,
            B or C or F or G,
            A or C or D or F or G,
            A or C or D or E or F or G,
            A or B or C,
            ALL_SEGMENTS,
            A or B or C or D or F or G,
        )
    }
}

// A 5 x 7 dot matrix. Digits and capital letters are five columns wide, the colon and the
// period one, with one empty column between characters. Lowercase letters are drawn as capitals.
internal class DotMatrixPainter(private val style: Glyphs.DotMatrix) : GlyphPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val litPath = Path()
    private val ghostPath = Path()

    override fun width(text: String, height: Float): Float {
        if (text.isEmpty()) return 0f
        val columns = text.fold(0) { sum, char -> sum + columnsOf(char) + 1 } - 1
        return columns * height / ROWS
    }

    override fun gap(height: Float): Float = height / ROWS

    private fun columnsOf(char: Char): Int = PATTERNS[char.uppercaseChar()]?.first()?.length ?: DIGIT_COLUMNS

    override fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        val lit = isVisible(color)
        val ghost = style.ghost && isVisible(unlit)
        if (!lit && !ghost) return

        val pitch = height / ROWS
        val half = style.dotSize * pitch / 2f
        litPath.reset()
        ghostPath.reset()
        var column = 0
        for (char in text) {
            val pattern = PATTERNS[char.uppercaseChar()]
            if (pattern != null) {
                for ((row, line) in pattern.withIndex()) {
                    for ((col, cell) in line.withIndex()) {
                        val on = cell == '#'
                        val target = when {
                            on && lit -> litPath
                            !on && ghost -> ghostPath
                            else -> continue
                        }
                        val x = left + (column + col + 0.5f) * pitch
                        val y = baseline - height + (row + 0.5f) * pitch
                        if (style.square) {
                            target.addRect(x - half, y - half, x + half, y + half, Path.Direction.CW)
                        } else {
                            target.addCircle(x, y, half, Path.Direction.CW)
                        }
                    }
                }
            }
            column += columnsOf(char) + 1
        }

        if (ghost) {
            paint.color = unlit
            paint.setGlow(null)
            canvas.drawPath(ghostPath, paint)
        }
        if (lit) {
            paint.color = color
            paint.setGlow(glow)
            canvas.drawPath(litPath, paint)
        }
    }

    private companion object {
        const val ROWS = 7
        const val DIGIT_COLUMNS = 5

        private val DIGIT_PATTERNS: Map<Char, List<String>> = mapOf(
            '0' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
            '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
            '3' to listOf(".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."),
            '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
            '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
            '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
            '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
            '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
            '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
            ':' to listOf(".", ".", "#", ".", "#", ".", "."),
        )

        private val LETTERS: Map<Char, List<String>> = mapOf(
            'A' to listOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
            'B' to listOf("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."),
            'C' to listOf(".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."),
            'D' to listOf("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
            'E' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
            'F' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#...."),
            'G' to listOf(".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"),
            'H' to listOf("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
            'I' to listOf(".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."),
            'J' to listOf("..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."),
            'K' to listOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
            'L' to listOf("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
            'M' to listOf("#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"),
            'N' to listOf("#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"),
            'O' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            'P' to listOf("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
            'Q' to listOf(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
            'R' to listOf("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
            'S' to listOf(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
            'T' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
            'U' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            'V' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."),
            'W' to listOf("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#."),
            'X' to listOf("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
            'Y' to listOf("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
            'Z' to listOf("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
            '.' to listOf(".", ".", ".", ".", ".", ".", "#"),
            ' ' to listOf("...", "...", "...", "...", "...", "...", "..."),
            '-' to listOf("...", "...", "...", "###", "...", "...", "..."),
            '/' to listOf("....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."),
        )

        // Declared last, so both maps above are set by the time it is built.
        val PATTERNS: Map<Char, List<String>> = DIGIT_PATTERNS + LETTERS
    }
}

// Each digit stands in its own tube. The unlit numerals sit behind the lit one as thin wires,
// and the anode mesh in front of them is a honeycomb of fine dark lines.
internal class NixiePainter(private val style: Glyphs.Nixie, private val numerals: FontPainter) : GlyphPainter {

    private val dots = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mesh = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = MESH_COLOR
    }
    private val meshPath = Path()

    override fun width(text: String, height: Float): Float {
        if (text.isEmpty()) return 0f
        val advances = text.fold(0f) { sum, char -> sum + advance(char) }
        return (advances - style.spacing) * height
    }

    override fun gap(height: Float): Float = style.spacing * height

    private fun advance(char: Char): Float = (if (char == ':') COLON_WIDTH else TUBE_WIDTH) + style.spacing

    override fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        var x = left
        for (char in text) {
            when (char) {
                ':' -> drawColon(canvas, x + COLON_WIDTH * height / 2f, baseline, height, color, unlit, glow)
                in '0'..'9' -> drawTube(canvas, char, x, baseline, height, color, unlit, glow)
            }
            x += advance(char) * height
        }
    }

    private fun drawTube(
        canvas: Canvas,
        lit: Char,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        val center = left + TUBE_WIDTH * height / 2f
        if (isVisible(unlit)) {
            for (digit in '0'..'9') {
                if (digit == lit) continue
                val text = digit.toString()
                val textLeft = center - numerals.width(text, height) / 2f
                numerals.drawOutline(canvas, text, textLeft, baseline, height, unlit, WIRE * height)
            }
        }
        val text = lit.toString()
        numerals.draw(canvas, text, center - numerals.width(text, height) / 2f, baseline, height, color, glow = glow)
        if (style.mesh) drawMesh(canvas, left, baseline - height, left + TUBE_WIDTH * height, baseline, height)
    }

    private fun drawColon(
        canvas: Canvas,
        center: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        val shown = if (isVisible(color)) color else unlit
        if (!isVisible(shown)) return
        dots.color = shown
        dots.setGlow(if (isVisible(color)) glow else null)
        for (y in floatArrayOf(-height * 0.68f, -height * 0.32f)) {
            canvas.drawCircle(center, baseline + y, COLON_DOT * height, dots)
        }
    }

    private fun drawMesh(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, height: Float) {
        val size = MESH_CELL * height
        val across = size * SQRT_3
        val down = size * 1.5f
        meshPath.reset()
        var row = 0
        var y = top
        while (y <= bottom + size) {
            var x = left + if (row % 2 == 1) across / 2f else 0f
            while (x <= right + across) {
                meshPath.moveTo(x, y - size)
                for (corner in 1..5) {
                    val angle = Math.toRadians(corner * 60.0)
                    meshPath.lineTo(x + size * sin(angle).toFloat(), y - size * cos(angle).toFloat())
                }
                meshPath.close()
                x += across
            }
            y += down
            row++
        }
        mesh.strokeWidth = MESH_LINE * height
        canvas.withClip(left, top, right, bottom) { drawPath(meshPath, mesh) }
    }

    private companion object {
        const val TUBE_WIDTH = 0.62f
        const val COLON_WIDTH = 0.14f
        const val COLON_DOT = 0.045f
        const val WIRE = 0.012f
        const val MESH_CELL = 0.05f
        const val MESH_LINE = 0.008f
        const val SQRT_3 = 1.7320508f
        val MESH_COLOR = Color.argb(0x99, 0, 0, 0)
    }
}

// Each digit is a few straight strokes, given as center lines in a box 0.62 wide and 1 tall,
// with y going down. The strokes are thick with mitred joins, so wherever a stroke turns back
// on itself the corner becomes a spike. Every outline is worked out once, then all digits are
// scaled together so the tallest spike just reaches the top and bottom of the digit.
internal class BladePainter(private val style: Glyphs.Blade) : GlyphPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shapes = HashMap<Char, Path>()
    private val drawn = Path()
    private val place = Matrix()
    private val digitWidth: Float
    private val colonWidth: Float

    init {
        val thickness = style.thickness
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = thickness * SCALE
            strokeJoin = Paint.Join.MITER
            strokeMiter = MITER_LIMIT
            strokeCap = Paint.Cap.BUTT
        }
        val outlines = HashMap<Char, Path>()
        // The box every digit's strokes fit in, and the taller box that also holds the points.
        val bodyBounds = RectF()
        val allBounds = RectF()
        val bounds = RectF()
        for ((char, strokes) in strokes(thickness)) {
            val center = Path()
            for (line in strokes) {
                val p = line.points
                center.moveTo(p[0] * SCALE, p[1] * SCALE)
                for (i in 2 until p.size step 2) center.lineTo(p[i] * SCALE, p[i + 1] * SCALE)
                if (p.size > 4 && p[0] == p[p.size - 2] && p[1] == p[p.size - 1]) center.close()
            }
            val outline = Path()
            pen.getFillPath(center, outline)
            if (char != ':') {
                outline.computeBounds(bounds, true)
                if (bodyBounds.isEmpty) bodyBounds.set(bounds) else bodyBounds.union(bounds)
            }
            for (line in strokes) {
                if (line.startPoint > 0f) outline.op(point(line.points, 0, 2, line.startPoint, thickness), Path.Op.UNION)
                if (line.endPoint > 0f) {
                    val last = line.points.size - 2
                    outline.op(point(line.points, last, last - 2, line.endPoint, thickness), Path.Op.UNION)
                }
            }
            outlines[char] = outline
            if (char != ':') {
                outline.computeBounds(bounds, true)
                if (allBounds.isEmpty) allBounds.set(bounds) else allBounds.union(bounds)
            }
        }

        // One scale for every digit, so a digit without points isn't drawn bigger. The points
        // may reach past the sides into the space between digits, the way the strokes of a
        // hand-drawn logo run into each other.
        val scale = 1f / allBounds.height()
        digitWidth = bodyBounds.width() * scale
        val fit = Matrix().apply {
            setTranslate(-bodyBounds.left, -allBounds.bottom)
            postScale(scale, scale)
        }
        for ((char, outline) in outlines) {
            outline.transform(fit)
            shapes[char] = outline
        }
        val colon = shapes.getValue(':')
        colon.computeBounds(bounds, true)
        colon.offset(-bounds.left, 0f)
        colonWidth = bounds.width()
    }

    override fun width(text: String, height: Float): Float {
        if (text.isEmpty()) return 0f
        val advances = text.fold(0f) { sum, char -> sum + advance(char) }
        return (advances - style.spacing) * height
    }

    override fun gap(height: Float): Float = style.spacing * height

    override fun overhang(height: Float): Float = style.slant * height

    private fun advance(char: Char): Float = (if (char == ':') colonWidth else digitWidth) + style.spacing

    override fun draw(
        canvas: Canvas,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        @ColorInt color: Int,
        @ColorInt unlit: Int,
        glow: Glow?,
    ) {
        if (!isVisible(color)) return
        paint.color = color
        paint.setGlow(glow)
        canvas.withSave {
            translate(left, baseline)
            skew(-style.slant, 0f)
            var x = 0f
            for (char in text) {
                shapes[char]?.let { shape ->
                    place.setScale(height, height)
                    place.postTranslate(x * height, 0f)
                    shape.transform(place, drawn)
                    drawPath(drawn, paint)
                }
                x += advance(char)
            }
        }
    }

    private companion object {
        const val SCALE = 100f
        const val MITER_LIMIT = 3.2f
        const val WIDTH = 0.62f
        const val CHAMFER = 0.22f
        const val POINT = 0.3f

        // A point that carries on from the end of a stroke: a triangle as wide as the stroke,
        // running out to a tip in the stroke's direction.
        fun point(p: FloatArray, end: Int, previous: Int, length: Float, thickness: Float): Path {
            val dx = p[end] - p[previous]
            val dy = p[end + 1] - p[previous + 1]
            val norm = kotlin.math.hypot(dx, dy)
            val ux = dx / norm
            val uy = dy / norm
            val half = thickness / 2f
            return Path().apply {
                moveTo((p[end] - uy * half) * SCALE, (p[end + 1] + ux * half) * SCALE)
                lineTo((p[end] + ux * length) * SCALE, (p[end + 1] + uy * length) * SCALE)
                lineTo((p[end] + uy * half) * SCALE, (p[end + 1] - ux * half) * SCALE)
                close()
            }
        }

        // A stroke's center line as x, y pairs. A line that ends where it started is closed.
        // The start and end lengths add a point to that end.
        class Stroke(vararg val points: Float, val startPoint: Float = 0f, val endPoint: Float = 0f)

        fun strokes(thickness: Float): Map<Char, List<Stroke>> {
            val h = thickness / 2f
            val l = h
            val r = WIDTH - h
            val t = h
            val b = 1f - h
            val c = CHAMFER
            val mid = 0.47f
            val tip = POINT
            return mapOf(
                '0' to listOf(Stroke(l + c, t, r, t, r, b - c, r - c, b, l, b, l, t + c, l + c, t)),
                '1' to listOf(Stroke(l + 0.3f, b, l + 0.3f, t, l, t + 0.24f, endPoint = tip * 0.7f)),
                '2' to listOf(Stroke(l, t, r - c, t, r, t + c, r, 0.44f, l, b, r, b, endPoint = tip)),
                '3' to listOf(
                    Stroke(l, t, r, t, l + 0.16f, mid, r - c * 0.6f, mid, r, mid + c * 0.6f, r, b - c, r - c, b, l, b, endPoint = tip),
                ),
                '4' to listOf(Stroke(r - 0.1f, b, r - 0.1f, t, l, 0.68f, r, 0.68f, endPoint = tip * 0.8f)),
                '5' to listOf(
                    Stroke(r, t, l, t, l, mid, r - c, mid, r, mid + c, r, b - c, r - c, b, l, b, startPoint = tip * 0.7f, endPoint = tip),
                ),
                '6' to listOf(Stroke(r, t, l + c, t, l, t + c, l, b, r, b, r, mid, l, mid, startPoint = tip * 0.7f)),
                '7' to listOf(Stroke(l, t, r, t, l + 0.1f, b, startPoint = tip * 0.7f, endPoint = tip * 0.5f)),
                '8' to listOf(
                    Stroke(l + c, t, r, t, r, b - c, r - c, b, l, b, l, t + c, l + c, t),
                    Stroke(l, mid, r, mid),
                ),
                '9' to listOf(Stroke(r, mid + 0.06f, l, mid + 0.06f, l, t + c, l + c, t, r, t, r, b - c, r - c, b, l, b, endPoint = tip)),
                ':' to listOf(
                    Stroke(0f, 0.3f, thickness, 0.3f),
                    Stroke(0f, 0.72f, thickness, 0.72f),
                ),
            )
        }
    }
}

internal const val REFERENCE_HEIGHT = 100f
