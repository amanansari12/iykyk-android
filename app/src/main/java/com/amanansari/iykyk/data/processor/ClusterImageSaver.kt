package com.amanansari.iykyk.data.processor

import android.content.Context
import android.graphics.Bitmap
import com.amanansari.iykyk.data.model.FaceCluster
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ClusterImageSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveClusterSamples(
        frames: List<Bitmap>,
        clusters: List<FaceCluster>,
        intervalMs: Long = 200L
    ) {
        val outputDir = File(context.cacheDir, "cluster_samples")

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        clusters.forEach { cluster ->

            val representative =
                cluster.members.maxByOrNull { it.sharpness }
                    ?: return@forEach

            val frameIndex =
                (representative.detectedFace.timestampMs / intervalMs).toInt()

            val frame = frames.getOrNull(frameIndex)
                ?: return@forEach

            val box = representative.detectedFace.boundingBox

            val left = box.left.coerceIn(0, frame.width)
            val top = box.top.coerceIn(0, frame.height)
            val right = box.right.coerceIn(0, frame.width)
            val bottom = box.bottom.coerceIn(0, frame.height)

            if (right <= left || bottom <= top) {
                return@forEach
            }

            val crop = Bitmap.createBitmap(
                frame,
                left,
                top,
                right - left,
                bottom - top
            )

            val outputFile =
                File(outputDir, "cluster_${cluster.id}.jpg")

            outputFile.outputStream().use { outputStream ->
                crop.compress(
                    Bitmap.CompressFormat.JPEG,
                    95,
                    outputStream
                )
            }

            crop.recycle()
        }
    }
}