package com.dugong.eirsoft.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.model.Property
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.ui.components.PropertyCard
import com.dugong.eirsoft.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePropertyScreen(
    onBack: () -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(PropertyRepository(context))
    )
    val properties by viewModel.properties.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var propertyToDelete by remember { mutableStateOf<Property?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredProperties = remember(properties, searchQuery, selectedSortOrder) {
        val filtered = properties.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.category.contains(searchQuery, ignoreCase = true)
        }
        viewModel.sortProperties(filtered, selectedSortOrder)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PropertyState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Berhasil memperbarui data")
                }
                viewModel.resetState()
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
                title = { Text("Kelola Properti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Urutkan")
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nama (A-Z)") },
                                onClick = { selectedSortOrder = SortOrder.NAME_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Stok Terendah") },
                                onClick = { selectedSortOrder = SortOrder.STOCK_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Stok Terbanyak") },
                                onClick = { selectedSortOrder = SortOrder.STOCK_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Harga Termahal") },
                                onClick = { selectedSortOrder = SortOrder.PRICE_DESC; showSortMenu = false }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProperty) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Properti")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cari properti...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (filteredProperties.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (properties.isEmpty()) "Belum ada properti." else "Properti tidak ditemukan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredProperties) { property ->
                        PropertyCard(
                            property = property,
                            isAdmin = true,
                            onEdit = { onEditProperty(property.id) },
                            onDelete = { propertyToDelete = property }
                        )
                    }
                }
            }
        }

        if (propertyToDelete != null) {
            AlertDialog(
                onDismissRequest = { propertyToDelete = null },
                title = { Text("Hapus Properti") },
                text = { Text("Apakah Anda yakin ingin menghapus ${propertyToDelete?.name}? Tindakan ini tidak dapat dibatalkan.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            propertyToDelete?.let { viewModel.deleteProperty(it) }
                            propertyToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { propertyToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
