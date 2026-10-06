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
    val iconRes: Int? = null,
    val isFavorite: Boolean = false
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val isReturn: Boolean = false,
    val discountPercent: Double = 0.0
) {
    val unitPriceAfterDiscount: Double
        get() = (product.price * (1.0 - (discountPercent / 100.0))).coerceAtLeast(0.0)

    val totalPrice: Double
        get() = (if (isReturn) -1.0 else 1.0) * unitPriceAfterDiscount * quantity
}

data class HeldOrder(
    val id: String,
    val note: String,
    val items: List<CartItem>,
    val customerName: String?,
    val discountPercent: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() {
            val sub = items.sumOf { it.totalPrice }
            return (sub * (1.0 - discountPercent / 100.0)).coerceAtLeast(0.0)
        }
    val itemCount: Int
        get() = items.sumOf { it.quantity }
}

data class TransactionRecord(
    val id: String,
    val type: String = "sale", // "sale", "sale_return", "return", "income", "expense", "purchase"
    val items: List<CartItem> = emptyList(),
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val customerName: String? = null,
    val paymentType: String = "Nağd", // "Nağd", "Kart", "Nisyə", "Qarışıq"
    val cashPaid: Double = 0.0,
    val cardPaid: Double = 0.0,
    val changeGiven: Double = 0.0,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReceiptData(
    val invoiceNumber: String,
    val companyName: String,
    val voen: String? = null,
    val cashierName: String,
    val customerName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paymentType: String,
    val cashPaid: Double,
    val cardPaid: Double,
    val changeGiven: Double,
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
