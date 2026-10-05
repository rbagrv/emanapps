package com.example.business.data.model

data class SupplierInvoice(
    val invoiceNumber: String,
    val date: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val isPaid: Boolean
)

data class Supplier(
    val id: String,
    val companyName: String,
    val contactPerson: String,
    val phone: String,
    val email: String? = null,
    val totalPayable: Double, // Verəcək borc
    val category: String, // Qida, İçki, Qablaşdırma, Xammal və s.
    val lastDeliveryDate: String,
    val invoices: List<SupplierInvoice> = emptyList()
)
