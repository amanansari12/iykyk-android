package com.amanansari.iykyk.data.processor

import android.graphics.Bitmap
import com.amanansari.iykyk.data.model.DetectedFace
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FaceDetector @Inject constructor(){

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            FaceDetectorOptions.PERFORMANCE_MODE_FAST
        )
        .setClassificationMode(
        FaceDetectorOptions.CLASSIFICATION_MODE_ALL
        )
        .enableTracking()
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

        return faces.map{ face ->
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
}
