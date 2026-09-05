package com.amanansari.iykyk.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import android.net.Uri
import android.util.Log
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.processor.FaceDetector
import com.amanansari.iykyk.data.processor.FaceEmbedding
import com.amanansari.iykyk.data.processor.FrameExtractor
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import javax.inject.Inject

class ProcessingRepository @Inject constructor(
    private val videoMetadataExtractor: VideoMetadataExtractor,
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedding: FaceEmbedding
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

    fun inspectEmbeddingModel() {
        faceEmbedding.inspectModel()
    }

    fun testEmbedding(
        bitmap: Bitmap,
        detectedFace: DetectedFace
    ) {
        val embedding = faceEmbedding.generateEmbedding(
            bitmap,
            detectedFace
        )

        Log.d(
            "FaceEmbedding",
            "Embedding size: ${embedding.size}"
        )

        Log.d(
            "FaceEmbedding",
            "First 5 values: ${embedding.take(5)}"
        )
    }


    //> Test Cosine Similarity

    fun testCosineSimilarity(
        bitmap1: Bitmap,
        face1: DetectedFace,
        bitmap2: Bitmap,
        face2: DetectedFace
    ) {

        val embedding1 = faceEmbedding.generateEmbedding(
            bitmap = bitmap1,
            detectedFace = face1
        )

        val embedding2 = faceEmbedding.generateEmbedding(
            bitmap = bitmap2,
            detectedFace = face2
        )

        val similarity = faceEmbedding.cosineSimilarity(
            embedding1,
            embedding2
        )

        Log.d(
            "FaceSimilarity",
            "Cosine similarity: $similarity"
        )
    }

    fun testMultipleSimilarities(
        frames: List<Bitmap>,
        detectedFaces: List<DetectedFace>,
        intervalMs: Long = 200L
    ) {
        val facesWithTracking = detectedFaces
            .filter { it.trackingId != null }

        // -----------------------------
        // SAME PERSON TESTS
        // -----------------------------

        val samePersonGroups = facesWithTracking
            .groupBy { it.trackingId }
            .values
            .filter { it.size >= 2 }

        samePersonGroups
            .take(5)
            .forEachIndexed { groupIndex, faces ->

                val face1 = faces[0]
                val face2 = faces[1]

                val frame1Index =
                    (face1.timestampMs / intervalMs).toInt()

                val frame2Index =
                    (face2.timestampMs / intervalMs).toInt()

                val frame1 = frames.getOrNull(frame1Index)
                val frame2 = frames.getOrNull(frame2Index)

                if (frame1 != null && frame2 != null) {

                    val embedding1 = faceEmbedding.generateEmbedding(
                        bitmap = frame1,
                        detectedFace = face1
                    )

                    val embedding2 = faceEmbedding.generateEmbedding(
                        bitmap = frame2,
                        detectedFace = face2
                    )

                    val similarity = faceEmbedding.cosineSimilarity(
                        embedding1 = embedding1,
                        embedding2 = embedding2
                    )

                    Log.d(
                        "SimilarityTest",
                        "SAME[$groupIndex] " +
                                "trackingId=${face1.trackingId}, " +
                                "timestamps=${face1.timestampMs},${face2.timestampMs}, " +
                                "similarity=$similarity"
                    )
                }
            }

        // -----------------------------
        // DIFFERENT PERSON TESTS
        // -----------------------------

        val multiFaceFrames = facesWithTracking
            .groupBy { it.timestampMs }
            .values
            .filter { faces ->
                faces.size >= 2
            }

        multiFaceFrames
            .take(5)
            .forEachIndexed { groupIndex, faces ->

                val face1 = faces[0]
                val face2 = faces[1]

                if (face1.trackingId == face2.trackingId) {
                    return@forEachIndexed
                }

                val frameIndex =
                    (face1.timestampMs / intervalMs).toInt()

                val frame = frames.getOrNull(frameIndex)

                if (frame != null) {

                    val embedding1 = faceEmbedding.generateEmbedding(
                        bitmap = frame,
                        detectedFace = face1
                    )

                    val embedding2 = faceEmbedding.generateEmbedding(
                        bitmap = frame,
                        detectedFace = face2
                    )

                    val similarity = faceEmbedding.cosineSimilarity(
                        embedding1 = embedding1,
                        embedding2 = embedding2
                    )

                    Log.d(
                        "SimilarityTest",
                        "DIFFERENT[$groupIndex] " +
                                "trackingIds=${face1.trackingId},${face2.trackingId}, " +
                                "timestamp=${face1.timestampMs}, " +
                                "similarity=$similarity"
                    )
                }
            }
    }


    fun collectSimilaritySamples(
        frames: List<Bitmap>,
        detectedFaces: List<DetectedFace>,
        intervalMs: Long = 200L,
        maxSamePairs: Int = 40,
        maxDifferentPairs: Int = 40
    ) {

        val facesWithTracking = detectedFaces
            .filter { it.trackingId != null }

        val samePersonSimilarities = mutableListOf<Float>()
        val differentPersonSimilarities = mutableListOf<Float>()

        // -----------------------------------------
        // SAME PERSON
        // -----------------------------------------

        val samePersonGroups = facesWithTracking
            .groupBy { it.trackingId }
            .values

        for (faces in samePersonGroups) {

            if (samePersonSimilarities.size >= maxSamePairs) {
                break
            }

            val sortedFaces = faces.sortedBy { it.timestampMs }

            // Compare faces that are separated by at least 2 samples
            for (i in sortedFaces.indices) {

                if (i + 2 >= sortedFaces.size) {
                    continue
                }

                val face1 = sortedFaces[i]
                val face2 = sortedFaces[i + 2]

                val frame1Index =
                    (face1.timestampMs / intervalMs).toInt()

                val frame2Index =
                    (face2.timestampMs / intervalMs).toInt()

                val frame1 = frames.getOrNull(frame1Index)
                val frame2 = frames.getOrNull(frame2Index)

                if (frame1 == null || frame2 == null) {
                    continue
                }

                if (!faceEmbedding.isUsableForEmbedding(frame1, face1)) {
                    continue
                }

                if (!faceEmbedding.isUsableForEmbedding(frame2, face2)) {
                    continue
                }


                val embedding1 = faceEmbedding.generateEmbedding(
                    bitmap = frame1,
                    detectedFace = face1
                )

                val embedding2 = faceEmbedding.generateEmbedding(
                    bitmap = frame2,
                    detectedFace = face2
                )

                val similarity = faceEmbedding.cosineSimilarity(
                    embedding1,
                    embedding2
                )

                samePersonSimilarities.add(similarity)

                Log.d(
                    "SimilaritySample",
                    "SAME " +
                            "trackingId=${face1.trackingId} " +
                            "${face1.timestampMs}ms -> ${face2.timestampMs}ms " +
                            "similarity=$similarity"
                )
            }
        }

        // -----------------------------------------
        // DIFFERENT PEOPLE
        // -----------------------------------------

        val multiFaceFrames = facesWithTracking
            .groupBy { it.timestampMs }
            .values

        for (faces in multiFaceFrames) {

            if (differentPersonSimilarities.size >= maxDifferentPairs) {
                break
            }

            if (faces.size < 2) {
                continue
            }

            for (i in 0 until faces.lastIndex) {

                for (j in i + 1 until faces.size) {

                    if (
                        faces[i].trackingId ==
                        faces[j].trackingId
                    ) {
                        continue
                    }

                    val face1 = faces[i]
                    val face2 = faces[j]

                    val frameIndex =
                        (face1.timestampMs / intervalMs).toInt()

                    val frame = frames.getOrNull(frameIndex)
                        ?: continue

                    if (!faceEmbedding.isUsableForEmbedding(frame, face1)) {
                        continue
                    }

                    if (!faceEmbedding.isUsableForEmbedding(frame, face2)) {
                        continue
                    }

                    val embedding1 = faceEmbedding.generateEmbedding(
                        bitmap = frame,
                        detectedFace = face1
                    )

                    val embedding2 = faceEmbedding.generateEmbedding(
                        bitmap = frame,
                        detectedFace = face2
                    )

                    val similarity = faceEmbedding.cosineSimilarity(
                        embedding1,
                        embedding2
                    )

                    differentPersonSimilarities.add(similarity)

                    Log.d(
                        "SimilaritySample",
                        "DIFFERENT " +
                                "trackingIds=${face1.trackingId},${face2.trackingId} " +
                                "timestamp=${face1.timestampMs}ms " +
                                "similarity=$similarity"
                    )

                    if (
                        differentPersonSimilarities.size >=
                        maxDifferentPairs
                    ) {
                        break
                    }
                }

                if (
                    differentPersonSimilarities.size >=
                    maxDifferentPairs
                ) {
                    break
                }
            }
        }

        // -----------------------------------------
        // SUMMARY
        // -----------------------------------------

        if (samePersonSimilarities.isNotEmpty()) {

            Log.d(
                "SimilaritySummary",
                "Same-person count=${samePersonSimilarities.size}, " +
                        "min=${samePersonSimilarities.minOrNull()}, " +
                        "max=${samePersonSimilarities.maxOrNull()}, " +
                        "avg=${samePersonSimilarities.average()}"
            )
        }

        if (differentPersonSimilarities.isNotEmpty()) {

            Log.d(
                "SimilaritySummary",
                "Different-person count=${differentPersonSimilarities.size}, " +
                        "min=${differentPersonSimilarities.minOrNull()}, " +
                        "max=${differentPersonSimilarities.maxOrNull()}, " +
                        "avg=${differentPersonSimilarities.average()}"
            )
        }
    }




}