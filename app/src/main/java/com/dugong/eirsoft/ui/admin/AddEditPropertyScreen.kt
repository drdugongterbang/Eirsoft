package com.dugong.eirsoft.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dugong.eirsoft.data.model.Property
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.viewmodel.PropertyState
import com.dugong.eirsoft.viewmodel.PropertyViewModel
import com.dugong.eirsoft.viewmodel.PropertyViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPropertyScreen(
    propertyId: String? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(PropertyRepository(context))
    )
    
    val properties by viewModel.properties.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val existingProperty = remember(propertyId, properties) {
        properties.find { it.id == propertyId }
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Baik") }
    var totalStock by remember { mutableStateOf("0") }
    var rentPrice by remember { mutableStateOf("0") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImagePath by remember { mutableStateOf("") }

    LaunchedEffect(existingProperty) {
        existingProperty?.let {
            name = it.name
            category = it.category
            description = it.description
            condition = it.condition
            totalStock = it.totalStock.toString()
            rentPrice = it.rentPrice.toString()
            existingImagePath = it.imagePath
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    LaunchedEffect(uiState) {
        when (uiState) {
            is PropertyState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Berhasil menyimpan properti")
                    delay(800)
                    onSuccess()
                    viewModel.resetState()
                }
            }
            is PropertyState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as PropertyState.Error).message)
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
                title = { Text(if (propertyId == null) "Tambah Properti" else "Edit Properti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (existingImagePath.isNotEmpty()) {
                        AsyncImage(model = File(existingImagePath), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(48.dp))
                            Text("Pilih Foto")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name, 
                onValueChange = { if (it.length <= 40) name = it }, 
                label = { Text("Nama Properti") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = category, 
                onValueChange = { if (it.length <= 25) category = it }, 
                label = { Text("Kategori") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Barang") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = condition, 
                onValueChange = { if (it.length <= 20) condition = it }, 
                label = { Text("Kondisi") }, 
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Stock Input with +/- buttons
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val current = totalStock.toIntOrNull() ?: 0
                        if (current > 0) totalStock = (current - 1).toString()
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }
                    
                    OutlinedTextField(
                        value = totalStock,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) totalStock = it },
                        label = { Text("Stok") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    IconButton(onClick = {
                        val current = totalStock.toIntOrNull() ?: 0
                        if (current < 9999) totalStock = (current + 1).toString()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                OutlinedTextField(
                    value = rentPrice,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 10) rentPrice = it },
                    label = { Text("Harga Sewa") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") },
                    singleLine = true
                )
            }

            if (uiState is PropertyState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val cleanName = name.trim().take(40)
                        val cleanCategory = category.trim().take(25)
                        val cleanCondition = condition.trim().take(20)
                        val stock = totalStock.toIntOrNull() ?: 0
                        val price = rentPrice.toLongOrNull() ?: 0L

                        if (cleanName.isNotEmpty() && cleanCategory.isNotEmpty()) {
                            // Hitung availableStock berdasarkan selisih totalStock
                            val newAvailableStock = if (propertyId == null) {
                                stock // Properti baru, semua stok tersedia
                            } else {
                                val oldTotal = existingProperty?.totalStock ?: 0
                                val oldAvailable = existingProperty?.availableStock ?: 0
                                val diff = stock - oldTotal
                                (oldAvailable + diff).coerceAtLeast(0)
                            }

                            val prop = Property(
                                id = propertyId ?: "",
                                name = cleanName,
                                category = cleanCategory,
                                description = description.trim(),
                                condition = cleanCondition,
                                totalStock = stock,
                                availableStock = newAvailableStock,
                                rentPrice = price,
                                imagePath = existingImagePath
                            )
                            if (propertyId == null) viewModel.addProperty(prop, imageUri)
                            else viewModel.updateProperty(prop, imageUri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && category.isNotBlank() && (imageUri != null || existingImagePath.isNotEmpty())
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
