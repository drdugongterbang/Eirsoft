package com.dugong.eirsoft.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dugong.eirsoft.data.model.User
import com.dugong.eirsoft.data.repository.AuthRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
    object UpdateSuccess : ProfileState()
    data class LocationSuccess(val address: String) : ProfileState()
}

class ProfileViewModel(private val repository: AuthRepository, private val context: Context) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val updateState: StateFlow<ProfileState> = _updateState.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun fetchProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            repository.getCurrentUser()
                .onSuccess { user -> _profileState.value = ProfileState.Success(user) }
                .onFailure { e -> _profileState.value = ProfileState.Error(e.message ?: "Gagal mengambil profil") }
        }
    }

    fun fetchUserById(userId: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            repository.getUserById(userId)
                .onSuccess { user -> _profileState.value = ProfileState.Success(user) }
                .onFailure { e -> _profileState.value = ProfileState.Error(e.message ?: "Gagal mengambil data member") }
        }
    }

    fun updateProfile(name: String, username: String, address: String) {
        viewModelScope.launch {
            _updateState.value = ProfileState.Loading
            repository.updateProfile(name, username, address)
                .onSuccess { 
                    _updateState.value = ProfileState.UpdateSuccess 
                    fetchProfile() // Refresh data
                }
                .onFailure { e -> _updateState.value = ProfileState.Error(e.message ?: "Gagal update profil") }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _updateState.value = ProfileState.Loading
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                repository.updateProfilePicture(file.absolutePath)
                    .onSuccess {
                        _updateState.value = ProfileState.UpdateSuccess
                        fetchProfile()
                    }
                    .onFailure { e -> _updateState.value = ProfileState.Error(e.message ?: "Gagal simpan foto") }
            } catch (e: Exception) {
                _updateState.value = ProfileState.Error("Error: ${e.message}")
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _updateState.value = ProfileState.Loading
            repository.changePassword(oldPass, newPass)
                .onSuccess { _updateState.value = ProfileState.UpdateSuccess }
                .onFailure { e -> _updateState.value = ProfileState.Error(e.message ?: "Gagal ganti password") }
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                _updateState.value = ProfileState.Loading
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addressString = addresses[0].getAddressLine(0)
                        _updateState.value = ProfileState.LocationSuccess(addressString)
                    } else {
                        _updateState.value = ProfileState.Error("Gagal mendapatkan alamat dari koordinat")
                    }
                } else {
                    _updateState.value = ProfileState.Error("Gagal mendapatkan lokasi GPS")
                }
            } catch (e: Exception) {
                _updateState.value = ProfileState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = ProfileState.Idle
    }
}

class ProfileViewModelFactory(private val repository: AuthRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
