package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

// ==========================================
// 1. LOGIN REQUEST & RESPONSE MODELS
// ==========================================

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,
    
    @SerializedName("apiKey")
    val apiKey: String? = null
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("token")
    val token: String?,

    @SerializedName("tenant")
    val tenant: Tenant?,

    @SerializedName("user")
    val user: User?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("message")
    val message: String?
)

data class Tenant(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("voen")
    val voen: String?
)

data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("displayName")
    val displayName: String
)


// ==========================================
// 2. PRODUCT MODELS
// ==========================================

data class ProductResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("count")
    val count: Int,

    @SerializedName("total")
    val total: Int,

    @SerializedName("products")
    val products: List<Product>
)

data class Product(
    @SerializedName("id")
    val id: String,

    @SerializedName("productNumber")
    val productNumber: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("sku")
    val sku: String?,

    @SerializedName("barcode")
    val barcode: String?,

    @SerializedName("price")
    val price: Double,

    @SerializedName("salePrice")
    val salePrice: Double,

    @SerializedName("costPrice")
    val costPrice: Double?,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("stock")
    val stock: Double,

    @SerializedName("stockQuantity")
    val stockQuantity: Double,

    @SerializedName("taxRate")
    val taxRate: Double,

    @SerializedName("isVatApplicable")
    val isVatApplicable: Boolean
)


// ==========================================
// 3. SALE SUBMISSION MODELS
// ==========================================

data class SaleRequest(
    @SerializedName("paymentType")
    val paymentType: String,

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("cashierName")
    val cashierName: String?,

    @SerializedName("cashboxCode")
    val cashboxCode: String?,

    @SerializedName("customerName")
    val customerName: String?,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("items")
    val items: List<SaleItem>
)

data class SaleItem(
    @SerializedName("productId")
    val productId: String,

    @SerializedName("barcode")
    val barcode: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("price")
    val price: Double,

    @SerializedName("unit")
    val unit: String
)

data class SaleResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("data")
    val data: SaleData?
)

data class SaleData(
    @SerializedName("invoiceId")
    val invoiceId: String,

    @SerializedName("invoiceNumber")
    val invoiceNumber: String,

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("paymentType")
    val paymentType: String,

    @SerializedName("itemsCount")
    val itemsCount: Int,

    @SerializedName("date")
    val date: String
)
