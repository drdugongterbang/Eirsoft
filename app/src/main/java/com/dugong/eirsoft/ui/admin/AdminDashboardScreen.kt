package com.dugong.eirsoft.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.LoanRepository
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onManageProperties: () -> Unit,
    onApproveLoans: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    // Inisialisasi ViewModel di dalam body menggunakan Factory
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(context))
    )
    
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(PropertyRepository(context), LoanRepository())
    )
    val stats by dashboardViewModel.stats.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eirsoft Admin") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dashboard Ringkasan",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(
                    title = "Total Properti",
                    value = stats.totalProperties.toString(),
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Pending Pinjaman",
                    value = stats.pendingLoans.toString(), 
                    icon = Icons.Default.PlaylistAddCheck,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(
                    title = "Sedang Disewa",
                    value = stats.activeLoans.toString(),
                    icon = Icons.Default.Assignment,
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Income Bulan Ini",
                    value = "Rp ${stats.estimatedIncomeMonth}",
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onManageProperties,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Inventory, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola Stok Properti")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApproveLoans,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.PlaylistAddCheck, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Persetujuan Peminjaman")
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Apakah Anda yakin ingin keluar?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Text("Keluar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
