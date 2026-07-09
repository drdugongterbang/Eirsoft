package com.dugong.eirsoft.util

object Validators {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        // Mengikuti instruksi user: tidak perlu kerumitan password (angka/huruf) dahulu
        // Hanya cek panjang minimal untuk keamanan dasar
        return password.length >= 6
    }
}