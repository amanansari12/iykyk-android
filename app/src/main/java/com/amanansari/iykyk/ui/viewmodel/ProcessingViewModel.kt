package com.amanansari.iykyk.ui.viewmodel

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanansari.iykyk.data.model.FaceCluster
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import com.amanansari.iykyk.data.model.PersonResult
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

    var embeddingResults: List<FaceEmbeddingResult> = emptyList()
        private set

    var faceClusters: List<FaceCluster> = emptyList()
        private set

    var appearanceCounts: Map<Int, Int> = emptyMap()
        private set

    var personResults: List<PersonResult> = emptyList()
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

                Log.d(
                    "ProcessingViewModel",
                    "Extracted ${extractedFrames.size} frames"
                )


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

                //> Phase 3 - Face Detection Completed

                //> Phase 4 - Face Embedding

                processingUiState = ProcessingUiState(
                    phase = ProcessingPhase.FACE_EMBEDDING,
                    progress = 0f,
                    message = "Generating face embeddings...",
                    isProcessing = true
                )

                Log.d(
                    "ProcessingViewModel",
                    "Starting face embedding generation"
                )

                embeddingResults = withContext(Dispatchers.IO) {
                    processingRepository.generateEmbeddings(
                        frames = extractedFrames,
                        detectedFaces = detectedFaces,
                        intervalMs = 200L,
                        onProgress = { progress, message ->

                            processingUiState = processingUiState.copy(
                                progress = progress,
                                message = message
                            )

                            Log.d(
                                "ProcessingViewModel",
                                "Embedding Progress: $progress, Message: $message"
                            )
                        }
                    )
                }

                Log.d(
                    "ProcessingViewModel",
                    "Generated ${embeddingResults.size} face embeddings"
                )

                processingUiState = ProcessingUiState(
                    phase = ProcessingPhase.FACE_EMBEDDING,
                    progress = 1f,
                    message = "Face embeddings generated",
                    isProcessing = false
                )

                //> Phase 4 - Face Embedding Ending

                //> Phase 5 - Face Clustering (Making Clusters from Embeddings)

                Log.d("ProcessingViewModel", "Starting face clustering")

//                faceClusters = withContext(Dispatchers.Default) {
//                    processingRepository.clusterFaces(embeddingResults)
//                }

                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.CLUSTERING,
                    progress = 0f,
                    message = "Clustering faces..."
                )

                val clusters = withContext(Dispatchers.Default) {
                    processingRepository.clusterFaces(
                        frames = extractedFrames,
                        embeddingResults = embeddingResults
                    )
                }

                faceClusters = clusters

                Log.d(
                    "ProcessingViewModel",
                    "Generated ${faceClusters.size} face clusters"
                )

                faceClusters.forEach { cluster ->
                    Log.d(
                        "ProcessingViewModel",
                        "Cluster ${cluster.id}: ${cluster.members.size} members"
                    )
                }


                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.CLUSTERING,
                    progress = 1f,
                    message = "Face clustering completed"
                )


                //> Phase 5 - Face Clustering Completed

                //> Phase 6 - Appearance Counting

                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.APPEARANCE_COUNTING,
                    progress = 0f,
                    message = "Counting appearances..."
                )

                appearanceCounts = withContext(Dispatchers.Default) {
                    processingRepository.countAppearances(clusters)
                }


                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.APPEARANCE_COUNTING,
                    progress = 1f,
                    message = "Appearance counting completed"
                )

                //> Phase 6 - Appearance Counting Completed

                //> Phase 7 - Best Shot Selection

                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.BEST_SHOT_SELECTION,
                    progress = 0f,
                    message = "Selecting best shots..."
                )

                Log.d("ProcessingViewModel", "Starting best shot selection")

                val results = withContext(Dispatchers.Default) {
                    processingRepository.buildPersonResults(
                        frames = extractedFrames,
                        clusters = clusters,
                        appearanceCounts = appearanceCounts
                    )
                }

                personResults = results

                Log.d(
                    "ProcessingViewModel",
                    "Selected ${personResults.size} representative faces"
                )

                personResults.forEach { person ->
                    Log.d(
                        "ProcessingViewModel",
                        "Cluster ${person.clusterId}: appearances=${person.appearanceCount}"
                    )
                }

                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.BEST_SHOT_SELECTION,
                    progress = 1f,
                    message = "Best shots selected"
                )

                //> Phase 7 - Best Shot Selection Completed

                Log.d(
                    "ProcessingViewModel",
                    "Selected ${personResults.size} representative faces"
                )

                personResults.forEach { person ->
                    Log.d(
                        "ProcessingViewModel",
                        "Cluster ${person.clusterId}: appearances=${person.appearanceCount}"
                    )
                }

                withContext(Dispatchers.IO) {
                    processingRepository.saveSelectedFaces(personResults)
                }

                processingUiState = processingUiState.copy(
                    phase = ProcessingPhase.BEST_SHOT_SELECTION,
                    progress = 1f,
                    message = "Best shots selected"
                )


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

                        ProcessingPhase.FACE_EMBEDDING ->
                            "Failed to generate face embeddings"

                        ProcessingPhase.CLUSTERING ->
                            "Failed to cluster faces"

                        ProcessingPhase.APPEARANCE_COUNTING ->
                            "Failed to count appearances"

                        ProcessingPhase.BEST_SHOT_SELECTION ->
                            "Failed to select best shots"

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







        }

    }

}