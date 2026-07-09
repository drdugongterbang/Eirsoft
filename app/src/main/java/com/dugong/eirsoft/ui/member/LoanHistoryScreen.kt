package com.dugong.eirsoft.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.model.LoanRequest
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.LoanRepository
import com.dugong.eirsoft.ui.components.LoanStatusBadge
import com.dugong.eirsoft.viewmodel.LoanViewModel
import com.dugong.eirsoft.viewmodel.LoanViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val viewModel: LoanViewModel = viewModel(
        factory = LoanViewModelFactory(LoanRepository())
    )
    
    val authRepository = remember { AuthRepository(context) }
    val userId = authRepository.getCurrentUserId() ?: ""
    
    // Gunakan StateFlow yang stabil dari ViewModel
    val userLoans by viewModel.userLoans.collectAsState()

    // Ambil data hanya sekali saat layar dibuka
    LaunchedEffect(userId) {
        viewModel.fetchUserLoans(userId)
    }

    // Grouping logic: Gabungkan item yang sama dengan status yang sama
    val groupedLoans = remember(userLoans) {
        userLoans.groupBy { it.propertyId + it.status.name }
            .map { (_, list) ->
                list.reduce { acc, loan ->
                    acc.copy(
                        quantity = acc.quantity + loan.quantity,
                        requestDate = maxOf(acc.requestDate, loan.requestDate)
                    )
                }
            }
            .sortedByDescending { it.requestDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Peminjaman") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (groupedLoans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada riwayat peminjaman.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = groupedLoans,
                    key = { it.id } // Tambahkan key untuk stabilitas list
                ) { loan ->
                    LoanMemberCard(loan = loan)
                }
            }
        }
    }
}

@Composable
fun LoanMemberCard(loan: LoanRequest) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(), 
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = { /* Interaksi tambahan bisa ditambahkan di sini */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = loan.propertyName, style = MaterialTheme.typography.titleMedium)
                LoanStatusBadge(loan.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Total Jumlah: ${loan.quantity} unit", style = MaterialTheme.typography.bodySmall)
            Text(text = "Tgl Request Terakhir: ${sdf.format(Date(loan.requestDate))}", style = MaterialTheme.typography.bodySmall)
            
            if (loan.status != LoanStatus.PENDING && loan.approvedByAdminName != null) {
                val label = if (loan.status == LoanStatus.REJECTED) "Ditolak oleh: " else "Disetujui oleh: "
                Text(text = "$label${loan.approvedByAdminName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (loan.status == LoanStatus.RETURNED && loan.receivedByAdminName != null) {
                Text(text = "Diterima kembali oleh: ${loan.receivedByAdminName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            loan.adminNote?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Catatan Admin: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
