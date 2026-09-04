package com.amanansari.iykyk.data.processor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.amanansari.iykyk.data.model.VideoMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class VideoMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractMetadata(
        videoUri: Uri,
        onProgress: (Float, String) ->Unit
    ): VideoMetadata {
        val retriever = MediaMetadataRetriever()

        return try{

            onProgress(0.1f, "Opening video...")

            retriever.setDataSource(context, videoUri)

            onProgress(0.25f, "Reading video duration...")

            val durationMs =
                retriever
                    .extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                    ?.toLongOrNull()
                    ?: 0L

            onProgress(0.45f, "Reading video dimensions...")
            val width =
                retriever
                    .extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )
                    ?.toIntOrNull()
                    ?: 0

            val height =
                retriever
                    .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )
                    ?.toIntOrNull()
                    ?: 0

            onProgress(0.65f, "Reading video rotation...")
            val rotation =
                retriever
                    .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )
                    ?.toIntOrNull()
                    ?: 0

            onProgress(0.85f, "Reading video frame rate...")
            val frameRate =
                retriever
                    .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
                    )
                    ?.toFloatOrNull()

            onProgress(1f, "Video metadata extracted")
            VideoMetadata(
                durationMs = durationMs,
                width = width,
                height = height,
                rotation = rotation,
                frameRate = frameRate
            )
        } finally {
            retriever.release()
        }
    }


}