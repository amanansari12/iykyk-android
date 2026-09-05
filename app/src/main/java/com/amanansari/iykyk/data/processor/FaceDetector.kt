package com.amanansari.iykyk.data.processor

import android.graphics.Bitmap
import android.util.Log
import com.amanansari.iykyk.data.model.DetectedFace
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class FaceDetector @Inject constructor(){

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
        )
        .setClassificationMode(
        FaceDetectorOptions.CLASSIFICATION_MODE_ALL
        )
//        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun detectFaces(bitmap : Bitmap, timestampMs : Long) : List<DetectedFace> {


        val image = InputImage.fromBitmap(
            bitmap,
            0
        )

        val faces = detector
            .process(image)
            .await()

        // Remove duplicate / heavily overlapping detections
        val filteredFaces = suppressOverlappingFaces(faces)

        faces.forEachIndexed { index, face ->
            Log.d(
                "RawDetection",
                "timestamp=$timestampMs | " +
                        "index=$index | " +
                        "box=${face.boundingBox}"
            )
        }

        return filteredFaces.map{ face ->
            DetectedFace(
                timestampMs = timestampMs,
                boundingBox = face.boundingBox,
                headEulerAngleX = face.headEulerAngleX,
                headEulerAngleY = face.headEulerAngleY,
                headEulerAngleZ = face.headEulerAngleZ,
                smilingProbability = face.smilingProbability,
                leftEyeOpenProbability = face.leftEyeOpenProbability,
                rightEyeOpenProbability = face.rightEyeOpenProbability,
                trackingId = face.trackingId
            )

        }

    }

    private fun suppressOverlappingFaces(
        faces: List<Face>
    ): List<Face> {

        val keptFaces = mutableListOf<Face>()

        for (face in faces) {

            var isDuplicate = false

            for (keptFace in keptFaces) {

                val currentBox = face.boundingBox
                val keptBox = keptFace.boundingBox

                val intersectionLeft =
                    max(currentBox.left, keptBox.left)

                val intersectionTop =
                    max(currentBox.top, keptBox.top)

                val intersectionRight =
                    min(currentBox.right, keptBox.right)

                val intersectionBottom =
                    min(currentBox.bottom, keptBox.bottom)

                val intersectionWidth =
                    (intersectionRight - intersectionLeft).coerceAtLeast(0)

                val intersectionHeight =
                    (intersectionBottom - intersectionTop).coerceAtLeast(0)

                val intersectionArea =
                    intersectionWidth * intersectionHeight

                val currentArea =
                    currentBox.width() * currentBox.height()

                val keptArea =
                    keptBox.width() * keptBox.height()

                val smallerArea =
                    min(currentArea, keptArea)

                if (smallerArea > 0) {

                    val containmentRatio =
                        intersectionArea.toFloat() / smallerArea.toFloat()

                    if (containmentRatio >= 0.80f) {
                        isDuplicate = true

                        Log.d(
                            "FaceNMS",
                            "Removed duplicate detection, " +
                                    "containment=$containmentRatio"
                        )

                        break
                    }
                }
            }

            if (!isDuplicate) {
                keptFaces.add(face)
            }
        }

        return keptFaces
    }

}
