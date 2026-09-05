package com.amanansari.iykyk.data.processor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
)
{

    fun extractFrames(
        videoUri: Uri,
        durationMs: Long,
        intervalMs: Long = 200L,
        onProgress: (Float, String) -> Unit
    ): List<Bitmap>{

        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()


        return try{

            retriever.setDataSource(context, videoUri)
            var timestampMs = 0L

            while(timestampMs < durationMs){

                val frame = retriever.getFrameAtTime(
                    timestampMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (frame != null) {
                    frames.add(frame)
                }

                timestampMs += intervalMs

                val progress =
                    timestampMs.toFloat() / durationMs.toFloat()

                onProgress(
                    progress.coerceAtMost(1f),
                    "Extracting frames..."
                )
            }

            onProgress(
                1f,
                "Frame extraction completed"
            )

            frames
        }
        finally {
            retriever.release()
        }

    }


}