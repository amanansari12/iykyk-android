package com.amanansari.iykyk.ui.viewmodel

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
    }

    fun onDialogConfirm() {
        openDialog = false
    }




    var videoMetadata: VideoMetadata? = null
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
            }
            catch (e : Exception){
                processingUiState = ProcessingUiState(
                    phase = ProcessingPhase.METADATA_EXTRACTION,
                    progress = 0f,
                    message = "Failed to extract video metadata",
                    isProcessing = false,
                    error = e.message
                )
            }

            //> Phase 1 - Metadata Extraction Complete

            //> Phase 2 - Frame Extraction



        }

    }

}