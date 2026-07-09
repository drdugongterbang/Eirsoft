package com.dugong.eirsoft.data.model

enum class LoanStatus {
    PENDING, APPROVED, REJECTED, RETURNED
}

data class LoanRequest(
    val id: String = "",
    val propertyId: String = "",
    val propertyName: String = "",
    val userId: String = "",
    val userName: String = "",
    val quantity: Int = 1,
    val status: LoanStatus = LoanStatus.PENDING,
    val requestDate: Long = System.currentTimeMillis(),
    val decisionDate: Long? = null,
    val returnDate: Long? = null,
    val adminNote: String? = null,
    val approvedByAdminId: String? = null,
    val approvedByAdminName: String? = null,
    val receivedByAdminId: String? = null,
    val receivedByAdminName: String? = null
)