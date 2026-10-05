package com.example.business.data.model

enum class DebtStatus(val label: String) {
    ACTIVE("Aktiv"),
    OVERDUE("Gecikmiş"),
    PAID("Tam ödənilmiş")
}

data class PaymentRecord(
    val id: String,
    val amount: Double,
    val date: String,
    val paymentType: String, // Nəğd, Kart, Bank köçürməsi
    val cashier: String
)

data class Debtor(
    val id: String,
    val fullName: String,
    val companyName: String? = null,
    val phone: String,
    val totalDebt: Double,
    val creditLimit: Double = 5000.0,
    val dueDate: String,
    val status: DebtStatus,
    val lastPaymentDate: String? = null,
    val lastPaymentAmount: Double? = null,
    val notes: String = "",
    val history: List<PaymentRecord> = emptyList()
)
