package com.dugong.eirsoft.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.model.LoanRequest
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.LoanRepository
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestLoanScreen(
    propertyId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(PropertyRepository(context))
    )
    val loanViewModel: LoanViewModel = viewModel(
        factory = LoanViewModelFactory(LoanRepository())
    )

    val properties by propertyViewModel.properties.collectAsState()
    val property = remember(propertyId, properties) {
        properties.find { it.id == propertyId }
    }
    
    val authRepository = remember { AuthRepository(context) }
    val userId = authRepository.getCurrentUserId() ?: ""
    val userName = authRepository.getCurrentUserName() ?: "Member"
    
    var quantity by remember { mutableStateOf("1") }
    val uiState by loanViewModel.uiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoanState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Pengajuan berhasil dikirim")
                    delay(800)
                    onSuccess()
                    loanViewModel.resetState()
                }
            }
            is LoanState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as LoanState.Error).message)
                }
                loanViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajukan Pinjaman") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (property == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = property.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Harga Sewa: ${currencyFormatter.format(property.rentPrice)} / unit")
                        Text(text = "Tersedia: ${property.availableStock} unit", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                            quantity = it
                        }
                    },
                    label = { Text("Jumlah Pinjam") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        val q = quantity.toIntOrNull() ?: 0
                        if (q > property.availableStock) {
                            Text("Jumlah melebihi stok tersedia", color = MaterialTheme.colorScheme.error)
                        } else if (q <= 0 && quantity.isNotEmpty()) {
                            Text("Jumlah minimal adalah 1", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    isError = (quantity.toIntOrNull() ?: 0) > property.availableStock || (quantity.toIntOrNull() ?: 0) <= 0
                )

                val totalHarga = (quantity.toIntOrNull() ?: 0) * property.rentPrice
                
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Total Harga Pinjam:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = currencyFormatter.format(totalHarga),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState is LoanState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            val q = quantity.toIntOrNull() ?: 0
                            if (q > 0 && q <= property.availableStock) {
                                val request = LoanRequest(
                                    propertyId = property.id,
                                    propertyName = property.name,
                                    userId = userId,
                                    userName = userName,
                                    quantity = q,
                                    status = LoanStatus.PENDING
                                )
                                loanViewModel.createLoanRequest(request)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = (quantity.toIntOrNull() ?: 0) in 1..property.availableStock,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Kirim Pengajuan", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
