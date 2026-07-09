package com.dugong.eirsoft.data.model

data class Property(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val condition: String = "Baik",
    val totalStock: Int = 0,
    val availableStock: Int = 0,
    val rentPrice: Long = 0L,
    val imagePath: String = "", // Menggunakan path local
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)