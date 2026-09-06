package com.amanansari.iykyk.data.processor

import android.graphics.Bitmap
import android.graphics.Rect
import com.amanansari.iykyk.data.model.DetectedFace
import com.amanansari.iykyk.data.model.FaceCluster
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import com.amanansari.iykyk.data.model.PersonResult
import javax.inject.Inject
import kotlin.math.abs
class BestShotSelector @Inject constructor() {


    fun buildPersonResult(
        cluster: FaceCluster,
        frames: List<Bitmap>,
        appearanceCount: Int,
        intervalMs: Long = 200L
    ): PersonResult? {
        val bestShot = selectBestShot(cluster) ?: return null

        val frameIndex = (bestShot.detectedFace.timestampMs / intervalMs).toInt()
        val frame = frames.getOrNull(frameIndex) ?: return null

        val representativeFace = cropGenerously(frame, bestShot.detectedFace.boundingBox)

        return PersonResult(
            clusterId = cluster.id,
            appearanceCount = appearanceCount,
            representativeFace = representativeFace
        )
    }

    fun selectBestShot(cluster: FaceCluster): FaceEmbeddingResult? {
        if (cluster.members.isEmpty()) return null

        val visible = cluster.members.filter { it.visibleRatio >= MIN_VISIBLE_RATIO }
        val candidates = visible.ifEmpty { cluster.members } // fallback, never empty

        val maxSharpness = candidates.maxOf { it.sharpness }.takeIf { it > 0 } ?: 1.0

        return candidates.maxByOrNull { member ->
            val frontality = frontalityScore(member.detectedFace)
            val sharpness = member.sharpness / maxSharpness
            val eyesOpen = eyesOpenScore(member.detectedFace)
            val smiling = (member.detectedFace.smilingProbability ?: 0.5f).toDouble()

            FRONTALITY_WEIGHT * frontality +
                    SHARPNESS_WEIGHT * sharpness +
                    EYES_OPEN_WEIGHT * eyesOpen +
                    SMILING_WEIGHT * smiling
        }
    }

    private fun frontalityScore(face: DetectedFace): Double {
        val yaw = (1 - abs(face.headEulerAngleY) / MAX_YAW_DEG).coerceIn(0f, 1f)
        val pitch = (1 - abs(face.headEulerAngleX) / MAX_PITCH_DEG).coerceIn(0f, 1f)
        return (yaw * 0.7f + pitch * 0.3f).toDouble()
    }

    private fun eyesOpenScore(face: DetectedFace): Double {
        val left = face.leftEyeOpenProbability ?: 0.5f
        val right = face.rightEyeOpenProbability ?: 0.5f
        return ((left + right) / 2f).toDouble()
    }


    private fun cropGenerously(frame: Bitmap, box: Rect, expansionFactor: Float = 2.5f): Bitmap {
        val expandedW = (box.width() * expansionFactor).toInt()
        val expandedH = (box.height() * expansionFactor).toInt()
        val cx = box.centerX()
        val cy = box.centerY()

        val left = (cx - expandedW / 2).coerceIn(0, frame.width)
        val top = (cy - expandedH / 2).coerceIn(0, frame.height)
        val right = (cx + expandedW / 2).coerceIn(0, frame.width)
        val bottom = (cy + expandedH / 2).coerceIn(0, frame.height)

        return Bitmap.createBitmap(frame, left, top, right - left, bottom - top)
    }

    companion object {
        private const val MIN_VISIBLE_RATIO = 0.6f
        private const val MAX_YAW_DEG = 45f
        private const val MAX_PITCH_DEG = 30f
        private const val FRONTALITY_WEIGHT = 0.35
        private const val SHARPNESS_WEIGHT = 0.30
        private const val EYES_OPEN_WEIGHT = 0.25
        private const val SMILING_WEIGHT = 0.10
    }
}