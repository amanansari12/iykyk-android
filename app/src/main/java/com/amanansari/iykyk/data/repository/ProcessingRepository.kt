package com.amanansari.iykyk.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import android.net.Uri
import android.util.Log
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.FaceCluster
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.processor.ClusterImageSaver
import com.amanansari.iykyk.data.processor.FaceClusterer
import com.amanansari.iykyk.data.processor.FaceDetector
import com.amanansari.iykyk.data.processor.FaceEmbedding
import com.amanansari.iykyk.data.processor.FrameExtractor
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import javax.inject.Inject

class ProcessingRepository @Inject constructor(
    private val videoMetadataExtractor: VideoMetadataExtractor,
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedding: FaceEmbedding,
    private val faceClusterer: FaceClusterer,
    private val clusterImageSaver: ClusterImageSaver
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

    //> step 4 - Face Embedding

    fun generateEmbeddings(
        frames: List<Bitmap>,
        detectedFaces: List<DetectedFace>,
        intervalMs: Long = 200L,
        onProgress: (Float, String) -> Unit
    ): List<FaceEmbeddingResult> {

        val results = mutableListOf<FaceEmbeddingResult>()

        detectedFaces.forEachIndexed { index, detectedFace ->

            val frameIndex =
                (detectedFace.timestampMs / intervalMs).toInt()

            val frame = frames.getOrNull(frameIndex) ?: return@forEachIndexed

            val embeddingResult = faceEmbedding.generateEmbedding(
                bitmap = frame,
                detectedFace = detectedFace
            )

            if (embeddingResult != null) {
                results.add(embeddingResult)
            }

            val progress =
                (index + 1).toFloat() / detectedFaces.size.toFloat()

            onProgress(
                progress.coerceAtMost(1f),
                "Generating face embeddings..."
            )
        }

        return results
    }


    //> Face Clustering

//    fun clusterFaces(
//        embeddingResults: List<FaceEmbeddingResult>
//    ): List<FaceCluster> {
//        return faceClusterer.cluster(embeddingResults)
//    }


    //> Save Face Clusters

    fun clusterFaces(
        frames: List<Bitmap>,
        embeddingResults: List<FaceEmbeddingResult>
    ): List<FaceCluster> {

        val clusters = faceClusterer.cluster(embeddingResults)

        // Temporary debugging only
        clusterImageSaver.saveClusterSamples(
            frames = frames,
            clusters = clusters
        )

        return clusters
    }
}