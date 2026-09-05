package com.amanansari.iykyk.data.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.scale
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.math.sqrt

class FaceEmbedding @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MODEL_INPUT_SIZE = 160
        private const val EMBEDDING_SIZE = 512

        // Minimum acceptable face dimensions in the original frame.
        private const val MIN_FACE_SIZE = 80

        // At least 80% of detected face box must be inside frame.
        private const val MIN_VISIBLE_RATIO = 0.80f

        // Reject detections that extend too far outside the frame.
        private const val MAX_OUTSIDE_FRAME_RATIO = 0.20f

        // Reject detections that occupy an implausibly large part of the frame.
        private const val MAX_FACE_AREA_RATIO = 0.80f

        // Starting sharpness threshold after resizing face crop
        // to 160x160.
        private const val MIN_SHARPNESS = 50.0

    }

    private data class PreparedFace(
        val bitmap: Bitmap,
        val sharpness: Double,
        val visibleRatio: Float
    )


    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile())
    }

    /**
     * Generates a 512-dimensional embedding for one detected face.
     */
    fun generateEmbedding(
        bitmap: Bitmap,
        detectedFace: DetectedFace
    ): FaceEmbeddingResult? {

        val preparedFace = prepareFace(
            bitmap = bitmap,
            detectedFace = detectedFace
        ) ?: return null

        Log.d(
            "FaceEmbedding",
            "Generating embedding: " +
                    "timestamp=${detectedFace.timestampMs}, " +
                    "crop=${preparedFace.bitmap.width}x${preparedFace.bitmap.height}"
        )

        val inputBuffer = convertBitmapToBuffer(
            preparedFace.bitmap
        )

        val output = Array(1) {
            FloatArray(EMBEDDING_SIZE)
        }

        interpreter.run(
            inputBuffer,
            output
        )

        Log.d(
            "FaceEmbedding",
            "Generated embedding: " +
                    "timestamp=${detectedFace.timestampMs}, " +
                    "values=${output[0].size}"
        )

        return FaceEmbeddingResult(
            detectedFace = detectedFace,
            embedding = output[0],
            sharpness = preparedFace.sharpness,
            visibleRatio = preparedFace.visibleRatio
        )
    }

    private fun prepareFace(
        bitmap: Bitmap,
        detectedFace: DetectedFace
    ): PreparedFace? {

        val box = detectedFace.boundingBox

        // 1. Validate bounding box
        val boxWidth = box.width()
        val boxHeight = box.height()

        if (boxWidth <= 0 || boxHeight <= 0) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: invalid bounding box " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "box=$box"
            )
            return null
        }

        // 2. Minimum face size
        if (
            boxWidth < MIN_FACE_SIZE ||
            boxHeight < MIN_FACE_SIZE
        ) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: face too small " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "width=$boxWidth, " +
                        "height=$boxHeight"
            )
            return null
        }

        // 3. Calculate visible area
        val visibleLeft = box.left.coerceIn(0, bitmap.width)
        val visibleTop = box.top.coerceIn(0, bitmap.height)
        val visibleRight = box.right.coerceIn(0, bitmap.width)
        val visibleBottom = box.bottom.coerceIn(0, bitmap.height)

        val visibleWidth = visibleRight - visibleLeft
        val visibleHeight = visibleBottom - visibleTop

        if (
            visibleWidth <= 0 ||
            visibleHeight <= 0
        ) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: face completely outside frame " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "box=$box"
            )
            return null
        }

        val boxArea =
            boxWidth.toLong() * boxHeight.toLong()

        val visibleArea =
            visibleWidth.toLong() * visibleHeight.toLong()

        val visibleRatio =
            visibleArea.toFloat() / boxArea.toFloat()

        // 4. Outside-frame check
        val outsideRatio = 1f - visibleRatio

        if (outsideRatio > MAX_OUTSIDE_FRAME_RATIO) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: too much outside frame " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "outsideRatio=$outsideRatio, " +
                        "box=$box"
            )
            return null
        }

        // 5. Face area check
        val frameArea =
            bitmap.width.toLong() * bitmap.height.toLong()

        val faceAreaRatio =
            boxArea.toFloat() / frameArea.toFloat()

        if (faceAreaRatio > MAX_FACE_AREA_RATIO) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: face box too large " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "faceAreaRatio=$faceAreaRatio, " +
                        "box=$box"
            )
            return null
        }

        // 6. Visible ratio check
        if (visibleRatio < MIN_VISIBLE_RATIO) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: clipped face " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "visibleRatio=$visibleRatio, " +
                        "box=$box"
            )
            return null
        }

        // 7. Crop once
        val faceBitmap = cropFace(
            bitmap = bitmap,
            detectedFace = detectedFace
        ) ?: return null

        // 8. Resize once
        val resizedFace = faceBitmap.scale(
            MODEL_INPUT_SIZE,
            MODEL_INPUT_SIZE
        )

        // 9. Sharpness
        val sharpness = calculateSharpness(
            resizedFace
        )

        Log.d(
            "EmbeddingQuality",
            "Face sharpness: " +
                    "timestamp=${detectedFace.timestampMs}, " +
                    "sharpness=$sharpness"
        )

        if (sharpness < MIN_SHARPNESS) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: blurry face " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "sharpness=$sharpness, " +
                        "threshold=$MIN_SHARPNESS"
            )
            return null
        }

        Log.d(
            "EmbeddingQuality",
            "Accepted face: " +
                    "timestamp=${detectedFace.timestampMs}, " +
                    "sharpness=$sharpness"
        )

        return PreparedFace(
            bitmap = resizedFace,
            sharpness = sharpness,
            visibleRatio = visibleRatio
        )
    }

    /**
     * Crops the detected face from the original frame.
     */
    private fun cropFace(
        bitmap: Bitmap,
        detectedFace: DetectedFace
    ): Bitmap? {

        val box = detectedFace.boundingBox

        if (bitmap.width <= 0 || bitmap.height <= 0) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: invalid bitmap size " +
                        "${bitmap.width}x${bitmap.height}"
            )
            return null
        }

        val boxWidth = box.width()
        val boxHeight = box.height()

        if (boxWidth <= 0 || boxHeight <= 0) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: invalid face box " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "box=$box"
            )
            return null
        }

        // ---------------------------------------------------------
        // Use the original detected bounding box.
        // No extra crop margin is added.
        // ---------------------------------------------------------

        val left = box.left.coerceIn(
            0,
            bitmap.width - 1
        )

        val top = box.top.coerceIn(
            0,
            bitmap.height - 1
        )

        val right = box.right.coerceIn(
            left + 1,
            bitmap.width
        )

        val bottom = box.bottom.coerceIn(
            top + 1,
            bitmap.height
        )

        if (right <= left || bottom <= top) {
            Log.d(
                "EmbeddingQuality",
                "Rejected: invalid crop " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "box=$box"
            )
            return null
        }

        val safeRect = Rect(
            left,
            top,
            right,
            bottom
        )

        Log.d(
            "EmbeddingQuality",
            "Crop: " +
                    "timestamp=${detectedFace.timestampMs}, " +
                    "original=$box, " +
                    "crop=$safeRect"
        )

        return try {
            Bitmap.createBitmap(
                bitmap,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
            )
        } catch (e: Exception) {

            Log.d(
                "EmbeddingQuality",
                "Rejected: crop failed " +
                        "timestamp=${detectedFace.timestampMs}, " +
                        "box=$safeRect, " +
                        "error=${e.message}"
            )

            null
        }
    }

    /**
     * Calculates sharpness using Laplacian variance.
     *
     * Higher value = more edge detail.
     * Lower value = smoother / blurrier image.
     */
    private fun calculateSharpness(
        bitmap: Bitmap
    ): Double {

        val width = bitmap.width
        val height = bitmap.height

        if (
            width < 3 ||
            height < 3
        ) {
            return 0.0
        }

        val pixels = IntArray(
            width * height
        )

        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        // ---------------------------------------------------------
        // Convert RGB to grayscale
        // ---------------------------------------------------------

        val gray = DoubleArray(
            width * height
        )

        for (i in pixels.indices) {

            val pixel = pixels[i]

            val red =
                (pixel shr 16) and 0xFF

            val green =
                (pixel shr 8) and 0xFF

            val blue =
                pixel and 0xFF

            gray[i] =
                0.299 * red +
                        0.587 * green +
                        0.114 * blue
        }

        // ---------------------------------------------------------
        // Laplacian
        // ---------------------------------------------------------

        var sum = 0.0
        var sumSquared = 0.0
        var count = 0

        for (y in 1 until height - 1) {

            for (x in 1 until width - 1) {

                val center =
                    gray[y * width + x]

                val top =
                    gray[(y - 1) * width + x]

                val bottom =
                    gray[(y + 1) * width + x]

                val left =
                    gray[y * width + (x - 1)]

                val right =
                    gray[y * width + (x + 1)]

                val laplacian =
                    top +
                            bottom +
                            left +
                            right -
                            (4.0 * center)

                sum += laplacian
                sumSquared +=
                    laplacian * laplacian

                count++
            }
        }

        if (count == 0) {
            return 0.0
        }

        // ---------------------------------------------------------
        // Variance
        // ---------------------------------------------------------

        val mean =
            sum / count

        return (sumSquared / count) -
                (mean * mean)
    }

    /**
     * Converts a 160x160 RGB bitmap into FLOAT32 input.
     */
    private fun convertBitmapToBuffer(
        bitmap: Bitmap
    ): ByteBuffer {

        val inputBuffer = ByteBuffer.allocateDirect(
            1 *
                    MODEL_INPUT_SIZE *
                    MODEL_INPUT_SIZE *
                    3 *
                    4
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(
            MODEL_INPUT_SIZE *
                    MODEL_INPUT_SIZE
        )

        bitmap.getPixels(
            pixels,
            0,
            MODEL_INPUT_SIZE,
            0,
            0,
            MODEL_INPUT_SIZE,
            MODEL_INPUT_SIZE
        )

        for (pixel in pixels) {

            val red =
                (pixel shr 16) and 0xFF

            val green =
                (pixel shr 8) and 0xFF

            val blue =
                pixel and 0xFF

            // Normalize [0,255] -> [-1,1]
            inputBuffer.putFloat(
                (red - 127.5f) / 127.5f
            )

            inputBuffer.putFloat(
                (green - 127.5f) / 127.5f
            )

            inputBuffer.putFloat(
                (blue - 127.5f) / 127.5f
            )
        }

        inputBuffer.rewind()

        return inputBuffer
    }

    /**
     * Loads FaceNet model from assets.
     */
    private fun loadModelFile(): MappedByteBuffer {

        val fileDescriptor =
            context.assets.openFd(
                "facenet_512.tflite"
            )

        FileInputStream(
            fileDescriptor.fileDescriptor
        ).use { inputStream ->

            val fileChannel =
                inputStream.channel

            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    /**
     * Logs model input/output information.
     */
    fun inspectModel() {

        val inputTensor =
            interpreter.getInputTensor(0)

        val outputTensor =
            interpreter.getOutputTensor(0)

        Log.d(
            "FaceEmbedding",
            "Input shape: " +
                    inputTensor
                        .shape()
                        .contentToString()
        )

        Log.d(
            "FaceEmbedding",
            "Input type: " +
                    inputTensor.dataType()
        )

        Log.d(
            "FaceEmbedding",
            "Output shape: " +
                    outputTensor
                        .shape()
                        .contentToString()
        )

        Log.d(
            "FaceEmbedding",
            "Output type: " +
                    outputTensor.dataType()
        )
    }


    /**
     * Releases the TensorFlow Lite interpreter.
     */
    fun close() {
        interpreter.close()
    }
}