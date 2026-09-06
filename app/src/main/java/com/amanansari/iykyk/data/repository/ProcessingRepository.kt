package com.amanansari.iykyk.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import android.net.Uri
import android.util.Log
import com.amanansari.iykyk.data.model.ClusteringResult
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.FaceCluster
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import com.amanansari.iykyk.data.model.PersonResult
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.processor.AppearanceCounter
import com.amanansari.iykyk.data.processor.BestShotSelector
import com.amanansari.iykyk.data.processor.ClusterImageSaver
import com.amanansari.iykyk.data.processor.FaceClusterer
import com.amanansari.iykyk.data.processor.FaceDetector
import com.amanansari.iykyk.data.processor.FaceEmbedding
import com.amanansari.iykyk.data.processor.FrameExtractor
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.inject.Inject

class ProcessingRepository @Inject constructor(
    private val videoMetadataExtractor: VideoMetadataExtractor,
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedding: FaceEmbedding,
    private val faceClusterer: FaceClusterer,
    private val clusterImageSaver: ClusterImageSaver,
    private val appearanceCounter: AppearanceCounter,
    private val bestShotSelector: BestShotSelector
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
        val frames = frameExtractor.extractFrames(
            videoUri = uri,
            durationMs = durationMs,
            intervalMs = intervalMs,
            onProgress = onProgress
        )

        // TEMPORARY DEBUG
        Log.d(
            "FrameHash",
            "timestamp=200 hash=${bitmapHash(frames[1])}"
        )

        Log.d(
            "FrameHash",
            "timestamp=13400 hash=${bitmapHash(frames[67])}"
        )

        return frames
    }


    //! only temporary
    private fun bitmapHash(bitmap: Bitmap): String {
        val buffer = ByteBuffer.allocate(bitmap.allocationByteCount)

        bitmap.copyPixelsToBuffer(buffer)

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(buffer.array())

        return digest.joinToString("") { "%02x".format(it) }
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

    fun clusterFaces(
        frames: List<Bitmap>,
        embeddingResults: List<FaceEmbeddingResult>
    ): List<FaceCluster> {


        val orderedResults = embeddingResults.sortedWith(
            compareBy(
                { it.detectedFace.timestampMs },
                { it.detectedFace.boundingBox.top },
                { it.detectedFace.boundingBox.left }
            )
        )

        val clusters = faceClusterer.cluster(orderedResults)

        faceClusterer.logMemberSimilarities(clusters)
        faceClusterer.logClusterTimeline(clusters)
        faceClusterer.logClusterBoundingBoxes(clusters)

        clusterImageSaver.saveClusterSamples(
            frames = frames,
            clusters = clusters
        )

        return clusters

    }

    //> Appearance Counting

    fun countAppearances(
        clusters: List<FaceCluster>
    ): Map<Int, Int> {

        return clusters.associate { cluster ->
            cluster.id to appearanceCounter.countAppearances(cluster)
        }
    }

    //> Building PersonResult

    fun buildPersonResults(
        frames: List<Bitmap>,
        clusters: List<FaceCluster>,
        appearanceCounts: Map<Int, Int>,
        intervalMs: Long = 200L
    ): List<PersonResult> = clusters.mapNotNull { cluster ->
        bestShotSelector.buildPersonResult(
            cluster = cluster,
            frames = frames,
            appearanceCount = appearanceCounts[cluster.id] ?: 0,
            intervalMs = intervalMs
        )
    }


    fun saveSelectedFaces(personResults: List<PersonResult>) {
        clusterImageSaver.saveSelectedFaces(personResults)
    }
}