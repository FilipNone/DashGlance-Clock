package com.filipnone.dashglanceclock.clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withClip
import androidx.core.graphics.withSave
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal class DigitalFace(private val locale: Locale) {

    private val glyphs = GlyphPainters()
    private val dates = DateFormats(locale)
    private val amPmFormatter = DateTimeFormatter.ofPattern("a", locale)
    private val englishAmPm = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val panelRect = RectF()
    private val area = RectF()
    private val path = Path()
    private val flapMatrix = Matrix()
    private val flapFrom = FloatArray(8)
    private val flapTo = FloatArray(8)

    fun draw(
        canvas: Canvas,
        bounds: RectF,
        now: LocalDateTime,
        design: DigitalDesign,
        palette: ClockPalette,
        use24Hour: Boolean,
    ) {
        val millis = now.toLocalTime().toNanoOfDay() / NANOS_PER_MILLI
        area.set(bounds)
        if (design.grid) {
            drawGrid(canvas, bounds, palette.accent, millis)
            // The time and date stand just above the horizon line.
            area.bottom = bounds.top + bounds.height() * (GRID_HORIZON - GRID_CLEARANCE)
        }

        val digits = glyphs[design.glyphs]
        val labels = glyphs.font(design.glyphs as? Glyphs.Font ?: design.date?.font ?: Glyphs.Font())

        val amPm = when {
            use24Hour || !design.amPm -> null
            // A camcorder stamp prints "PM 10:08" in its own pixel font, which only has Latin capitals.
            design.amPmBefore -> now.format(englishAmPm)
            else -> now.format(amPmFormatter)
        }
        fun hourText(time: LocalDateTime): String = if (use24Hour) twoDigits(time.hour) else ((time.hour + 11) % 12 + 1).toString()
        val hours = if (design.amPmBefore && amPm != null) "$amPm ${hourText(now)}" else hourText(now)
        val sideAmPm = amPm.takeUnless { design.amPmBefore }
        val minutes = twoDigits(now.minute)
        val seconds = if (design.seconds) twoDigits(now.second) else null
        val look = design.date
        val date = look?.let {
            val text = dates.format(now, it.style)
            if (it.uppercase) text.uppercase(locale) else text
        }
        val datePainter = look?.let { glyphs[it.glyphs ?: it.font] }
        val dateAbove = look != null && look.position == DatePosition.ABOVE
        val dateBelow = look != null && look.position == DatePosition.BELOW
        val dateAtEnd = look != null && date != null && (look.align ?: design.align) == TextAlign.END

        // Sizes from here on are multiples of the digit height. Text width grows with its height,
        // so measuring everything at height 1 tells us the largest height that fits.
        val gap = digits.gap(1f)
        val overhang = digits.overhang(1f)
        // A flip clock's hour card keeps its size when the hour drops to one digit.
        val wholeCells = design.cells?.whole == true
        val hoursText = digits.width(hours, 1f)
        val hoursWidth = if (wholeCells) max(hoursText, digits.width("00", 1f)) else hoursText
        val colonWidth = digits.width(":", 1f)
        val minutesWidth = digits.width(minutes, 1f)
        val timeWidth = hoursWidth + gap + colonWidth + gap + minutesWidth + overhang
        val sideWidth = max(
            seconds?.let { digits.width(it, SIDE_SIZE) + digits.overhang(SIDE_SIZE) } ?: 0f,
            sideAmPm?.let { labels.width(it, SIDE_SIZE) } ?: 0f,
        )
        val rowWidth = if (sideWidth > 0f) timeWidth + SIDE_GAP + sideWidth else timeWidth
        // A date lined up with the time's right edge can be the wider of the two. The time then
        // moves right to keep the edges together.
        val dateWidth = if (dateAtEnd && datePainter != null) textWidth(datePainter, date, look.size, look.trace) else 0f
        val contentWidth = max(rowWidth, dateWidth)

        val dateRoom = when {
            look == null -> 0f
            dateAbove -> look.size + look.gap
            look.glyphs != null -> look.gap + look.size
            else -> look.gap + look.size + glyphs.font(look.font).descent(look.size)
        }
        val ruleRoom = if (design.rule != null && !dateBelow) RULE_GAP * 2f else 0f
        val cellPadding = design.cells?.padding ?: 0f
        val shift = max(design.chroma?.offset ?: 0f, design.glitch?.edge ?: 0f)
        val padding = (design.panel?.padding ?: 0f) + cellPadding
        val sidePadding = (design.panel?.sidePadding ?: 0f) + cellPadding + shift
        val blockWidth = contentWidth + 2f * sidePadding
        val blockHeight = 1f + dateRoom + ruleRoom + 2f * padding

        val unit = min(
            area.width() * FILL / blockWidth,
            area.height() * FILL / blockHeight,
        )
        val blockLeft = area.centerX() - blockWidth * unit / 2f
        val blockTop = area.centerY() - blockHeight * unit / 2f
        val blockRight = blockLeft + blockWidth * unit
        val blockBottom = blockTop + blockHeight * unit
        val left = blockLeft + sidePadding * unit
        val rowLeft = left + (contentWidth - rowWidth) * unit

        design.panel?.let { panel ->
            panelRect.set(blockLeft, blockTop, blockRight, blockBottom)
            fill.color = palette.panel
            drawPanelShape(canvas, panelRect, panel.corner * unit, panel.chamfer, fill)
            if (panel.border > 0f) {
                // The border is drawn inside the panel's edge, so it never adds to the size.
                val border = panel.border * unit
                panelRect.inset(border / 2f, border / 2f)
                stroke.color = palette.color(panel.borderInk)
                stroke.strokeWidth = border
                drawPanelShape(canvas, panelRect, panel.corner * unit, panel.chamfer, stroke)
            }
        }

        // Each digit gets its own window, the colon none. Whole cells hold a whole number.
        fun forEachCell(text: String, textLeft: Float, baseline: Float, height: Float, action: (RectF) -> Unit) {
            val cells = design.cells ?: return
            val pad = cells.padding * height
            val slant = digits.overhang(height)
            if (cells.whole) {
                val slot = max(digits.width(text, height), digits.width("00", height))
                val slotLeft = textLeft - (slot - digits.width(text, height)) / 2f
                panelRect.set(slotLeft - pad, baseline - height - pad, slotLeft + slot + slant + pad, baseline + pad)
                action(panelRect)
                return
            }
            for ((index, char) in text.withIndex()) {
                if (!char.isDigit()) continue
                val charWidth = digits.width(char.toString(), height)
                val x = textLeft + digits.width(text.substring(0, index + 1), height) - charWidth
                panelRect.set(x - pad, baseline - height - pad, x + charWidth + slant + pad, baseline + pad)
                action(panelRect)
            }
        }

        fun drawCells(text: String, textLeft: Float, baseline: Float, height: Float) {
            val corner = (design.cells?.corner ?: 0f) * height
            fill.color = palette.panel
            forEachCell(text, textLeft, baseline, height) { canvas.drawRoundRect(it, corner, corner, fill) }
        }

        // The gap between a flip clock's two flaps runs across the digits.
        fun drawSplits(text: String, textLeft: Float, baseline: Float, height: Float) {
            if (design.cells?.split != true) return
            val half = height * SPLIT / 2f
            val middle = baseline - height / 2f
            fill.color = palette.background
            forEachCell(text, textLeft, baseline, height) {
                canvas.drawRect(it.left, middle - half, it.right, middle + half, fill)
            }
        }

        var y = blockTop + padding * unit
        val timeBaseline = y + (if (look != null && dateAbove) look.size + look.gap else 0f) * unit + unit
        // The bottom right corner of the digits, which a date at the end lines up with.
        val timeRight = rowLeft + (timeWidth - overhang) * unit

        fun drawDate(text: String, look: DigitalDate, painter: GlyphPainter, baseline: Float) {
            val align = look.align ?: design.align
            val margin = area.width() * (1f - FILL) / 2f
            val maxWidth = when (align) {
                TextAlign.CENTER -> area.width() * FILL
                TextAlign.START -> area.right - margin - left
                TextAlign.END -> timeRight - area.left - margin
            }
            var height = look.size * unit
            val width = textWidth(painter, text, height, look.trace)
            if (width > maxWidth) height *= maxWidth / width
            val shown = textWidth(painter, text, height, look.trace)
            val dateLeft = when (align) {
                TextAlign.CENTER -> area.centerX() - shown / 2f
                TextAlign.START -> left
                TextAlign.END -> timeRight - shown
            }
            val color = palette.color(look.ink)
            if (look.trace > 0f) {
                drawTraced(canvas, painter, text, dateLeft, baseline, height, look.trace * height, color)
            } else {
                design.chroma?.let { chroma ->
                    val offset = chroma.offset * unit
                    painter.draw(canvas, text, dateLeft - offset, baseline, height, palette.color(chroma.left))
                    painter.draw(canvas, text, dateLeft + offset, baseline, height, palette.color(chroma.right))
                }
                painter.draw(canvas, text, dateLeft, baseline, height, color)
            }
        }

        if (look != null && date != null && datePainter != null && dateAbove) {
            drawDate(date, look, datePainter, y + look.size * unit)
            y += (look.size + look.gap) * unit
        }
        y += unit

        // Only the digits glow. A glow on the date would blur small text.
        val glow = if (design.glow > 0f) Glow(design.glow * unit, palette.glow) else null
        // A blinking colon is off on odd seconds. Its space stays, so the digits don't move.
        val colonColor = if (design.blinkColon && now.second % 2 == 1) {
            Color.TRANSPARENT
        } else {
            palette.color(design.colonInk)
        }
        val hoursLeft = rowLeft + (hoursWidth - hoursText) * unit / 2f
        val colonLeft = rowLeft + (hoursWidth + gap) * unit
        val minutesLeft = colonLeft + (colonWidth + gap) * unit
        val sideLeft = rowLeft + (timeWidth + SIDE_GAP) * unit
        val sideHeight = SIDE_SIZE * unit

        fun drawDigits(text: String, textLeft: Float, baseline: Float, height: Float, @ColorInt color: Int, digitGlow: Glow?) {
            design.chroma?.let { chroma ->
                if (Color.alpha(color) > 0) {
                    val offset = chroma.offset * unit
                    digits.draw(canvas, text, textLeft - offset, baseline, height, palette.color(chroma.left))
                    digits.draw(canvas, text, textLeft + offset, baseline, height, palette.color(chroma.right))
                }
            }
            digits.draw(canvas, text, textLeft, baseline, height, color, palette.unlit, digitGlow)
        }

        // The whole time row, moved by dx. A tint repaints every part in one color.
        fun drawTime(dx: Float, dy: Float, @ColorInt tint: Int?, rowGlow: Glow?) {
            val base = y + dy
            drawDigits(hours, hoursLeft + dx, base, unit, tint ?: palette.color(design.hoursInk), rowGlow)
            if (design.colon) {
                val color = if (tint != null && Color.alpha(colonColor) > 0) tint else colonColor
                drawDigits(":", colonLeft + dx, base, unit, color, rowGlow)
            }
            drawDigits(minutes, minutesLeft + dx, base, unit, tint ?: palette.color(design.minutesInk), rowGlow)
            if (seconds != null) {
                drawDigits(seconds, sideLeft + dx, base, sideHeight, tint ?: palette.color(design.secondsInk), rowGlow)
            }
        }

        // The glitch edge sits behind the digits and carries the glow, like the colored rim of
        // a lit sign.
        fun drawLayers(dx: Float, @ColorInt tint: Int?) {
            val glitch = design.glitch
            if (glitch == null) {
                drawTime(dx, 0f, tint, glow)
                return
            }
            val edge = glitch.edge * unit
            drawTime(dx - edge, edge * EDGE_DROP, tint ?: palette.color(glitch.edgeInk), glow)
            drawTime(dx, 0f, tint, null)
        }

        // A card turning over from the old number to the new one. The top flap, printed with the
        // top half of the old number, falls toward the viewer and uncovers the new number's top
        // half. Past halfway it is the bottom flap, printed with the new number's bottom half,
        // and it lands over the old one. Angle runs from 0 to 180 degrees.
        fun drawFlip(oldText: String, oldLeft: Float, newText: String, newLeft: Float, @ColorInt color: Int, angle: Float) {
            val card = RectF()
            forEachCell(newText, newLeft, y, unit) { card.set(it) }
            if (card.isEmpty) return
            val hinge = y - unit / 2f
            val corner = (design.cells?.corner ?: 0f) * unit

            fun face(text: String, textLeft: Float) {
                drawCells(text, textLeft, y, unit)
                drawDigits(text, textLeft, y, unit, color, glow)
            }

            canvas.withClip(card) { drawColor(palette.background) }
            canvas.withClip(card.left, card.top, card.right, hinge) { face(newText, newLeft) }
            canvas.withClip(card.left, hinge, card.right, card.bottom) { face(oldText, oldLeft) }

            val falling = angle < 90f
            val turn = Math.toRadians((if (falling) angle else 180f - angle).toDouble())
            val squash = cos(turn).toFloat()
            // Edge on, the flap is a line and the perspective mapping has nothing to work with.
            if (squash < MIN_FLAP_SQUASH) return
            // The flap's free edge comes toward the viewer as it turns, so it looks wider.
            val spread = card.width() * FLAP_SPREAD * sin(turn).toFloat() / 2f
            val edge = if (falling) card.top else card.bottom
            val far = hinge + (edge - hinge) * squash
            flapFrom[0] = card.left; flapFrom[1] = edge
            flapFrom[2] = card.right; flapFrom[3] = edge
            flapFrom[4] = card.right; flapFrom[5] = hinge
            flapFrom[6] = card.left; flapFrom[7] = hinge
            flapTo[0] = card.left - spread; flapTo[1] = far
            flapTo[2] = card.right + spread; flapTo[3] = far
            flapTo[4] = card.right; flapTo[5] = hinge
            flapTo[6] = card.left; flapTo[7] = hinge
            flapMatrix.setPolyToPoly(flapFrom, 0, flapTo, 0, 4)
            canvas.withSave {
                concat(flapMatrix)
                if (falling) {
                    clipRect(card.left, card.top, card.right, hinge)
                    face(oldText, oldLeft)
                } else {
                    clipRect(card.left, hinge, card.right, card.bottom)
                    face(newText, newLeft)
                }
                // The flap darkens as it turns away from the light.
                fill.color = ColorUtils.setAlphaComponent(Color.BLACK, (FLAP_SHADE * sin(turn)).toInt())
                drawRoundRect(card, corner, corner, fill)
            }
        }

        // All cells go down first, so none of them covers the colon.
        drawCells(hours, hoursLeft, y, unit)
        drawCells(minutes, minutesLeft, y, unit)
        seconds?.let { drawCells(it, sideLeft, y, sideHeight) }

        drawLayers(0f, null)
        if (sideAmPm != null) {
            // Seconds share the time's baseline. AM/PM lines up with the top of the digits.
            labels.draw(canvas, sideAmPm, sideLeft, y - unit + sideHeight, sideHeight, palette.secondaryText)
        }
        // The cards turn for the first moments of each minute. The flap falls like a weight,
        // slowly at first and fastest as it lands.
        val sinceMinute = millis % MILLIS_PER_MINUTE
        if (design.flip && sinceMinute < Motion.FLIP_MILLIS) {
            val progress = sinceMinute.toFloat() / Motion.FLIP_MILLIS
            val angle = 180f * progress * progress
            val before = now.minusMinutes(1)
            val oldHours = hourText(before)
            val oldMinutes = twoDigits(before.minute)
            if (oldHours != hours) {
                val oldLeft = rowLeft + (hoursWidth - digits.width(oldHours, 1f)) * unit / 2f
                drawFlip(oldHours, oldLeft, hours, hoursLeft, palette.color(design.hoursInk), angle)
            }
            if (oldMinutes != minutes) {
                drawFlip(oldMinutes, minutesLeft, minutes, minutesLeft, palette.color(design.minutesInk), angle)
            }
        }
        drawSplits(hours, hoursLeft, y, unit)
        drawSplits(minutes, minutesLeft, y, unit)
        seconds?.let { drawSplits(it, sideLeft, y, sideHeight) }
        val timeBottom = y

        design.rule?.let { rule ->
            val ruleWidth = timeWidth * rule.width * unit
            val ruleY = if (look != null && dateBelow) {
                timeBottom + look.gap * unit / 2f
            } else {
                timeBottom + RULE_GAP * unit
            }
            val ruleLeft = when (design.align) {
                TextAlign.CENTER -> rowLeft + (timeWidth * unit - ruleWidth) / 2f
                TextAlign.START -> rowLeft
                TextAlign.END -> rowLeft + timeWidth * unit - ruleWidth
            }
            val half = rule.thickness * unit / 2f
            fill.color = palette.color(rule.ink)
            canvas.drawRect(ruleLeft, ruleY - half, ruleLeft + ruleWidth, ruleY + half, fill)
        }

        if (look != null && date != null && datePainter != null && dateBelow) {
            drawDate(date, look, datePainter, timeBottom + (look.gap + look.size) * unit)
        }

        design.glitch?.let { glitch ->
            panelRect.set(blockLeft, blockTop, blockRight, blockBottom)
            drawGlitch(canvas, panelRect, timeBaseline - unit, timeBaseline, unit, millis, palette, palette.color(glitch.edgeInk)) {
                    dx, tint ->
                drawLayers(dx, tint)
            }
        }
        if (design.scanlines) drawScanlines(canvas, blockLeft, blockTop, blockRight, blockBottom, unit, palette.background)
    }

    private fun textWidth(painter: GlyphPainter, text: String, height: Float, trace: Float): Float {
        if (trace <= 0f) return painter.width(text, height)
        val letters = text.sumOf { painter.width(it.toString(), height).toDouble() }.toFloat()
        return letters + trace * height * (text.length - 1)
    }

    // Letters spread apart, with a short trace in each gap: a line through the middle, ending
    // in a small ring at every other gap, and a longer one leading in from the left.
    private fun drawTraced(
        canvas: Canvas,
        painter: GlyphPainter,
        text: String,
        left: Float,
        baseline: Float,
        height: Float,
        spacing: Float,
        @ColorInt color: Int,
    ) {
        stroke.color = color
        stroke.strokeWidth = height * TRACE_LINE
        stroke.strokeCap = Paint.Cap.BUTT
        val ring = height * TRACE_RING
        val middle = baseline - height / 2f

        fun trace(from: Float, to: Float, y: Float, ringAtEnd: Boolean) {
            if (ringAtEnd) {
                canvas.drawLine(from, y, to - ring * 2f, y, stroke)
                canvas.drawCircle(to - ring, y, ring, stroke)
            } else {
                canvas.drawLine(from, y, to, y, stroke)
            }
        }

        trace(left - height * TRACE_LEAD, left - spacing * 0.3f, middle + height * 0.18f, ringAtEnd = true)
        var x = left
        for ((index, char) in text.withIndex()) {
            val width = painter.width(char.toString(), height)
            painter.draw(canvas, char.toString(), x, baseline, height, color)
            x += width
            if (index < text.length - 1) {
                val y = if (index % 2 == 0) middle else middle + height * 0.18f
                trace(x + spacing * 0.2f, x + spacing * 0.8f, y, ringAtEnd = index % 2 == 1)
                x += spacing
            }
        }
    }

    // Glitches come in bursts. Most seconds have one, a quarter of a second or so long, and
    // during a burst the picture changes every few frames: slices of the time jump sideways,
    // some of them recolored, and noise bars flash around it. Between bursts a couple of faint
    // dashes stay put. All of it comes from the time, so the same moment always looks the same.
    private fun drawGlitch(
        canvas: Canvas,
        block: RectF,
        rowTop: Float,
        rowBottom: Float,
        unit: Float,
        millis: Long,
        palette: ClockPalette,
        @ColorInt edge: Int,
        drawRow: (dx: Float, tint: Int?) -> Unit,
    ) {
        val second = millis / 1000L
        val within = (millis % 1000L).toInt()
        val start = noise(second, 1) % BURST_LATEST_START
        val length = BURST_MIN + noise(second, 2) % BURST_EXTRA
        val bursting = noise(second, 0) % 100 < BURST_CHANCE && within >= start && within < start + length

        if (!bursting) {
            fill.color = ColorUtils.setAlphaComponent(edge, IDLE_ALPHA)
            for (i in 0 until 2) {
                val x = block.left + block.width() * fraction(second, 80 + i)
                val y = block.top + block.height() * fraction(second, 90 + i)
                canvas.drawRect(x, y, x + unit * (0.08f + 0.2f * fraction(second, 100 + i)), y + unit * 0.012f, fill)
            }
            return
        }

        val frame = millis / GLITCH_FRAME_MILLIS
        val slices = 2 + noise(frame, 3) % 3
        for (i in 0 until slices) {
            val top = rowTop + (rowBottom - rowTop) * fraction(frame, 10 + i) - unit * 0.05f
            val height = unit * (0.04f + 0.14f * fraction(frame, 20 + i))
            val direction = if (noise(frame, 30 + i) % 2 == 0) 1f else -1f
            val dx = direction * unit * (0.04f + 0.2f * fraction(frame, 40 + i))
            val tint = when (noise(frame, 50 + i) % 3) {
                0 -> edge
                1 -> palette.primaryText
                else -> null
            }
            canvas.withClip(block.left, top, block.right, top + height) {
                drawColor(palette.background)
                drawRow(dx, tint)
            }
        }

        val bars = 3 + noise(frame, 60) % 4
        for (i in 0 until bars) {
            val width = unit * (0.05f + 0.4f * fraction(frame, 61 + i))
            val x = block.left + (block.width() - width) * fraction(frame, 70 + i)
            val y = block.top + block.height() * fraction(frame, 80 + i)
            fill.color = if (noise(frame, 90 + i) % 2 == 0) edge else palette.primaryText
            canvas.drawRect(x, y, x + width, y + unit * (0.01f + 0.025f * fraction(frame, 100 + i)), fill)
        }
    }

    // Thin dark lines across the face, like the rows of a CRT or a video picture.
    private fun drawScanlines(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        unit: Float,
        @ColorInt background: Int,
    ) {
        fill.color = ColorUtils.setAlphaComponent(background, SCANLINE_ALPHA)
        val step = unit * SCANLINE_STEP
        val line = step * SCANLINE_SHARE
        var y = top
        while (y < bottom) {
            canvas.drawRect(left, y, right, min(y + line, bottom), fill)
            y += step
        }
    }

    // Rounded corners, or corners cut off at 45 degrees.
    private fun drawPanelShape(canvas: Canvas, rect: RectF, corner: Float, chamfer: Boolean, paint: Paint) {
        if (!chamfer) {
            canvas.drawRoundRect(rect, corner, corner, paint)
            return
        }
        path.reset()
        path.moveTo(rect.left + corner, rect.top)
        path.lineTo(rect.right - corner, rect.top)
        path.lineTo(rect.right, rect.top + corner)
        path.lineTo(rect.right, rect.bottom - corner)
        path.lineTo(rect.right - corner, rect.bottom)
        path.lineTo(rect.left + corner, rect.bottom)
        path.lineTo(rect.left, rect.bottom - corner)
        path.lineTo(rect.left, rect.top + corner)
        path.close()
        canvas.drawPath(path, paint)
    }

    // A floor of glowing lines running off to the horizon, moving toward the viewer. The lines
    // fade out as they get further away, so the far end doesn't turn into a solid bar. A bright
    // pulse runs along the horizon every few seconds.
    private fun drawGrid(canvas: Canvas, bounds: RectF, @ColorInt color: Int, millis: Long) {
        val horizon = bounds.top + bounds.height() * GRID_HORIZON
        val depth = bounds.bottom - horizon
        stroke.color = color
        stroke.strokeWidth = max(1f, bounds.height() * GRID_LINE)
        stroke.strokeCap = Paint.Cap.BUTT
        stroke.shader = LinearGradient(
            0f, horizon, 0f, bounds.bottom,
            ColorUtils.setAlphaComponent(color, 0), ColorUtils.setAlphaComponent(color, GRID_ALPHA),
            Shader.TileMode.CLAMP,
        )
        val travel = (millis % GRID_PERIOD_MILLIS).toFloat() / GRID_PERIOD_MILLIS
        canvas.withClip(bounds) {
            val spacing = bounds.width() / GRID_RAYS
            for (ray in -GRID_RAYS..GRID_RAYS) {
                drawLine(bounds.centerX(), horizon, bounds.centerX() + ray * spacing * 2f, bounds.bottom, stroke)
            }
            // Rows bunch up toward the horizon. Each one slides to where the next one was.
            for (row in 0 until GRID_ROWS) {
                val step = (row + travel) / GRID_ROWS
                val rowY = horizon + depth * step * step
                drawLine(bounds.left, rowY, bounds.right, rowY, stroke)
            }
            stroke.shader = null

            stroke.strokeWidth = max(1f, bounds.height() * GRID_LINE * 1.5f)
            stroke.color = ColorUtils.setAlphaComponent(color, HORIZON_ALPHA)
            drawLine(bounds.left, horizon, bounds.right, horizon, stroke)

            val pulse = (millis % PULSE_PERIOD_MILLIS).toFloat() / PULSE_PERIOD_MILLIS
            val pulseWidth = bounds.width() * PULSE_WIDTH
            val pulseX = bounds.left - pulseWidth + (bounds.width() + pulseWidth) * pulse
            stroke.shader = LinearGradient(
                pulseX, 0f, pulseX + pulseWidth, 0f,
                intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            stroke.strokeWidth = max(1f, bounds.height() * GRID_LINE * 2.5f)
            drawLine(pulseX, horizon, pulseX + pulseWidth, horizon, stroke)
        }
        stroke.shader = null
    }

    private fun twoDigits(value: Int): String = if (value < 10) "0$value" else value.toString()

    private fun fraction(seed: Long, salt: Int): Float = (noise(seed, salt) % 1000) / 1000f

    // A small integer hash, so a glitch frame can be drawn again from its time alone.
    private fun noise(seed: Long, salt: Int): Int {
        var x = seed xor (salt.toLong() * HASH_SALT)
        x = (x xor (x ushr 33)) * HASH_MIX_1
        x = (x xor (x ushr 33)) * HASH_MIX_2
        x = x xor (x ushr 33)
        return (x and Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val SIDE_SIZE = 0.3f
        const val SIDE_GAP = 0.08f
        const val RULE_GAP = 0.2f
        const val SPLIT = 0.03f
        const val MILLIS_PER_MINUTE = 60_000L
        const val MIN_FLAP_SQUASH = 0.02f
        const val FLAP_SPREAD = 0.12f
        const val FLAP_SHADE = 150

        // The face uses at most this fraction of the bounds' width and height. The rest is margin.
        const val FILL = 0.9f

        const val EDGE_DROP = 0.7f
        const val GLITCH_FRAME_MILLIS = 80L
        const val BURST_CHANCE = 80
        const val BURST_LATEST_START = 650
        const val BURST_MIN = 150
        const val BURST_EXTRA = 250
        const val IDLE_ALPHA = 0x70

        const val SCANLINE_STEP = 0.035f
        const val SCANLINE_SHARE = 0.35f
        const val SCANLINE_ALPHA = 0x58

        const val TRACE_LINE = 0.06f
        const val TRACE_RING = 0.11f
        const val TRACE_LEAD = 1.4f

        const val GRID_HORIZON = 0.64f
        const val GRID_CLEARANCE = 0.02f
        const val GRID_LINE = 0.004f
        const val GRID_RAYS = 8
        const val GRID_ROWS = 7
        const val GRID_ALPHA = 0x90
        const val GRID_PERIOD_MILLIS = 1600L
        const val HORIZON_ALPHA = 0xC0
        const val PULSE_PERIOD_MILLIS = 3500L
        const val PULSE_WIDTH = 0.25f

        const val HASH_SALT = -7046029254386353131L
        const val HASH_MIX_1 = -49064778989728563L
        const val HASH_MIX_2 = -4265267296055464877L
    }
}
