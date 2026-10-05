package com.example.myapplication.data

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val category: String = "Ümumi",
    val stockQuantity: Double = 100.0,
    val damagedQuantity: Double = 0.0,
    val barcode: String? = null,
    val unit: String = "əd",
    val iconRes: Int? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val isReturn: Boolean = false
) {
    val totalPrice: Double
        get() = (if (isReturn) -1.0 else 1.0) * product.price * quantity
}

data class TransactionRecord(
    val id: String,
    val type: String = "sale", // "sale", "return", "income", "expense", "purchase"
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val customerName: String? = null,
    val paymentType: String = "Nağd",
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CashOperation(
    val id: String,
    val type: String, // "income" (Mədaxil) or "expense" (Məxaric)
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Supplier(
    val id: String,
    val name: String,
    val phone: String? = null,
    val company: String? = null,
    val balance: Double = 0.0
)

data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val balance: Double = 0.0
)

