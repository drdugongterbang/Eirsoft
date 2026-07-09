package com.dugong.eirsoft.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.viewmodel.PropertyViewModel
import com.dugong.eirsoft.viewmodel.PropertyViewModelFactory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: String,
    onBack: () -> Unit,
    onRequestLoan: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(PropertyRepository(context))
    )
    val properties by viewModel.properties.collectAsState()
    val property = remember(propertyId, properties) {
        properties.find { it.id == propertyId }
    }

    // FIX: Masukkan context ke AuthRepository
    val authRepository = remember { AuthRepository(context) }
    var userRole by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        userRole = authRepository.getCurrentUserRole()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Properti") },
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
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = if (property.imagePath.isNotEmpty()) File(property.imagePath) else null,
                    contentDescription = property.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = property.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = property.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Kondisi", style = MaterialTheme.typography.labelLarge)
                            Text(text = property.condition, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Harga Sewa", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Rp ${property.rentPrice}/hari",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Informasi Stok", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Tersedia: ${property.availableStock} unit (Total: ${property.totalStock})",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (userRole == "member") {
                        Button(
                            onClick = { onRequestLoan(property.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = property.availableStock > 0,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (property.availableStock > 0) "Ajukan Peminjaman" else "Stok Habis")
                        }
                    } else if (userRole == "admin") {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kembali (Admin Mode)")
                        }
                    }
                }
            }
        }
    }
}
