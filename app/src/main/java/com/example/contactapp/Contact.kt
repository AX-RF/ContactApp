package com.example.contactapp

data class Contact(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val email: String? = null,
    val photoUri: String? = null
)