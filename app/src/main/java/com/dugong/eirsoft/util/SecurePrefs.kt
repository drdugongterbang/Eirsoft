package com.dugong.eirsoft.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveRole(role: String) {
        sharedPrefs.edit().putString("user_role", role).apply()
    }

    fun getRole(): String? {
        return sharedPrefs.getString("user_role", null)
    }

    fun saveUserName(name: String) {
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String? {
        return sharedPrefs.getString("user_name", null)
    }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}