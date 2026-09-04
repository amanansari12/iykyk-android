package com.amanansari.iykyk.data.repository

import android.net.Uri
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.processor.VideoMetadataExtractor
import javax.inject.Inject

class ProcessingRepository @Inject constructor(
    private val videoMetadataExtractor: VideoMetadataExtractor
) {

    fun getVideoMetadata(
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): VideoMetadata {
        return videoMetadataExtractor.extractMetadata(uri, onProgress)
    }
}