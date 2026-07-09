package com.dugong.eirsoft.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val username: String = "",
    val address: String = "",
    val profilePicturePath: String = "",
    val role: String = "member",
    val createdAt: Long = System.currentTimeMillis()
)