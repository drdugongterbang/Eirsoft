package com.dugong.eirsoft

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.navigation.NavGraph
import com.dugong.eirsoft.navigation.Screen
import com.dugong.eirsoft.ui.theme.EirsoftTheme
import com.dugong.eirsoft.ui.components.LoadingIndicator

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        logoutUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository = AuthRepository(this)
        enableEdgeToEdge()
        setContent {
            EirsoftTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    try {
                        val userId = authRepository.getCurrentUserId()
                        if (userId != null) {
                            val role = authRepository.getCurrentUserRole()
                            startDestination = if (role == "admin") {
                                Screen.AdminDashboard.route
                            } else {
                                Screen.MemberDashboard.route
                            }
                            resetIdleTimer()
                        } else {
                            startDestination = Screen.Login.route
                        }
                    } catch (e: Exception) {
                        startDestination = Screen.Login.route
                    } finally {
                        isLoading = false
                    }
                }

                if (isLoading) {
                    LoadingIndicator()
                } else {
                    startDestination?.let { destination ->
                        NavGraph(navController = navController, startDestination = destination)
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetIdleTimer()
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        if (::authRepository.isInitialized && authRepository.getCurrentUserId() != null) {
            idleHandler.postDelayed(idleRunnable, 10 * 60 * 1000) // 10 Menit
        }
    }

    private fun logoutUser() {
        if (::authRepository.isInitialized) {
            authRepository.logout()
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        idleHandler.removeCallbacks(idleRunnable)
    }
}
