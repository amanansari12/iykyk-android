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
import com.amanansari.iykyk.data.processor.CollageGenerator
import com.amanansari.iykyk.data.processor.FaceClusterer
import com.amanansari.iykyk.data.processor.FaceDetector
import com.amanansari.iykyk.data.processor.FaceEmbedding
import com.amanansari.iykyk.data.processor.FrameExtractor
import com.amanansari.iykyk.data.processor.MediaExporter
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val bestShotSelector: BestShotSelector,
    private val collageGenerator: CollageGenerator,
    private val mediaExporter: MediaExporter
) {

    //> Video Metadata Extractor
    fun getVideoMetadata(
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): VideoMetadata {
        return videoMetadataExtractor.extractMetadata(uri, onProgress)
    }

    //> Frame Extractor
     suspend fun extractFrames(
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

        return frames
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

        for ((index, frame) in frames.withIndex()) {

            currentCoroutineContext().ensureActive()

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

    suspend fun generateEmbeddings(
        frames: List<Bitmap>,
        detectedFaces: List<DetectedFace>,
        intervalMs: Long = 200L,
        onProgress: (Float, String) -> Unit
    ): List<FaceEmbeddingResult> {

        val results = mutableListOf<FaceEmbeddingResult>()

        for ((index, detectedFace) in detectedFaces.withIndex()) {

            currentCoroutineContext().ensureActive()

            val frameIndex =
                (detectedFace.timestampMs / intervalMs).toInt()

            val frame = frames.getOrNull(frameIndex) ?: continue

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


    //> Save Selected Images to Cache
    fun saveSelectedFaces(personResults: List<PersonResult>) {
        clusterImageSaver.saveSelectedFaces(personResults)
    }

    //> Collage Generation

    fun generateCollage(personResults: List<PersonResult>): Bitmap {
        return collageGenerator.generateCollage(personResults)
    }

    //> Export - Save & Share

    fun saveCollageToGallery(bitmap: Bitmap): android.net.Uri? {
        return mediaExporter.saveToGallery(bitmap)
    }

    fun getShareableCollageUri(bitmap: Bitmap): android.net.Uri? {
        return mediaExporter.getShareableUri(bitmap)
    }
}