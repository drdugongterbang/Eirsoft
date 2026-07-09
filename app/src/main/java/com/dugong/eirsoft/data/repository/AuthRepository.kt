package com.dugong.eirsoft.data.repository

import android.content.Context
import com.dugong.eirsoft.data.model.User
import com.dugong.eirsoft.util.SecurePrefs
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val securePrefs = SecurePrefs(context)

    suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Gagal membuat user")
            
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                username = email.substringBefore("@"), // Default username from email
                role = "member" // Default role
            )
            
            firestore.collection("users").document(user.uid).set(user).await()
            securePrefs.saveRole(user.role)
            securePrefs.saveUserName(user.name)
            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Format email salah atau tidak valid."
                else -> e.localizedMessage ?: "Registrasi gagal"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Gagal login")
            
            val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()
            val user = snapshot.toObject(User::class.java) ?: throw Exception("Data user tidak ditemukan")
            
            securePrefs.saveRole(user.role)
            securePrefs.saveUserName(user.name)
            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    "Email atau password salah, atau sesi login telah kedaluwarsa."
                }
                is FirebaseAuthInvalidUserException -> {
                    if (e.errorCode == "ERROR_USER_DISABLED") {
                        "Akun Anda telah dinonaktifkan oleh admin."
                    } else {
                        "Akun tidak ditemukan. Silakan daftar terlebih dahulu."
                    }
                }
                else -> "Gagal login: ${e.localizedMessage}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    fun logout() {
        auth.signOut()
        securePrefs.clear()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getCurrentUser(): Result<User> {
        return try {
            val uid = getCurrentUserId() ?: throw Exception("User tidak terautentikasi")
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java) ?: throw Exception("Data user tidak ditemukan")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String, username: String, address: String): Result<Unit> {
        return try {
            val uid = getCurrentUserId() ?: throw Exception("User tidak terautentikasi")
            val updates = mapOf(
                "name" to name,
                "username" to username,
                "address" to address
            )
            firestore.collection("users").document(uid).update(updates).await()
            securePrefs.saveUserName(name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePicture(imagePath: String): Result<Unit> {
        return try {
            val uid = getCurrentUserId() ?: throw Exception("User tidak terautentikasi")
            firestore.collection("users").document(uid).update("profilePicturePath", imagePath).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User tidak terautentikasi")
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            
            // Re-authenticate user before sensitive action
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserRole(): String? {
        val cachedRole = securePrefs.getRole()
        if (cachedRole != null) return cachedRole

        val uid = getCurrentUserId() ?: return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val role = snapshot.getString("role")
            if (role != null) {
                securePrefs.saveRole(role)
            }
            role
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUserName(): String? {
        return securePrefs.getUserName()
    }

    suspend fun getAllMembers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "member")
                .get().await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val user = snapshot.toObject(User::class.java) ?: throw Exception("User tidak ditemukan")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
