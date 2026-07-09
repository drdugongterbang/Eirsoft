package com.dugong.eirsoft.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dugong.eirsoft.data.model.LoanRequest
import com.dugong.eirsoft.data.repository.LoanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LoanState {
    object Idle : LoanState()
    object Loading : LoanState()
    object Success : LoanState()
    data class Error(val message: String) : LoanState()
}

class LoanViewModel(
    private val repository: LoanRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val allLoanRequests: StateFlow<List<LoanRequest>> = repository.getAllLoanRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<LoanState>(LoanState.Idle)
    val uiState: StateFlow<LoanState> = _uiState.asStateFlow()

    private val _userLoans = MutableStateFlow<List<LoanRequest>>(emptyList())
    val userLoans: StateFlow<List<LoanRequest>> = _userLoans.asStateFlow()
    
    private var fetchJob: Job? = null

    fun fetchUserLoans(userId: String) {
        if (userId.isEmpty()) return
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            repository.getLoanRequestsByUser(userId).collect {
                _userLoans.value = it
            }
        }
    }

    fun createLoanRequest(loan: LoanRequest) {
        viewModelScope.launch {
            _uiState.value = LoanState.Loading
            val result = repository.createLoanRequest(loan)
            result.onSuccess {
                _uiState.value = LoanState.Success
            }.onFailure {
                _uiState.value = LoanState.Error(it.message ?: "Gagal mengajukan peminjaman")
            }
        }
    }

    fun approveLoan(loan: LoanRequest, adminId: String, adminName: String) {
        viewModelScope.launch {
            _uiState.value = LoanState.Loading
            val result = repository.approveLoan(loan, adminId, adminName)
            result.onSuccess {
                _uiState.value = LoanState.Success
            }.onFailure {
                _uiState.value = LoanState.Error(it.message ?: "Gagal menyetujui peminjaman")
            }
        }
    }

    fun rejectLoan(loanId: String, note: String, adminId: String, adminName: String) {
        viewModelScope.launch {
            _uiState.value = LoanState.Loading
            val result = repository.rejectLoan(loanId, note, adminId, adminName)
            result.onSuccess {
                _uiState.value = LoanState.Success
            }.onFailure {
                _uiState.value = LoanState.Error(it.message ?: "Gagal menolak peminjaman")
            }
        }
    }

    fun markAsReturned(loan: LoanRequest, adminId: String, adminName: String) {
        viewModelScope.launch {
            _uiState.value = LoanState.Loading
            val result = repository.markAsReturned(loan, adminId, adminName)
            result.onSuccess {
                _uiState.value = LoanState.Success
            }.onFailure {
                _uiState.value = LoanState.Error(it.message ?: "Gagal memproses pengembalian")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoanState.Idle
    }
}

class LoanViewModelFactory(private val repository: LoanRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            return LoanViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            return LoanViewModel(repository, SavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
