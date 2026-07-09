package com.dugong.eirsoft.data.repository

import android.content.Context
import android.net.Uri
import com.dugong.eirsoft.data.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PropertyRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("properties")

    fun getAllProperties(): Flow<List<Property>> = callbackFlow {
        val subscription = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Jika error karena logout (PERMISSION_DENIED), cukup tutup flow dengan tenang
                    close()
                    return@addSnapshotListener
                }
                val properties = snapshot?.toObjects(Property::class.java) ?: emptyList()
                trySend(properties)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addProperty(property: Property, imageUri: Uri?): Result<Unit> {
        return try {
            val id = if (property.id.isEmpty()) UUID.randomUUID().toString() else property.id
            var finalProperty = property.copy(id = id)
            
            if (imageUri != null) {
                val localPath = saveImageToInternalStorage(imageUri)
                finalProperty = finalProperty.copy(imagePath = localPath)
            }
            
            collection.document(id).set(finalProperty).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProperty(property: Property, newImageUri: Uri?): Result<Unit> {
        return try {
            var updatedProperty = property
            if (newImageUri != null) {
                if (property.imagePath.isNotEmpty()) {
                    File(property.imagePath).delete()
                }
                val localPath = saveImageToInternalStorage(newImageUri)
                updatedProperty = property.copy(imagePath = localPath)
            }
            collection.document(property.id).set(updatedProperty).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProperty(property: Property): Result<Unit> {
        return try {
            if (property.imagePath.isNotEmpty()) {
                File(property.imagePath).delete()
            }
            collection.document(property.id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val fileName = "prop_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
}