package com.dugong.eirsoft.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dugong.eirsoft.data.repository.AuthRepository
import com.dugong.eirsoft.data.repository.PropertyRepository
import com.dugong.eirsoft.ui.components.PropertyCard
import com.dugong.eirsoft.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    onPropertyClick: (String) -> Unit,
    onViewHistory: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(context))
    )
    
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(PropertyRepository(context))
    )
    val properties by propertyViewModel.properties.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedSortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = remember(properties) {
        listOf("Semua") + properties.map { it.category }.distinct().sorted()
    }

    val filteredProperties = remember(properties, searchQuery, selectedCategory, selectedSortOrder) {
        val filtered = properties.filter { 
            (selectedCategory == "Semua" || it.category == selectedCategory) &&
            (it.name.contains(searchQuery, ignoreCase = true))
        }
        propertyViewModel.sortProperties(filtered, selectedSortOrder)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katalog Eirsoft") },
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
                                text = { Text("Nama (Z-A)") },
                                onClick = { selectedSortOrder = SortOrder.NAME_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Harga Termurah") },
                                onClick = { selectedSortOrder = SortOrder.PRICE_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Harga Termahal") },
                                onClick = { selectedSortOrder = SortOrder.PRICE_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Stok Terbanyak") },
                                onClick = { selectedSortOrder = SortOrder.STOCK_DESC; showSortMenu = false }
                            )
                        }
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profil")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            if (filteredProperties.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (properties.isEmpty()) "Tidak ada properti tersedia." else "Properti tidak ditemukan.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredProperties) { property ->
                        PropertyCard(
                            property = property,
                            isAdmin = false,
                            onClick = { onPropertyClick(property.id) }
                        )
                    }
                }
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
