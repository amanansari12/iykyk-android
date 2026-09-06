package com.amanansari.iykyk.data.processor

import android.graphics.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.amanansari.iykyk.data.model.PersonResult
import javax.inject.Inject

class CollageGenerator @Inject constructor() {

    fun generateCollage(
        personResults: List<PersonResult>,
        canvasWidth: Int = 1080
    ): Bitmap {
        require(personResults.isNotEmpty()) {
            "Cannot generate collage with no people"
        }

        val sorted = personResults.sortedBy { it.clusterId }

        // 9:16 Story format -> 1080 x 1920
        val canvasHeight = (canvasWidth * 16f / 9f).toInt()

        val output = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(output)

        drawBackground(
            canvas = canvas,
            width = canvasWidth,
            height = canvasHeight
        )

        drawCollage(
            canvas = canvas,
            people = sorted,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )

        return output
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            "#101419".toColorInt(),
            "#0A0E13".toColorInt(),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )
    }

    private fun drawCollage(
        canvas: Canvas,
        people: List<PersonResult>,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        val padding = (canvasWidth * 0.025f).toInt()
        val gap = (canvasWidth * 0.012f).toInt()
        val cornerRadius = canvasWidth * 0.035f

        val contentWidth = canvasWidth - padding * 2
        val halfWidth = (contentWidth - gap) / 2

        /*
         * Layout for 5 people:
         *
         * ┌─────────────────────────────┐
         * │                             │
         * │          Person 1           │
         * │                             │
         * ├──────────────┬──────────────┤
         * │              │              │
         * │   Person 2   │   Person 3   │
         * │              │              │
         * ├──────────────┼──────────────┤
         * │   Person 4   │   Person 5   │
         * │              │              │
         * └──────────────┴──────────────┘
         *
         * Person 1 gets the hero treatment.
         */

        /*
         * Keep the hero reasonably short.
         * The blurred background fills the area so we don't
         * need to enlarge the actual face to fill the tile.
         */
        val topHeight = (canvasHeight * HERO_HEIGHT_RATIO).toInt()

        val bottomAreaTop = padding + topHeight + gap

        val remainingHeight =
            canvasHeight - bottomAreaTop - padding

        val halfHeight =
            (remainingHeight - gap) / 2

        /*
         * ---------------------------------------------------------
         * Person 1: HERO TILE
         * ---------------------------------------------------------
         */
        if (people.isNotEmpty()) {
            val heroRect = RectF(
                padding.toFloat(),
                padding.toFloat(),
                (padding + contentWidth).toFloat(),
                (padding + topHeight).toFloat()
            )

            drawHeroImage(
                canvas = canvas,
                person = people[0],
                rect = heroRect,
                cornerRadius = cornerRadius
            )
        }

        /*
         * ---------------------------------------------------------
         * Person 2
         * ---------------------------------------------------------
         */
        if (people.size > 1) {
            val rect = RectF(
                padding.toFloat(),
                bottomAreaTop.toFloat(),
                (padding + halfWidth).toFloat(),
                (bottomAreaTop + halfHeight).toFloat()
            )

            drawPersonImage(
                canvas = canvas,
                person = people[1],
                rect = rect,
                cornerRadius = cornerRadius
            )
        }

        /*
         * ---------------------------------------------------------
         * Person 3
         * ---------------------------------------------------------
         */
        if (people.size > 2) {
            val rect = RectF(
                (padding + halfWidth + gap).toFloat(),
                bottomAreaTop.toFloat(),
                (padding + contentWidth).toFloat(),
                (bottomAreaTop + halfHeight).toFloat()
            )

            drawPersonImage(
                canvas = canvas,
                person = people[2],
                rect = rect,
                cornerRadius = cornerRadius
            )
        }

        /*
         * ---------------------------------------------------------
         * Person 4
         * ---------------------------------------------------------
         */
        if (people.size > 3) {
            val rect = RectF(
                padding.toFloat(),
                (bottomAreaTop + halfHeight + gap).toFloat(),
                (padding + halfWidth).toFloat(),
                (bottomAreaTop + halfHeight * 2 + gap).toFloat()
            )

            drawPersonImage(
                canvas = canvas,
                person = people[3],
                rect = rect,
                cornerRadius = cornerRadius
            )
        }

        /*
         * ---------------------------------------------------------
         * Person 5
         * ---------------------------------------------------------
         */
        if (people.size > 4) {
            val rect = RectF(
                (padding + halfWidth + gap).toFloat(),
                (bottomAreaTop + halfHeight + gap).toFloat(),
                (padding + contentWidth).toFloat(),
                (bottomAreaTop + halfHeight * 2 + gap).toFloat()
            )

            drawPersonImage(
                canvas = canvas,
                person = people[4],
                rect = rect,
                cornerRadius = cornerRadius
            )
        }
    }

    /**
     * Draws the first person as a hero tile.
     *
     * The same face is used twice:
     *
     * 1. Full tile background
     *    - enlarged
     *    - blurred
     *    - darkened
     *
     * 2. Foreground portrait
     *    - keeps natural proportions
     *    - does not fill the entire width
     *    - centered inside the tile
     *
     * This prevents the face from becoming unnecessarily large.
     */
    private fun drawHeroImage(
        canvas: Canvas,
        person: PersonResult,
        rect: RectF,
        cornerRadius: Float
    ) {
        val path = Path().apply {
            addRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }

        canvas.save()
        canvas.clipPath(path)

        /*
         * ---------------------------------------------------------
         * Background
         * ---------------------------------------------------------
         *
         * Blurred manually (see blurredBackground/boxBlur below)
         * instead of via Paint.renderEffect, since RenderEffect only
         * applies on a hardware-accelerated canvas (a View or a
         * RenderNode). This canvas is Canvas(output) over a plain
         * Bitmap, i.e. always a software canvas — RenderEffect would
         * silently no-op here on every Android version, not just
         * pre-API-31 ones.
         */

        val backgroundBitmap = blurredBackground(
            source = person.representativeFace,
            targetWidth = rect.width().toInt(),
            targetHeight = rect.height().toInt()
        )

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(
                Color.argb(
                    HERO_OVERLAY_ALPHA,
                    0,
                    0,
                    0
                ),
                PorterDuff.Mode.SRC_OVER
            )
            isFilterBitmap = true
        }

        canvas.drawBitmap(
            backgroundBitmap,
            rect.left,
            rect.top,
            backgroundPaint
        )

        backgroundBitmap.recycle()

        /*
         * ---------------------------------------------------------
         * Foreground portrait
         * ---------------------------------------------------------
         */

        val source = person.representativeFace

        val zoomedSource = zoomIn(
            source = source,
            zoomRatio = HERO_ZOOM_RATIO
        )

        /*
         * The foreground portrait is deliberately smaller than
         * the complete hero tile.
         *
         * This is the important part that prevents the face from
         * becoming huge.
         */
        val foregroundHeight =
            (rect.height() * HERO_FOREGROUND_HEIGHT_RATIO).toInt()

        val sourceRatio =
            zoomedSource.width.toFloat() / zoomedSource.height.toFloat()

        val foregroundWidth =
            (foregroundHeight * sourceRatio).toInt()

        val foreground = scaleToFit(
            source = zoomedSource,
            targetWidth = foregroundWidth,
            targetHeight = foregroundHeight
        )

        if (zoomedSource !== source) {
            zoomedSource.recycle()
        }

        val foregroundLeft =
            rect.centerX() - foreground.width / 2f

        val foregroundTop =
            rect.centerY() - foreground.height / 2f

        /*
         * Slight shadow around the foreground portrait.
         */
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(
                foreground.width * 0.02f,
                0f,
                foreground.width * 0.008f,
                Color.argb(120, 0, 0, 0)
            )
        }

        val foregroundRect = RectF(
            foregroundLeft,
            foregroundTop,
            foregroundLeft + foreground.width,
            foregroundTop + foreground.height
        )

        canvas.drawRoundRect(
            foregroundRect,
            cornerRadius * 0.65f,
            cornerRadius * 0.65f,
            shadowPaint
        )

        canvas.drawBitmap(
            foreground,
            foregroundLeft,
            foregroundTop,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }
        )

        foreground.recycle()

        /*
         * Dark vignette over the entire hero tile.
         */
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                rect.centerX(),
                rect.centerY(),
                rect.width() * 0.75f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(70, 0, 0, 0)
                ),
                floatArrayOf(
                    0.45f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(
            rect,
            vignettePaint
        )

        canvas.restore()
    }

    /**
     * Regular tiles for Persons 2-5.
     */
    private fun drawPersonImage(
        canvas: Canvas,
        person: PersonResult,
        rect: RectF,
        cornerRadius: Float
    ) {
        /*
         * Shadow
         */
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK

            setShadowLayer(
                rect.width() * 0.025f,
                0f,
                rect.width() * 0.01f,
                Color.argb(
                    90,
                    0,
                    0,
                    0
                )
            )
        }

        canvas.drawRoundRect(
            rect,
            cornerRadius,
            cornerRadius,
            shadowPaint
        )

        /*
         * Clip to rounded rectangle.
         */
        val path = Path().apply {
            addRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }

        canvas.save()
        canvas.clipPath(path)

        val bitmap = centerCrop(
            source = person.representativeFace,
            targetWidth = rect.width().toInt(),
            targetHeight = rect.height().toInt()
        )

        canvas.drawBitmap(
            bitmap,
            rect.left,
            rect.top,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }
        )

        canvas.restore()

        bitmap.recycle()
    }

    /**
     * Center crop and resize.
     */
    private fun centerCrop(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val sourceRatio =
            source.width.toFloat() / source.height.toFloat()

        val targetRatio =
            targetWidth.toFloat() / targetHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val x: Int
        val y: Int

        if (sourceRatio > targetRatio) {

            cropHeight = source.height
            cropWidth =
                (source.height * targetRatio)
                    .toInt()
                    .coerceAtMost(source.width)

            x = (source.width - cropWidth) / 2
            y = 0

        } else {

            cropWidth = source.width
            cropHeight =
                (source.width / targetRatio)
                    .toInt()
                    .coerceAtMost(source.height)

            x = 0
            y = (source.height - cropHeight) / 2
        }

        val cropped = Bitmap.createBitmap(
            source,
            x,
            y,
            cropWidth,
            cropHeight
        )

        val scaled = cropped.scale(
            targetWidth,
            targetHeight
        )

        if (scaled !== cropped) {
            cropped.recycle()
        }

        return scaled
    }

    /**
     * Resize while preserving the source aspect ratio.
     */
    private fun scaleToFit(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val sourceRatio =
            source.width.toFloat() / source.height.toFloat()

        val targetRatio =
            targetWidth.toFloat() / targetHeight.toFloat()

        val width: Int
        val height: Int

        if (sourceRatio > targetRatio) {
            width = targetWidth
            height = (targetWidth / sourceRatio).toInt()
        } else {
            height = targetHeight
            width = (targetHeight * sourceRatio).toInt()
        }

        return source.scale(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1)
        )
    }

    /**
     * Crops out the centered [zoomRatio] fraction of [source]. A ratio of 1f
     * is a no-op passthrough; smaller ratios crop in tighter around the
     * center, so the face fills more of whatever tile it ends up in.
     */
    private fun zoomIn(source: Bitmap, zoomRatio: Float): Bitmap {
        if (zoomRatio >= 1f) return source

        val zoomWidth = (source.width * zoomRatio).toInt().coerceAtLeast(1)
        val zoomHeight = (source.height * zoomRatio).toInt().coerceAtLeast(1)

        val x = (source.width - zoomWidth) / 2
        val y = (source.height - zoomHeight) / 2

        return Bitmap.createBitmap(source, x, y, zoomWidth, zoomHeight)
    }

    /**
     * Produces a blurred copy of [source] sized to fit [targetWidth] x [targetHeight].
     *
     * Downscales aggressively first (blurring a tiny bitmap is nearly free), then
     * blurs at that small size, then scales back up — the upscale's bilinear
     * filtering does a lot of the softening on its own. Works identically on
     * every Android version: no RenderEffect (API 31+, hardware-canvas only)
     * and no RenderScript (deprecated) involved.
     */
    private fun blurredBackground(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val smallWidth = (targetWidth * HERO_BACKGROUND_DOWNSCALE_RATIO)
            .toInt()
            .coerceAtLeast(8)

        val smallHeight = (targetHeight * HERO_BACKGROUND_DOWNSCALE_RATIO)
            .toInt()
            .coerceAtLeast(8)

        val small = centerCrop(
            source = source,
            targetWidth = smallWidth,
            targetHeight = smallHeight
        )

        val blurredSmall = boxBlur(
            bitmap = small,
            radius = HERO_BLUR_RADIUS,
            passes = HERO_BLUR_PASSES
        )

        if (blurredSmall !== small) {
            small.recycle()
        }

        val upscaled = blurredSmall.scale(
            targetWidth,
            targetHeight,
            filter = true
        )

        if (upscaled !== blurredSmall) {
            blurredSmall.recycle()
        }

        return upscaled
    }

    /**
     * Dependency-free box blur (a few passes approximates a Gaussian blur).
     * Operates directly on ARGB pixels via getPixels/setPixels.
     */
    private fun boxBlur(
        bitmap: Bitmap,
        radius: Int,
        passes: Int
    ): Bitmap {
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        repeat(passes) {
            boxBlurHorizontal(pixels, w, h, radius)
            boxBlurVertical(pixels, w, h, radius)
        }

        val result = createBitmap(w, h)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun boxBlurHorizontal(
        pixels: IntArray,
        w: Int,
        h: Int,
        radius: Int
    ) {
        val source = pixels.copyOf()
        val count = radius * 2 + 1

        for (y in 0 until h) {
            val rowOffset = y * w

            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0

                for (dx in -radius..radius) {
                    val xi = (x + dx).coerceIn(0, w - 1)
                    val pixel = source[rowOffset + xi]
                    a += (pixel ushr 24) and 0xFF
                    r += (pixel ushr 16) and 0xFF
                    g += (pixel ushr 8) and 0xFF
                    b += pixel and 0xFF
                }

                pixels[rowOffset + x] =
                    ((a / count) shl 24) or
                            ((r / count) shl 16) or
                            ((g / count) shl 8) or
                            (b / count)
            }
        }
    }

    private fun boxBlurVertical(
        pixels: IntArray,
        w: Int,
        h: Int,
        radius: Int
    ) {
        val source = pixels.copyOf()
        val count = radius * 2 + 1

        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0

                for (dy in -radius..radius) {
                    val yi = (y + dy).coerceIn(0, h - 1)
                    val pixel = source[yi * w + x]
                    a += (pixel ushr 24) and 0xFF
                    r += (pixel ushr 16) and 0xFF
                    g += (pixel ushr 8) and 0xFF
                    b += pixel and 0xFF
                }

                pixels[y * w + x] =
                    ((a / count) shl 24) or
                            ((r / count) shl 16) or
                            ((g / count) shl 8) or
                            (b / count)
            }
        }
    }

    companion object {

        /*
         * Hero tile occupies approximately 36% of the story.
         *
         * 1920 * 0.36 ≈ 691px
         */
        private const val HERO_HEIGHT_RATIO = 0.36f

        /*
         * Foreground portrait occupies 95% of the hero height, leaving
         * just a sliver of blurred border showing around it.
         */
        private const val HERO_FOREGROUND_HEIGHT_RATIO = 1.05f

        /*
         * Strength of the black overlay on the blurred background.
         */
        private const val HERO_OVERLAY_ALPHA = 85

        /*
         * Same idea, applied to Person 1's foreground portrait specifically.
         */
        private const val HERO_ZOOM_RATIO = 0.72f

        /*
         * How small the background is shrunk before blurring — blurring
         * at this size is essentially free, and the later upscale does
         * most of the softening.
         */
        private const val HERO_BACKGROUND_DOWNSCALE_RATIO = 0.12f

        /*
         * Box blur radius, applied at the downscaled size.
         */
        private const val HERO_BLUR_RADIUS = 3

        /*
         * Passes of the box blur — 2-3 gives a good Gaussian-like look.
         */
        private const val HERO_BLUR_PASSES = 2
    }
}