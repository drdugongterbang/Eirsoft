package com.dugong.eirsoft.data.repository

import com.dugong.eirsoft.data.model.LoanRequest
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.data.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LoanRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val loansCollection = firestore.collection("loanRequests")
    private val propertiesCollection = firestore.collection("properties")

    fun getAllLoanRequests(): Flow<List<LoanRequest>> = callbackFlow {
        val subscription = loansCollection
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val loans = snapshot?.toObjects(LoanRequest::class.java) ?: emptyList()
                trySend(loans)
            }
        awaitClose { subscription.remove() }
    }

    fun getLoanRequestsByUser(userId: String): Flow<List<LoanRequest>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = loansCollection
            .whereEqualTo("userId", userId)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val loans = snapshot?.toObjects(LoanRequest::class.java) ?: emptyList()
                trySend(loans)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createLoanRequest(loan: LoanRequest): Result<Unit> {
        return try {
            // Merge logic: Jika ada request PENDING untuk barang yang sama oleh user yang sama, update jumlahnya.
            val existing = loansCollection
                .whereEqualTo("userId", loan.userId)
                .whereEqualTo("propertyId", loan.propertyId)
                .whereEqualTo("status", LoanStatus.PENDING.name)
                .get().await()

            if (!existing.isEmpty) {
                val doc = existing.documents[0]
                val currentQty = doc.getLong("quantity") ?: 0
                doc.reference.update("quantity", currentQty + loan.quantity).await()
            } else {
                val id = loansCollection.document().id
                val newLoan = loan.copy(id = id)
                loansCollection.document(id).set(newLoan).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveLoan(loan: LoanRequest, adminId: String, adminName: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val propertyRef = propertiesCollection.document(loan.propertyId)
                val propertySnapshot = transaction.get(propertyRef)
                val property = propertySnapshot.toObject(Property::class.java)
                    ?: throw Exception("Properti tidak ditemukan")

                if (property.availableStock < loan.quantity) {
                    throw Exception("Stok tidak mencukupi")
                }

                transaction.update(propertyRef, "availableStock", property.availableStock - loan.quantity)
                
                val loanRef = loansCollection.document(loan.id)
                transaction.update(loanRef, mapOf(
                    "status" to LoanStatus.APPROVED,
                    "decisionDate" to System.currentTimeMillis(),
                    "approvedByAdminId" to adminId,
                    "approvedByAdminName" to adminName
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectLoan(loanId: String, note: String, adminId: String, adminName: String): Result<Unit> {
        return try {
            // Gunakan Map untuk menghindari error argument position
            val updates = mapOf(
                "status" to LoanStatus.REJECTED,
                "decisionDate" to System.currentTimeMillis(),
                "adminNote" to note,
                "approvedByAdminId" to adminId,
                "approvedByAdminName" to adminName
            )
            loansCollection.document(loanId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsReturned(loan: LoanRequest, adminId: String, adminName: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val propertyRef = propertiesCollection.document(loan.propertyId)
                val propertySnapshot = transaction.get(propertyRef)
                val property = propertySnapshot.toObject(Property::class.java)
                    ?: throw Exception("Properti tidak ditemukan")

                transaction.update(propertyRef, "availableStock", property.availableStock + loan.quantity)
                
                val loanRef = loansCollection.document(loan.id)
                transaction.update(loanRef, mapOf(
                    "status" to LoanStatus.RETURNED,
                    "returnDate" to System.currentTimeMillis(),
                    "receivedByAdminId" to adminId,
                    "receivedByAdminName" to adminName
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
