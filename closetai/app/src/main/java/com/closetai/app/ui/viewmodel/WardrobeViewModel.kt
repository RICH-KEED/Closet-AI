package com.closetai.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetai.app.data.model.WardrobeItem
import com.closetai.app.data.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WardrobeUiState {
    object Loading : WardrobeUiState()
    data class Success(val items: List<WardrobeItem>) : WardrobeUiState()
    data class Error(val message: String) : WardrobeUiState()
}

class WardrobeViewModel(
    private val wardrobeRepository: WardrobeRepository = WardrobeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WardrobeUiState>(WardrobeUiState.Loading)
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    init {
        fetchWardrobe()
    }

    fun fetchWardrobe() {
        viewModelScope.launch {
            _uiState.value = WardrobeUiState.Loading
            val result = wardrobeRepository.fetchWardrobeItems()
            result.onSuccess { items ->
                _uiState.value = WardrobeUiState.Success(items)
            }.onFailure { error ->
                _uiState.value = WardrobeUiState.Error(error.localizedMessage ?: "Failed to fetch wardrobe")
            }
        }
    }

    fun uploadItem(imageUri: Uri, category: String, color: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = wardrobeRepository.uploadWardrobeItem(imageUri, category, color)
            result.onSuccess {
                fetchWardrobe() // Refresh list on success
                _uploadMessage.value = "Uploaded to wardrobe"
                onComplete(true)
            }.onFailure {
                _uploadMessage.value = it.localizedMessage ?: "Upload failed"
                onComplete(false)
            }
            _isUploading.value = false
        }
    }

    fun consumeUploadMessage() {
        _uploadMessage.value = null
    }
}
