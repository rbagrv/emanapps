package com.example.business.data.model

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OwnerProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "Sahibkar",
    val phone: String = "",
    val companyName: String = "Global Ticarət MMC",
    val voen: String = "",
    val role: String = "Sahibkar / Təsisçi",
    val authorizedTenants: List<String> = emptyList()
)
