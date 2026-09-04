package com.amanansari.iykyk.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanansari.iykyk.data.model.ProcessingPhase
import com.amanansari.iykyk.data.model.ProcessingUiState
import com.amanansari.iykyk.data.model.VideoMetadata
import com.amanansari.iykyk.data.repository.ProcessingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    val processingRepository: ProcessingRepository

) : ViewModel() {

    var openDialog by mutableStateOf(false)

    var selectedUri by mutableStateOf<Uri?>(null)
        private set

    var processingUiState by mutableStateOf<ProcessingUiState>(ProcessingUiState())
        private set


    fun updateUri(uri: Uri?){
        this.selectedUri = uri
        openDialog = uri != null
    }

    fun onDialogConfirm() {
        openDialog = false
    }




    /**
     * Starting the Video Processing Steps
     * Phase 1 - Metadata Extraction
     * Phase 2 - Frame Extraction - Every 200ms
     * Phase 3 - Face Detection
     */

    var videoMetadata: VideoMetadata? = null
        private set

    var extractedFrames: List<Bitmap> = emptyList()
        private set


    fun startProcessing(){

        val uri = selectedUri ?: return

        viewModelScope.launch {

            //> Phase 1 - Metadata Extraction
            Log.d("ProcessingViewModel", "Starting metadata extraction")
             processingUiState = ProcessingUiState(
                phase = ProcessingPhase.METADATA_EXTRACTION,
                progress = 0f,
                message = "Reading video metadata...",
                isProcessing = true
            )

            try{
                //> Phase 1 - Metadata Extraction
                Log.d("ProcessingViewModel", "Extracting Metadata $processingUiState" )
                videoMetadata = withContext(Dispatchers.IO) {
                    processingRepository.getVideoMetadata(
                        uri = uri,
                        onProgress = { progress, message ->
                            processingUiState = processingUiState.copy(
                                progress = progress,
                                message = message
                            )

                            Log.d(
                                "ProcessingViewModel",
                                "Progress: $progress, Message: $message"
                            )
                        }
                    )
                }

                processingUiState = ProcessingUiState(
                    phase = ProcessingPhase.METADATA_EXTRACTION,
                    progress = 1f,
                    message = "Video metadata extracted",
                    isProcessing = false
                )

                Log.d(
                    "ProcessingViewModel",
                    "Metadata Progress $processingUiState"
                )

                Log.d("ProcessingViewModel", "Extracted Metadata $videoMetadata" )

                //> Phase 1 - Metadata Extraction Complete

                //> Phase 2 - Frame Extraction

                val metadata = videoMetadata ?: return@launch

                processingUiState = ProcessingUiState(
                    phase = ProcessingPhase.FRAME_EXTRACTION,
                    progress = 0f,
                    message = "Preparing frame extraction...",
                    isProcessing = true
                )

                extractedFrames = withContext(Dispatchers.IO) {
                    processingRepository.extractFrames(
                        uri = uri,
                        durationMs = metadata.durationMs,
                        intervalMs = 200L,
                        onProgress = { progress, message ->

                            processingUiState = processingUiState.copy(
                                progress = progress,
                                message = message
                            )

                            Log.d(
                                "ProcessingViewModel",
                                "Frame Progress: $progress, Message: $message"
                            )
                        }
                    )
                }

                //> Phase 2 - Frame Extraction Complete

                //> Phase 3 - Face Detection
                val detectedFaces = processingRepository.detectFacesInFrames(
                    frames = extractedFrames,
                    intervalMs = 200L,
                    onProgress = { progress, message ->

                        processingUiState = processingUiState.copy(
                            progress = progress,
                            message = message
                        )

                        Log.d(
                            "ProcessingViewModel",
                            "Face Progress: $progress, Message: $message"
                        )
                    }
                )

                Log.d(
                    "FaceDetection",
                    "Total detected faces: ${detectedFaces.size}"
                )

                detectedFaces.forEach { face ->

                    Log.d(
                        "FaceDetection",
                        "Timestamp=${face.timestampMs}, " +
                                "BoundingBox=${face.boundingBox}, " +
                                "TrackingId=${face.trackingId}"
                    )
                }








            }
            catch (e : Exception){
                val failedPhase = processingUiState.phase

                processingUiState = processingUiState.copy(
                    isProcessing = false,
                    error = e.message ?: "An unknown error occurred",
                    message = when (failedPhase) {
                        ProcessingPhase.METADATA_EXTRACTION ->
                            "Failed to extract video metadata"

                        ProcessingPhase.FRAME_EXTRACTION ->
                            "Failed to extract video frames"

                        ProcessingPhase.FACE_DETECTION ->
                            "Failed to detect faces"

                        else ->
                            "Video processing failed"
                    }
                )

                Log.e(
                    "ProcessingViewModel",
                    "Processing failed during $failedPhase",
                    e
                )
            }


            Log.d(
                "ProcessingViewModel",
                "Extracted ${extractedFrames.size} frames"
            )





        }

    }

}