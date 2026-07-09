package com.dugong.eirsoft.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dugong.eirsoft.data.model.Property
import com.dugong.eirsoft.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PropertyState {
    object Idle : PropertyState()
    object Loading : PropertyState()
    object Success : PropertyState()
    data class Error(val message: String) : PropertyState()
}

enum class SortOrder {
    NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, STOCK_ASC, STOCK_DESC
}

class PropertyViewModel(
    private val repository: PropertyRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val properties: StateFlow<List<Property>> = repository.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<PropertyState>(PropertyState.Idle)
    val uiState: StateFlow<PropertyState> = _uiState.asStateFlow()

    fun addProperty(property: Property, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.value = PropertyState.Loading
            val result = repository.addProperty(property, imageUri)
            result.onSuccess {
                _uiState.value = PropertyState.Success
            }.onFailure {
                _uiState.value = PropertyState.Error(it.message ?: "Gagal menambah properti")
            }
        }
    }

    fun updateProperty(property: Property, newImageUri: Uri?) {
        viewModelScope.launch {
            _uiState.value = PropertyState.Loading
            val result = repository.updateProperty(property, newImageUri)
            result.onSuccess {
                _uiState.value = PropertyState.Success
            }.onFailure {
                _uiState.value = PropertyState.Error(it.message ?: "Gagal update properti")
            }
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            _uiState.value = PropertyState.Loading
            val result = repository.deleteProperty(property)
            result.onSuccess {
                _uiState.value = PropertyState.Success
            }.onFailure {
                _uiState.value = PropertyState.Error(it.message ?: "Gagal menghapus properti")
            }
        }
    }

    fun resetState() {
        _uiState.value = PropertyState.Idle
    }

    fun sortProperties(list: List<Property>, order: SortOrder): List<Property> {
        return when (order) {
            SortOrder.NAME_ASC -> list.sortedBy { it.name }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.name }
            SortOrder.PRICE_ASC -> list.sortedBy { it.rentPrice }
            SortOrder.PRICE_DESC -> list.sortedByDescending { it.rentPrice }
            SortOrder.STOCK_ASC -> list.sortedBy { it.availableStock }
            SortOrder.STOCK_DESC -> list.sortedByDescending { it.availableStock }
        }
    }
}

class PropertyViewModelFactory(private val repository: PropertyRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            return PropertyViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            return PropertyViewModel(repository, SavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
