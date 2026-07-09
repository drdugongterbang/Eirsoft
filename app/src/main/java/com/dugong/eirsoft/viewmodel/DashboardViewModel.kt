package com.dugong.eirsoft.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.data.repository.LoanRepository
import com.dugong.eirsoft.data.repository.PropertyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DashboardStats(
    val totalProperties: Int = 0,
    val pendingLoans: Int = 0,
    val activeLoans: Int = 0,
    val estimatedIncomeMonth: Long = 0L
)

class DashboardViewModel(
    private val propertyRepository: PropertyRepository,
    private val loanRepository: LoanRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val stats: StateFlow<DashboardStats> = combine(
        propertyRepository.getAllProperties(),
        loanRepository.getAllLoanRequests()
    ) { properties, loans ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val income = loans.filter { 
            it.status == LoanStatus.APPROVED || it.status == LoanStatus.RETURNED 
        }.filter {
            val loanCal = Calendar.getInstance()
            loanCal.timeInMillis = it.requestDate
            loanCal.get(Calendar.MONTH) == currentMonth && loanCal.get(Calendar.YEAR) == currentYear
        }.sumOf { loan ->
            val property = properties.find { it.id == loan.propertyId }
            (property?.rentPrice ?: 0L) * loan.quantity
        }

        DashboardStats(
            totalProperties = properties.size,
            pendingLoans = loans.count { it.status == LoanStatus.PENDING },
            activeLoans = loans.count { it.status == LoanStatus.APPROVED },
            estimatedIncomeMonth = income
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

class DashboardViewModelFactory(
    private val propertyRepository: PropertyRepository,
    private val loanRepository: LoanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(propertyRepository, loanRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(propertyRepository, loanRepository, SavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
