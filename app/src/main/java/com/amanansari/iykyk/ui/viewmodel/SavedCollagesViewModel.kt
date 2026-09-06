package com.amanansari.iykyk.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanansari.iykyk.data.local.SavedCollageEntity
import com.amanansari.iykyk.data.repository.SavedCollageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedCollagesViewModel @Inject constructor(
    private val savedCollageRepository: SavedCollageRepository
) : ViewModel() {

    val savedCollages: StateFlow<List<SavedCollageEntity>> =
        savedCollageRepository.observeSavedCollages()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteCollage(collage: SavedCollageEntity) {
        viewModelScope.launch {
            savedCollageRepository.deleteCollage(collage)
        }
    }

    fun getShareableUri(collage: SavedCollageEntity): Uri? {
        return savedCollageRepository.getShareableUri(collage)
    }
}
