package com.dugong.eirsoft.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.model.LoanRequest
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.LoanRepository
import com.dugong.eirsoft.ui.components.LoanStatusBadge
import com.dugong.eirsoft.viewmodel.LoanState
import com.dugong.eirsoft.viewmodel.LoanViewModel
import com.dugong.eirsoft.viewmodel.LoanViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveLoanScreen(
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val adminId = authRepository.getCurrentUserId() ?: ""
    val adminName = authRepository.getCurrentUserName() ?: "Admin"

    val viewModel: LoanViewModel = viewModel(factory = LoanViewModelFactory(LoanRepository()))
    val allLoans by viewModel.allLoanRequests.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Aktif", "Riwayat")

    val filteredLoans = remember(allLoans, selectedTab) {
        when (selectedTab) {
            0 -> allLoans.filter { it.status == LoanStatus.PENDING }
            1 -> allLoans.filter { it.status == LoanStatus.APPROVED }
            else -> allLoans.filter { it.status == LoanStatus.REJECTED || it.status == LoanStatus.RETURNED }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoanState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Berhasil memproses pengajuan")
                }
                viewModel.resetState()
            }
            is LoanState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as LoanState.Error).message)
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Persetujuan Pinjaman") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filteredLoans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data peminjaman.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLoans) { loan ->
                        LoanAdminCard(
                            loan = loan,
                            onApprove = { viewModel.approveLoan(loan, adminId, adminName) },
                            onReject = { viewModel.rejectLoan(loan.id, "Ditolak oleh admin", adminId, adminName) },
                            onReturn = { viewModel.markAsReturned(loan, adminId, adminName) },
                            onMemberClick = { onMemberClick(loan.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoanAdminCard(
    loan: LoanRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReturn: () -> Unit,
    onMemberClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = loan.propertyName, style = MaterialTheme.typography.titleMedium)
                LoanStatusBadge(loan.status)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp).clickable { onMemberClick() }
            ) {
                Text(
                    text = "Peminjam: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = loan.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(text = "Jumlah: ${loan.quantity} unit", style = MaterialTheme.typography.bodySmall)
            Text(text = "Tgl Request: ${sdf.format(Date(loan.requestDate))}", style = MaterialTheme.typography.bodySmall)

            if (loan.status == LoanStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Check, null)
                        Text("Setujui")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Text("Tolak")
                    }
                }
            } else if (loan.status == LoanStatus.APPROVED) {
                Button(
                    onClick = onReturn,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AssignmentReturn, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tandai Dikembalikan")
                }
            }
            
            if (loan.status != LoanStatus.PENDING && loan.approvedByAdminName != null) {
                val action = if (loan.status == LoanStatus.REJECTED) "Ditolak" else "Disetujui"
                Text(text = "$action oleh: ${loan.approvedByAdminName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            
            if (loan.status == LoanStatus.RETURNED && loan.receivedByAdminName != null) {
                Text(text = "Diterima oleh: ${loan.receivedByAdminName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
