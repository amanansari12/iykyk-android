package com.amanansari.iykyk.data.processor

import android.graphics.*
import com.amanansari.iykyk.data.model.PersonResult
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.min
import androidx.core.graphics.toColorInt
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

class CollageGenerator @Inject constructor() {

    fun generateCollage(
        personResults: List<PersonResult>,
        canvasWidth: Int = 1080
    ): Bitmap {
        require(personResults.isNotEmpty()) { "Cannot generate collage with no people" }

        val sorted = personResults.sortedBy { it.clusterId }
        val columns = calculateColumns(sorted.size)
        val rows = ceil(sorted.size / columns.toFloat()).toInt()

        val spacing = (canvasWidth * SPACING_RATIO).toInt()
        val cardSize = (canvasWidth - spacing * (columns + 1)) / columns
        val labelHeight = (cardSize * LABEL_HEIGHT_RATIO).toInt()
        val cardBlockHeight = cardSize + labelHeight

        val topPadding = (canvasWidth * TOP_PADDING_RATIO).toInt()
        val canvasHeight = topPadding + rows * cardBlockHeight + (rows + 1) * spacing

        val output = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(output)

        drawBackground(canvas, canvasWidth, canvasHeight)
        drawTitle(canvas, canvasWidth, topPadding)

        sorted.forEachIndexed { index, person ->
            val col = index % columns
            val row = index / columns

            val left = spacing + col * (cardSize + spacing)
            val top = topPadding + spacing + row * (cardBlockHeight + spacing)

            drawPersonCard(
                canvas = canvas,
                person = person,
                personNumber = index + 1,
                left = left,
                top = top,
                size = cardSize,
                labelHeight = labelHeight
            )
        }

        return output
    }

    private fun calculateColumns(count: Int): Int = when {
        count <= 2 -> count.coerceAtLeast(1)
        count <= 6 -> 2
        count <= 12 -> 3
        else -> 4
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            "#1A1A2E".toColorInt(),
            "#16213E".toColorInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = gradient })
    }

    private fun drawTitle(canvas: Canvas, width: Int, topPadding: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = topPadding * 0.4f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText("iykyk", width / 2f, topPadding * 0.65f, paint)
    }

    private fun drawPersonCard(
        canvas: Canvas,
        person: PersonResult,
        personNumber: Int,
        left: Int,
        top: Int,
        size: Int,
        labelHeight: Int
    ) {
        val cornerRadius = size * CORNER_RADIUS_RATIO
        val cardRect = RectF(left.toFloat(), top.toFloat(), (left + size).toFloat(), (top + size).toFloat())

        // Shadow: draw a white rounded rect with a shadow layer; the image drawn
        // on top fully covers the white fill, leaving just the cast shadow visible.
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(size * 0.04f, 0f, size * 0.02f, Color.argb(140, 0, 0, 0))
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, shadowPaint)

        // Clip to rounded rect, draw center-cropped face inside it
        val clipPath = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)
        val scaledFace = centerCropToSquare(person.representativeFace, size)
        canvas.drawBitmap(scaledFace, cardRect.left, cardRect.top, null)
        canvas.restore()
        scaledFace.recycle()

        drawCountBadge(canvas, person.appearanceCount, cardRect, size)
        drawNameLabel(canvas, personNumber, cardRect, labelHeight)
    }

    private fun centerCropToSquare(source: Bitmap, targetSize: Int): Bitmap {
        val dimension = min(source.width, source.height)
        val xOffset = (source.width - dimension) / 2
        val yOffset = (source.height - dimension) / 2
        val square = Bitmap.createBitmap(source, xOffset, yOffset, dimension, dimension)
        val scaled = square.scale(targetSize, targetSize)
        if (square != scaled) square.recycle()
        return scaled
    }

    private fun drawCountBadge(canvas: Canvas, appearanceCount: Int, cardRect: RectF, cardSize: Int) {
        val radius = cardSize * BADGE_RADIUS_RATIO
        val cx = cardRect.right - radius * 1.1f
        val cy = cardRect.top + radius * 1.1f

        canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF4757".toColorInt()
            setShadowLayer(radius * 0.3f, 0f, radius * 0.1f, Color.argb(100, 0, 0, 0))
        })

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = radius * 0.9f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText("×$appearanceCount", cx, cy - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    private fun drawNameLabel(canvas: Canvas, personNumber: Int, cardRect: RectF, labelHeight: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = labelHeight * 0.5f
            textAlign = Paint.Align.CENTER
        }
        val centerX = (cardRect.left + cardRect.right) / 2
        canvas.drawText("Person $personNumber", centerX, cardRect.bottom + labelHeight * 0.65f, paint)
    }

    companion object {
        private const val SPACING_RATIO = 0.03f
        private const val TOP_PADDING_RATIO = 0.12f
        private const val LABEL_HEIGHT_RATIO = 0.18f
        private const val CORNER_RADIUS_RATIO = 0.08f
        private const val BADGE_RADIUS_RATIO = 0.09f
    }
}