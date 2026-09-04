package com.amanansari.iykyk.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import android.net.Uri
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.processor.FaceDetector
import com.amanansari.iykyk.data.processor.FrameExtractor
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import javax.inject.Inject

class ProcessingRepository @Inject constructor(
    private val videoMetadataExtractor: VideoMetadataExtractor,
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector
) {

    //> Video Metadata Extractor
    fun getVideoMetadata(
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): VideoMetadata {
        return videoMetadataExtractor.extractMetadata(uri, onProgress)
    }

    //> Frame Extractor
    fun extractFrames(
        uri: Uri,
        durationMs: Long,
        intervalMs: Long = 200L,
        onProgress: (Float, String) -> Unit
    ): List<Bitmap> {
        return frameExtractor.extractFrames(
            videoUri = uri,
            durationMs = durationMs,
            intervalMs = intervalMs,
            onProgress = onProgress
        )
    }

    //> Face Detection
    suspend fun detectFaces(
        bitmap: Bitmap,
        timestampMs: Long
    ): List<DetectedFace> {
        return faceDetector.detectFaces(bitmap, timestampMs)
    }


    suspend fun detectFacesInFrames(
        frames: List<Bitmap>,
        intervalMs: Long = 200L,
        onProgress: (Float, String) -> Unit
    ): List<DetectedFace> {

        val detectedFaces = mutableListOf<DetectedFace>()

        frames.forEachIndexed { index, frame ->

            val timestampMs = index * intervalMs

            val faces = faceDetector.detectFaces(
                bitmap = frame,
                timestampMs = timestampMs
            )

            detectedFaces.addAll(faces)

            val progress =
                (index + 1).toFloat() / frames.size.toFloat()

            onProgress(
                progress.coerceAtMost(1f),
                "Detecting faces..."
            )
        }

        return detectedFaces
    }
}