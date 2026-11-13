package com.example.business.data.repository

import android.util.Log
import com.example.business.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BusinessRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val initialWarehouseItems = emptyList<WarehouseItem>()
    private val initialWarehouseSummary = WarehouseSummary(0, 0.0, 0.0, 0.0, 0)
    private val initialDebtors = emptyList<Debtor>()
    private val initialSuppliers = emptyList<Supplier>()
    private val initialSummary = BusinessSummary()

    private val _summary = MutableStateFlow(initialSummary)
    val summary: StateFlow<BusinessSummary> = _summary.asStateFlow()

    private val _debtors = MutableStateFlow<List<Debtor>>(initialDebtors)
    val debtors: StateFlow<List<Debtor>> = _debtors.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(initialSuppliers)
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    private val _warehouseItems = MutableStateFlow<List<WarehouseItem>>(initialWarehouseItems)
    val warehouseItems: StateFlow<List<WarehouseItem>> = _warehouseItems.asStateFlow()

    private val _warehouseSummary = MutableStateFlow(initialWarehouseSummary)
    val warehouseSummary: StateFlow<WarehouseSummary> = _warehouseSummary.asStateFlow()

    private val _stockMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    val stockMovements: StateFlow<List<StockMovement>> = _stockMovements.asStateFlow()

    private var initialInvoicesLoaded = false
    private var initialPurchasesLoaded = false
    private var initialPosLoaded = false
    private val d1StockMovementsCache = mutableListOf<StockMovement>()

    private val _channels = MutableStateFlow<List<ChatChannel>>(createDefaultChannels())
    val channels: StateFlow<List<ChatChannel>> = _channels.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _ownerProfile = MutableStateFlow(OwnerProfile())
    val ownerProfile: StateFlow<OwnerProfile> = _ownerProfile.asStateFlow()

    private var activeTenantId: String = ""

    // Listeners for Real ERP collections
    private var tenantListener: ListenerRegistration? = null
    private var customersListener: ListenerRegistration? = null
    private var mobilePosCustomersListener: ListenerRegistration? = null
    private var suppliersListener: ListenerRegistration? = null
    private var invoicesListener: ListenerRegistration? = null
    private var purchasesListener: ListenerRegistration? = null
    private var cashAccountsListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var posTransactionsListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null

    // Cache customer debts from mobile_pos_transactions
    private val customerDebtFromPos = mutableMapOf<String, Double>()
    private var lastCustomersDocs = emptyList<DocumentSnapshot>()
    private var lastMobilePosCustomersDocs = emptyList<DocumentSnapshot>()

    // Real dynamic analytics cache
    data class D1SaleEntry(
        val dateStr: String,
        val grossTotal: Double,
        val cashAmount: Double,
        val cardAmount: Double,
        val cashierName: String
    )
    private val realInvoices = mutableListOf<Map<String, Any>>()
    private val realPurchases = mutableListOf<Map<String, Any>>()
    private val realPosTransactions = mutableListOf<Map<String, Any>>()
    private var d1PosSalesCache = listOf<D1SaleEntry>()
    private var d1TopProductsCache = listOf<TopProductSale>()

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        val displayName = auth.currentUser?.displayName ?: "Sahibkar"

        _ownerProfile.value = _ownerProfile.value.copy(
            uid = uid,
            email = email,
            displayName = displayName
        )

        userDocListener?.remove()
        userDocListener = firestore.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val dName = doc.getString("displayName") ?: displayName
                    val phone = doc.getString("phone") ?: doc.getString("phoneNumber") ?: ""
                    val cName = doc.getString("companyName") ?: doc.getString("tenantName") ?: "e-Manager Müəssisə"
                    val role = doc.getString("role") ?: "Sahibkar"
                    @Suppress("UNCHECKED_CAST")
                    val tenants = doc.get("authorizedTenants") as? List<String> ?: emptyList()

                    _ownerProfile.value = OwnerProfile(
                        uid = uid,
                        email = email,
                        displayName = dName,
                        phone = phone,
                        companyName = cName,
                        voen = doc.getString("voen") ?: "",
                        role = role,
                        authorizedTenants = tenants
                    )
                }
            }
    }

    fun updateOwnerProfile(name: String, phone: String, companyName: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onComplete(false, "İstifadəçi sessiyası tapılmadı")
            return
        }

        val updates = hashMapOf<String, Any>(
            "displayName" to name,
            "phone" to phone,
            "phoneNumber" to phone,
            "companyName" to companyName,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                _ownerProfile.value = _ownerProfile.value.copy(
                    displayName = name,
                    phone = phone,
                    companyName = companyName
                )
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage ?: "Məlumatları yeniləmək mümkün olmadı")
            }
    }

    fun connectToTenant(tenantId: String, tenantName: String) {
        if (tenantId.isBlank()) return
        this.activeTenantId = tenantId
        Log.d("BusinessRepo", "Connecting to REAL e-manager ERP tenant: $tenantId ($tenantName)")

        // Remove old listeners
        tenantListener?.remove()
        customersListener?.remove()
        mobilePosCustomersListener?.remove()
        suppliersListener?.remove()
        invoicesListener?.remove()
        purchasesListener?.remove()
        cashAccountsListener?.remove()
        productsListener?.remove()
        chatListener?.remove()
        posTransactionsListener?.remove()

        // Reset caches so previous tenant's data never bleeds into the new tenant
        realInvoices.clear()
        realPurchases.clear()
        realPosTransactions.clear()
        d1PosSalesCache = emptyList()
        d1TopProductsCache = emptyList()
        d1StockMovementsCache.clear()
        customerDebtFromPos.clear()
        lastCustomersDocs = emptyList()
        lastMobilePosCustomersDocs = emptyList()
        initialInvoicesLoaded = false
        initialPurchasesLoaded = false
        initialPosLoaded = false

        _debtors.value = emptyList()
        _suppliers.value = emptyList()
        _warehouseItems.value = emptyList()
        _stockMovements.value = emptyList()
        _warehouseSummary.value = WarehouseSummary(0, 0.0, 0.0, 0.0, 0)
        _summary.value = BusinessSummary(companyName = tenantName)

        // =========================================================
        // 1. REAL TENANT INFO: `tenants/{tenantId}`
        // =========================================================
        tenantListener = firestore.collection("tenants").document(tenantId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: tenantName
                    val voen = doc.getString("voen") ?: _summary.value.voen
                    _summary.value = _summary.value.copy(
                        companyName = name,
                        voen = voen
                    )
                }
            }

        // =========================================================
        // 2. REAL CUSTOMERS / DEBITORS: `tenants/{tenantId}/customers` & `mobile_pos_customers`
        // =========================================================
        customersListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("customers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Customers sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    lastCustomersDocs = snapshot.documents
                    recalculateDebtors()
                }
            }

        mobilePosCustomersListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("mobile_pos_customers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Mobile POS Customers sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    lastMobilePosCustomersDocs = snapshot.documents
                    recalculateDebtors()
                }
            }

        // =========================================================
        // 3. REAL SUPPLIERS / CREDITORS: `tenants/{tenantId}/suppliers`
        // =========================================================
        suppliersListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("suppliers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Suppliers sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val suppliersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val contactPerson = doc.getString("contactPerson") ?: doc.getString("representativeName") ?: doc.getString("company") ?: name
                            val phone = doc.getString("phone") ?: ""
                            val email = doc.getString("email")
                            val balance = doc.safeDouble("totalPayable", doc.safeDouble("balance", doc.safeDouble("debt", doc.safeDouble("payable", 0.0))))
                            val voen = doc.getString("voen")
                            val category = doc.getString("category")?.ifBlank { null }
                                ?: if (!voen.isNullOrBlank()) "VÖEN: $voen" else "Təchizat"
                            val debtStartDate = doc.getString("debtStartDate")
                                ?: doc.getString("issueDate")
                                ?: doc.safeDateString("createdAt")
                                ?: "10.09.2026"

                            Supplier(
                                id = id,
                                companyName = name,
                                contactPerson = contactPerson,
                                phone = phone,
                                email = email,
                                totalPayable = balance,
                                debtStartDate = debtStartDate,
                                category = category,
                                lastDeliveryDate = "Aktiv"
                            )
                        } catch (e: Exception) {
                            Log.e("BusinessRepo", "Error mapping supplier document ${doc.id}", e)
                            null
                        }
                    }

                    if (suppliersList.isNotEmpty()) {
                        _suppliers.value = suppliersList
                        val totalPayables = suppliersList.filter { it.totalPayable > 0 }.sumOf { it.totalPayable }
                        _summary.value = _summary.value.copy(
                            totalPayables = totalPayables
                        )
                    }
                }
            }

        // =========================================================
        // 4. REAL CASH & BANK: `tenants/{tenantId}/cash_and_bank_accounts`
        // =========================================================
        cashAccountsListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("cash_and_bank_accounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Cash & Bank accounts error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.documents.isNotEmpty()) {
                    var totalCash = 0.0
                    var totalBank = 0.0

                    for (doc in snapshot.documents) {
                        try {
                            val type = doc.getString("type") ?: "cash"
                            val balance = doc.safeDouble("balance", 0.0)
                            if (type == "cash") {
                                totalCash += balance
                            } else if (type == "bank" || type == "vat_sub_account") {
                                totalBank += balance
                            }
                        } catch (e: Exception) {
                            Log.e("BusinessRepo", "Error mapping cash doc ${doc.id}", e)
                        }
                    }

                    if (totalCash > 0.0 || totalBank > 0.0) {
                        _summary.value = _summary.value.copy(
                            cashInHand = totalCash,
                            bankBalance = totalBank
                        )
                    }
                }
            }

        // =========================================================
        // 5. REAL WAREHOUSE PRODUCTS: `tenants/{tenantId}/products`
        // =========================================================
        productsListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Products sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val sku = doc.getString("sku") ?: doc.getString("barcode") ?: ""
                            val barcode = doc.getString("barcode") ?: doc.getString("sku")
                            val unit = doc.getString("unit") ?: "ədəd"
                            val stock = doc.safeDouble("stockQuantity", doc.safeDouble("stock", doc.safeDouble("quantity", doc.safeDouble("currentStock", 0.0))))
                            val salePrice = doc.safeDouble("salePrice", doc.safeDouble("price", doc.safeDouble("retailPrice", doc.safeDouble("unitPrice", 0.0))))
                            val costPrice = (doc.safeDouble("costPrice", 0.0).takeIf { it > 0.0 }
                                ?: doc.safeDouble("purchasePrice", 0.0).takeIf { it > 0.0 }
                                ?: doc.safeDouble("buyPrice", 0.0).takeIf { it > 0.0 }
                                ?: (doc.get("purchaseDetails") as? Map<*, *>)?.get("price")?.let { (it as? Number)?.toDouble() }
                                ?: (salePrice * 0.70))

                            val categoryName = doc.getString("category")?.ifBlank { null }
                                ?: doc.getString("categoryName")?.ifBlank { null }
                                ?: "Ümumi Mallar"

                            WarehouseItem(
                                id = id,
                                name = name,
                                sku = sku,
                                barcode = barcode,
                                unit = unit,
                                stock = stock,
                                costPrice = costPrice,
                                salePrice = salePrice,
                                categoryId = doc.getString("categoryId") ?: "",
                                categoryName = categoryName,
                                warehouseName = doc.getString("warehouseName") ?: "Əsas Anbar"
                            )
                        } catch (e: Exception) {
                            Log.e("BusinessRepo", "Error mapping product ${doc.id}", e)
                            null
                        }
                    }

                    if (items.isNotEmpty()) {
                        _warehouseItems.value = items
                        val newSummary = calculateWarehouseSummary(items)
                        _warehouseSummary.value = newSummary
                        _summary.value = _summary.value.copy(
                            warehouseStockValue = newSummary.totalStockSaleValue
                        )
                    }
                }
            }

        // =========================================================
        // 6. REAL SALES INVOICES: `tenants/{tenantId}/invoices`
        // =========================================================
        invoicesListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("invoices")
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Invoices sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (initialInvoicesLoaded) {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val num = dc.document.getString("invoiceNumber") ?: dc.document.id
                                val amt = dc.document.safeDouble("totalAmount", 0.0)
                                val cust = dc.document.getString("customerName") ?: "Müştəri"
                                com.example.business.BusinessApp.instance?.let { ctx ->
                                    com.example.business.util.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = "📄 Yeni Satış Qaiməsi (#$num)",
                                        message = "$cust: ₼${String.format(Locale.US, "%.2f", amt)}"
                                    )
                                }
                            }
                        }
                    } else {
                        initialInvoicesLoaded = true
                    }

                    realInvoices.clear()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        realInvoices.add(data)
                    }
                    recalculateAnalytics()
                    recalculateStockMovements()
                }
            }

        // =========================================================
        // 7. REAL PURCHASES: `tenants/{tenantId}/purchases`
        // =========================================================
        purchasesListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("purchases")
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Purchases sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (initialPurchasesLoaded) {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val num = dc.document.getString("purchaseNumber") ?: dc.document.getString("invoiceNumber") ?: dc.document.id
                                val amt = dc.document.safeDouble("totalAmount", 0.0)
                                val sup = dc.document.getString("supplierName") ?: "Təchizatçı"
                                com.example.business.BusinessApp.instance?.let { ctx ->
                                    com.example.business.util.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = "📥 Yeni Alış Əməliyyatı (#$num)",
                                        message = "$sup: ₼${String.format(Locale.US, "%.2f", amt)}"
                                    )
                                }
                            }
                        }
                    } else {
                        initialPurchasesLoaded = true
                    }

                    realPurchases.clear()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        realPurchases.add(data)
                    }
                    recalculateAnalytics()
                    recalculateStockMovements()
                }
            }

        // =========================================================
        // 8. REAL POS TRANSACTIONS: `tenants/{tenantId}/mobile_pos_transactions`
        // =========================================================
        posTransactionsListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("mobile_pos_transactions")
            .limit(300)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    if (initialPosLoaded) {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val pType = dc.document.getString("type") ?: "sale"
                                val amt = dc.document.safeDouble("totalAmount", 0.0)
                                val title = if (pType == "expense") "💸 Yeni Kassa Məxarici" else "🛒 Yeni POS Satışı"
                                com.example.business.BusinessApp.instance?.let { ctx ->
                                    com.example.business.util.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = title,
                                        message = "Məbləğ: ₼${String.format(Locale.US, "%.2f", amt)}"
                                    )
                                }
                            }
                        }
                    } else {
                        initialPosLoaded = true
                    }

                    realPosTransactions.clear()
                    customerDebtFromPos.clear()

                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        realPosTransactions.add(data)

                        // If sale with debt, tally up customer debt
                        val cName = (data["customerName"] as? String)?.trim() ?: ""
                        val debtAmt = (data["debtAmount"] as? Number)?.toDouble() ?: 0.0
                        val pType = (data["paymentType"] as? String) ?: ""
                        if (cName.isNotBlank() && (pType == "debt" || debtAmt > 0.0)) {
                            val cur = customerDebtFromPos[cName] ?: 0.0
                            customerDebtFromPos[cName] = cur + debtAmt
                        }
                    }
                    recalculateAnalytics()
                    recalculateDebtors()
                    recalculateStockMovements()
                }
            }

        // =========================================================
        // 9. REAL INTERNAL CHAT: `tenants/{tenantId}/chat`
        // =========================================================
        chatListener = firestore.collection("tenants")
            .document(tenantId)
            .collection("chat")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("BusinessRepo", "Chat sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val currentUserId = auth.currentUser?.uid ?: "owner_current"
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val text = doc.getString("text") ?: return@mapNotNull null
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("userName") ?: "İstifadəçi"
                            val type = doc.getString("type") ?: "text"
                            val timestamp = doc.safeTimestampMillis("createdAt")
                                ?: doc.safeTimestampMillis("timestamp")
                                ?: System.currentTimeMillis()

                            val isMine = userId == currentUserId

                            ChatMessage(
                                id = doc.id,
                                channelId = "c_general",
                                senderId = userId,
                                senderName = userName,
                                senderRole = if (isMine) "Sahibkar" else "Komanda",
                                text = text,
                                timestamp = timestamp,
                                isMine = isMine,
                                messageType = if (type == "alert") MessageType.ALERT else MessageType.TEXT
                            )
                        } catch (e: Exception) {
                            Log.e("BusinessRepo", "Error mapping chat message ${doc.id}", e)
                            null
                        }
                    }.sortedBy { it.timestamp }

                    val previousCount = (_messages.value["c_general"] ?: emptyList()).size
                    val map = _messages.value.toMutableMap()
                    map["c_general"] = list
                    _messages.value = map

                    if (list.isNotEmpty()) {
                        val lastMsg = list.last()
                        updateChannelLastMessage("c_general", lastMsg.text)

                        // Trigger push notification if a new message arrives from team / system
                        if (list.size > previousCount && !lastMsg.isMine && previousCount > 0) {
                            com.example.business.BusinessApp.instance?.let { ctx ->
                                val notifTitle = if (lastMsg.messageType == MessageType.ALERT) "⚠️ ERP Bildirişi" else "💬 ${lastMsg.senderName}"
                                com.example.business.util.NotificationHelper.showNotification(
                                    context = ctx,
                                    title = notifTitle,
                                    message = lastMsg.text
                                )
                            }
                        }
                    }
                }
            }

        // =========================================================
        // 10. REAL CLOUDFLARE D1 DATABASE SYNC & AGGREGATION
        // =========================================================
        syncWithCloudflareD1(tenantId)
    }

    private fun syncWithCloudflareD1(tenantId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("BusinessRepo", "Starting Cloudflare D1 synchronization for tenant: $tenantId")

                // 1. Fetch Products directly from Cloudflare D1 REST API
                val pResp = com.example.business.data.api.D1ApiClient.erpRestService.getProducts(tenantId, limit = 1000)
                if (pResp.isSuccessful && pResp.body()?.success == true) {
                    val d1Products = pResp.body()?.products ?: emptyList()
                    if (d1Products.isNotEmpty()) {
                        val d1Items = d1Products.map { p ->
                            val sQty = p.stockQuantity ?: p.stock ?: 0.0
                            val sPrice = p.salePrice ?: p.price ?: 0.0
                            val cPrice = (p.costPrice ?: 0.0).let { if (it > 0.0) it else sPrice * 0.70 }
                            WarehouseItem(
                                id = p.id,
                                name = p.name,
                                sku = p.sku ?: p.barcode ?: "",
                                barcode = p.barcode,
                                unit = p.unit ?: "ədəd",
                                stock = sQty,
                                costPrice = cPrice,
                                salePrice = sPrice,
                                categoryId = p.categoryId ?: "default",
                                categoryName = "D1 ERP Məhsulları",
                                warehouseName = "Əsas Anbar"
                            )
                        }

                        _warehouseItems.value = d1Items
                        val newSummary = calculateWarehouseSummary(d1Items)
                        _warehouseSummary.value = newSummary
                        _summary.value = _summary.value.copy(
                            warehouseStockValue = newSummary.totalStockSaleValue
                        )
                        Log.d("BusinessRepo", "Synced ${d1Items.size} products from D1. Stock value: ${newSummary.totalStockSaleValue}")
                    }
                }

                // 2. Fetch Customers directly from Cloudflare D1 REST API
                val cResp = com.example.business.data.api.D1ApiClient.erpRestService.getCustomers(tenantId, limit = 1000)
                if (cResp.isSuccessful && cResp.body()?.success == true) {
                    val d1Customers = cResp.body()?.customers ?: emptyList()
                    if (d1Customers.isNotEmpty()) {
                        val debtorsList = d1Customers.map { c ->
                            val debt = (c.balance ?: c.debt ?: 0.0).coerceAtLeast(0.0)
                            val status = if (debt > 2500.0) DebtStatus.OVERDUE
                            else if (debt > 0.0) DebtStatus.ACTIVE
                            else DebtStatus.PAID

                            Debtor(
                                id = c.id,
                                fullName = c.name,
                                companyName = if (!c.voen.isNullOrBlank()) "VÖEN: ${c.voen}" else "Müştəri",
                                phone = c.phone?.ifBlank { null } ?: "-",
                                totalDebt = debt,
                                creditLimit = 5000.0,
                                dueDate = "Dövri Hesablaşma",
                                debtStartDate = "",
                                status = status,
                                notes = "Cloudflare D1 bazasından"
                            )
                        }

                        _debtors.value = debtorsList
                        val totalReceivables = debtorsList.filter { it.totalDebt > 0 }.sumOf { it.totalDebt }
                        val overdue = debtorsList.filter { it.status == DebtStatus.OVERDUE }.sumOf { it.totalDebt }
                        _summary.value = _summary.value.copy(
                            totalReceivables = totalReceivables,
                            overdueReceivables = overdue
                        )
                        Log.d("BusinessRepo", "Synced ${debtorsList.size} debtors from D1. Total receivables: $totalReceivables")
                    }
                }

                // 3. Fetch POS Sales directly from D1 SQL filtered by tenant_id
                val posSalesSql = """
                    SELECT 
                        cheque_date,
                        gross_total,
                        cash_amount,
                        cashless_amount,
                        cashier_name
                    FROM pos_sales 
                    WHERE tenant_id = ?
                    ORDER BY cheque_date DESC
                """.trimIndent()

                val posResp = com.example.business.data.api.D1ApiClient.cfDirectService.queryRaw(
                    accountId = com.example.business.data.api.D1ApiClient.CF_ACCOUNT_ID,
                    databaseId = com.example.business.data.api.D1ApiClient.CF_D1_DATABASE_ID,
                    authHeader = com.example.business.data.api.D1ApiClient.CF_AUTH_HEADER,
                    body = com.example.business.data.api.D1SqlQueryRequest(
                        sql = posSalesSql,
                        params = listOf(tenantId)
                    )
                )

                var d1CashSales = 0.0
                var d1CardSales = 0.0

                if (posResp.isSuccessful) {
                    val root = posResp.body()
                    val resultArr = root?.getAsJsonArray("result")
                    if (resultArr != null && resultArr.size() > 0) {
                        val resultsObj = resultArr.get(0).asJsonObject
                        val rows = resultsObj.getAsJsonArray("results")
                        if (rows != null) {
                            val entries = mutableListOf<D1SaleEntry>()
                            for (i in 0 until rows.size()) {
                                val r = rows.get(i).asJsonObject
                                val dateStr = r.get("cheque_date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val gross = r.get("gross_total")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val cash = r.get("cash_amount")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val card = r.get("cashless_amount")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val cashier = r.get("cashier_name")?.takeIf { !it.isJsonNull }?.asString ?: "Kassir"

                                val dtPrefix = if (dateStr.length >= 10) dateStr.substring(0, 10) else dateStr
                                entries.add(D1SaleEntry(dtPrefix, gross, cash, card, cashier))
                                d1CashSales += cash
                                d1CardSales += card
                            }
                            d1PosSalesCache = entries
                            recalculateAnalytics()
                            Log.d("BusinessRepo", "Synced ${entries.size} real D1 POS Sales for tenant $tenantId")
                        }
                    }
                }

                // 4. Query Cloudflare D1 direct SQL for Suppliers (Creditors)
                val suppliersQuery = "SELECT id, name, voen, phone, balance FROM suppliers LIMIT 50"
                val supResp = com.example.business.data.api.D1ApiClient.cfDirectService.queryRaw(
                    accountId = com.example.business.data.api.D1ApiClient.CF_ACCOUNT_ID,
                    databaseId = com.example.business.data.api.D1ApiClient.CF_D1_DATABASE_ID,
                    authHeader = com.example.business.data.api.D1ApiClient.CF_AUTH_HEADER,
                    body = com.example.business.data.api.D1SqlQueryRequest(sql = suppliersQuery)
                )

                if (supResp.isSuccessful) {
                    val root = supResp.body()
                    val resultArr = root?.getAsJsonArray("result")
                    if (resultArr != null && resultArr.size() > 0) {
                        val resultsObj = resultArr.get(0).asJsonObject
                        val rows = resultsObj.getAsJsonArray("results")
                        if (rows != null && rows.size() > 0) {
                            val supList = mutableListOf<Supplier>()
                            for (i in 0 until rows.size()) {
                                val r = rows.get(i).asJsonObject
                                val id = r.get("id")?.asString ?: "sup_$i"
                                val name = r.get("name")?.asString ?: "Təchizatçı"
                                val voen = r.get("voen")?.takeIf { !it.isJsonNull }?.asString
                                val phone = r.get("phone")?.takeIf { !it.isJsonNull }?.asString ?: "+994 12 400 00 00"
                                val rawBalance = r.get("balance")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val balance = rawBalance.coerceAtLeast(0.0)

                                supList.add(
                                    Supplier(
                                        id = id,
                                        companyName = name,
                                        contactPerson = if (!voen.isNullOrBlank()) "VÖEN: $voen" else "Məsul Şəxs",
                                        phone = phone,
                                        email = "info@emanager.az",
                                        totalPayable = balance,
                                        debtStartDate = "10.09.2026",
                                        category = "D1 Təchizatçı",
                                        lastDeliveryDate = "Aktiv"
                                    )
                                )
                            }

                            if (supList.isNotEmpty()) {
                                _suppliers.value = supList
                                val totalPayables = supList.filter { it.totalPayable > 0 }.sumOf { it.totalPayable }
                                _summary.value = _summary.value.copy(
                                    totalPayables = totalPayables
                                )
                                Log.d("BusinessRepo", "Synced ${supList.size} suppliers from direct D1 SQL. Total payables: $totalPayables")
                            }
                        }
                    }
                }

                // 5. Query Chart of Accounts for Cash & Bank Accounts in D1 (Hesab adları və qalıqları ilə)
                val coaQuery = """
                    SELECT code, name, current_balance, normal, type 
                    FROM chart_of_accounts 
                    WHERE code LIKE '22%' 
                    ORDER BY code ASC
                """.trimIndent()

                val coaResp = com.example.business.data.api.D1ApiClient.cfDirectService.queryRaw(
                    accountId = com.example.business.data.api.D1ApiClient.CF_ACCOUNT_ID,
                    databaseId = com.example.business.data.api.D1ApiClient.CF_D1_DATABASE_ID,
                    authHeader = com.example.business.data.api.D1ApiClient.CF_AUTH_HEADER,
                    body = com.example.business.data.api.D1SqlQueryRequest(sql = coaQuery)
                )

                if (coaResp.isSuccessful) {
                    val root = coaResp.body()
                    val resultArr = root?.getAsJsonArray("result")
                    if (resultArr != null && resultArr.size() > 0) {
                        val resultsObj = resultArr.get(0).asJsonObject
                        val rows = resultsObj.getAsJsonArray("results")
                        val accountItems = mutableListOf<AccountBalanceItem>()
                        var totalKassa = 0.0
                        var totalBank = 0.0

                        if (rows != null) {
                            for (i in 0 until rows.size()) {
                                val r = rows.get(i).asJsonObject
                                val code = r.get("code")?.asString ?: ""
                                val rawName = r.get("name")?.asString ?: ""
                                val cleanName = when (code) {
                                    "22" -> "Pul vəsaitləri və ekvivalentləri (Cəmi)"
                                    "221" -> "Əsas Kassa (Milli Valyuta - AZN)"
                                    "221.1" -> "Ticarət POS Kassası (AZN)"
                                    "223" -> "Bank Hesablaşma Hesabı (Kapital Bank AZN)"
                                    "224" -> "Xarici Valyuta Hesabı (Bank USD/EUR)"
                                    "225" -> "Pul vəsaitlərinin digər ekvivalentləri"
                                    else -> rawName
                                }

                                var bal = r.get("current_balance")?.asDouble ?: 0.0

                                // If chart_of_accounts is raw unposted, aggregate real balance from D1 POS collections
                                if (code == "221" && bal == 0.0 && d1CashSales > 0.0) {
                                    bal = d1CashSales
                                } else if (code == "223" && bal == 0.0 && d1CardSales > 0.0) {
                                    bal = d1CardSales
                                }

                                val accType = when {
                                    code.startsWith("221") -> "Kassa"
                                    code.startsWith("223") || code.startsWith("224") -> "Bank"
                                    else -> "Pul Vəsaiti"
                                }

                                if (code != "22") { // Sub-accounts
                                    accountItems.add(
                                        AccountBalanceItem(
                                            code = code,
                                            name = cleanName,
                                            type = accType,
                                            balance = bal,
                                            currency = "AZN"
                                        )
                                    )
                                    if (accType == "Kassa") {
                                        totalKassa += bal
                                    } else if (accType == "Bank") {
                                        totalBank += bal
                                    }
                                }
                            }
                        }

                        if (accountItems.isNotEmpty()) {
                            _summary.value = _summary.value.copy(
                                cashInHand = totalKassa,
                                bankBalance = totalBank,
                                accountBalances = accountItems
                            )
                            Log.d("BusinessRepo", "Synced ${accountItems.size} Cash/Bank accounts. Kassa: $totalKassa, Bank: $totalBank")
                        }
                    }
                }

                // 6. Query Top Selling Products from pos_sale_items in D1 filtered by tenant_id
                val topProductsSql = """
                    SELECT psi.raw_name, SUM(psi.quantity) as sold_qty, SUM(psi.total_sum) as revenue, psi.unit_price 
                    FROM pos_sale_items psi
                    INNER JOIN pos_sales ps ON psi.sale_id = ps.id
                    WHERE ps.tenant_id = ?
                    GROUP BY psi.raw_name 
                    ORDER BY revenue DESC 
                    LIMIT 5
                """.trimIndent()

                val topProdResp = com.example.business.data.api.D1ApiClient.cfDirectService.queryRaw(
                    accountId = com.example.business.data.api.D1ApiClient.CF_ACCOUNT_ID,
                    databaseId = com.example.business.data.api.D1ApiClient.CF_D1_DATABASE_ID,
                    authHeader = com.example.business.data.api.D1ApiClient.CF_AUTH_HEADER,
                    body = com.example.business.data.api.D1SqlQueryRequest(
                        sql = topProductsSql,
                        params = listOf(tenantId)
                    )
                )

                if (topProdResp.isSuccessful) {
                    val root = topProdResp.body()
                    val resultArr = root?.getAsJsonArray("result")
                    if (resultArr != null && resultArr.size() > 0) {
                        val resultsObj = resultArr.get(0).asJsonObject
                        val rows = resultsObj.getAsJsonArray("results")
                        if (rows != null && rows.size() > 0) {
                            val d1TopSales = mutableListOf<TopProductSale>()
                            for (i in 0 until rows.size()) {
                                val r = rows.get(i).asJsonObject
                                val pName = r.get("raw_name")?.asString ?: "Məhsul"
                                val qty = r.get("sold_qty")?.asDouble ?: 1.0
                                val rev = r.get("revenue")?.asDouble ?: 0.0
                                d1TopSales.add(
                                    TopProductSale(
                                        productName = pName,
                                        quantitySold = qty,
                                        unit = "ədəd",
                                        totalRevenue = rev
                                    )
                                )
                            }
                            d1TopProductsCache = d1TopSales
                            Log.d("BusinessRepo", "Synced ${d1TopSales.size} top selling items from D1 pos_sale_items")
                        }
                    }
                }

                // 7. Query Stock Movements from D1 pos_sale_items
                val stockMovementsSql = """
                    SELECT 
                        psi.id,
                        ps.cheque_date as date,
                        psi.raw_name as product_name,
                        psi.quantity,
                        psi.unit_price,
                        psi.total_sum,
                        ps.id as doc_no
                    FROM pos_sale_items psi
                    INNER JOIN pos_sales ps ON psi.sale_id = ps.id
                    WHERE ps.tenant_id = ?
                    ORDER BY ps.cheque_date DESC 
                    LIMIT 100
                """.trimIndent()

                val smResp = com.example.business.data.api.D1ApiClient.cfDirectService.queryRaw(
                    accountId = com.example.business.data.api.D1ApiClient.CF_ACCOUNT_ID,
                    databaseId = com.example.business.data.api.D1ApiClient.CF_D1_DATABASE_ID,
                    authHeader = com.example.business.data.api.D1ApiClient.CF_AUTH_HEADER,
                    body = com.example.business.data.api.D1SqlQueryRequest(
                        sql = stockMovementsSql,
                        params = listOf(tenantId)
                    )
                )

                if (smResp.isSuccessful) {
                    val root = smResp.body()
                    val resultArr = root?.getAsJsonArray("result")
                    if (resultArr != null && resultArr.size() > 0) {
                        val resultsObj = resultArr.get(0).asJsonObject
                        val rows = resultsObj.getAsJsonArray("results")
                        if (rows != null && rows.size() > 0) {
                            val d1Movements = mutableListOf<StockMovement>()
                            for (i in 0 until rows.size()) {
                                val r = rows.get(i).asJsonObject
                                val smId = r.get("id")?.takeIf { !it.isJsonNull }?.asString ?: "sm_$i"
                                val dt = r.get("date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val pName = r.get("product_name")?.takeIf { !it.isJsonNull }?.asString ?: "Məhsul"
                                val qty = r.get("quantity")?.takeIf { !it.isJsonNull }?.asDouble ?: 1.0
                                val uPrice = r.get("unit_price")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val tSum = r.get("total_sum")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val docNo = r.get("doc_no")?.takeIf { !it.isJsonNull }?.asString ?: "POS Çek"
                                val cleanDate = if (dt.length >= 10) dt.substring(0, 10) else dt

                                d1Movements.add(
                                    StockMovement(
                                        id = smId,
                                        date = cleanDate,
                                        productName = pName,
                                        movementType = StockMovementType.SALE,
                                        quantity = qty,
                                        unit = "ədəd",
                                        unitPrice = uPrice,
                                        totalPrice = tSum,
                                        documentNumber = docNo,
                                        partnerOrChannel = "POS Kassa"
                                    )
                                )
                            }
                            d1StockMovementsCache.clear()
                            d1StockMovementsCache.addAll(d1Movements)
                            recalculateStockMovements()
                            Log.d("BusinessRepo", "Synced ${d1Movements.size} stock movements from D1")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("BusinessRepo", "Error syncing with Cloudflare D1: ${e.message}", e)
            }
        }
    }

    private fun recalculateStockMovements() {
        val list = mutableListOf<StockMovement>()

        // 1. D1 POS Movements
        list.addAll(d1StockMovementsCache)

        // 2. Real Purchases (Alışlar)
        for (pur in realPurchases) {
            val pId = (pur["id"] as? String) ?: UUID.randomUUID().toString()
            val pNum = (pur["purchaseNumber"] as? String) ?: (pur["invoiceNumber"] as? String) ?: "Alış Sənədi"
            val sup = (pur["supplierName"] as? String) ?: "Təchizatçı"
            val dateStr = (pur["issueDate"] as? String) ?: (pur["date"] as? String) ?: ""
            val totalAmt = (pur["totalAmount"] as? Number)?.toDouble() ?: 0.0

            val items = pur["items"] as? List<*>
            if (items != null && items.isNotEmpty()) {
                for ((idx, itemObj) in items.withIndex()) {
                    val map = itemObj as? Map<*, *> ?: continue
                    val itemName = (map["productName"] as? String) ?: (map["name"] as? String) ?: "Məhsul"
                    val qty = (map["quantity"] as? Number)?.toDouble() ?: 1.0
                    val price = (map["unitPrice"] as? Number)?.toDouble() ?: (map["price"] as? Number)?.toDouble() ?: 0.0
                    val total = (map["total"] as? Number)?.toDouble() ?: (qty * price)
                    val unit = (map["unit"] as? String) ?: "ədəd"

                    list.add(
                        StockMovement(
                            id = "${pId}_$idx",
                            date = dateStr,
                            productName = itemName,
                            movementType = StockMovementType.PURCHASE,
                            quantity = qty,
                            unit = unit,
                            unitPrice = price,
                            totalPrice = total,
                            documentNumber = pNum,
                            partnerOrChannel = sup
                        )
                    )
                }
            } else {
                list.add(
                    StockMovement(
                        id = pId,
                        date = dateStr,
                        productName = "Alış Qaiməsi: $sup",
                        movementType = StockMovementType.PURCHASE,
                        quantity = 1.0,
                        unit = "partiya",
                        unitPrice = totalAmt,
                        totalPrice = totalAmt,
                        documentNumber = pNum,
                        partnerOrChannel = sup
                    )
                )
            }
        }

        // 3. Real Invoices (Satış Qaimələri)
        for (inv in realInvoices) {
            val invId = (inv["id"] as? String) ?: UUID.randomUUID().toString()
            val invNum = (inv["invoiceNumber"] as? String) ?: "Satış Qaiməsi"
            val cust = (inv["customerName"] as? String) ?: "Müştəri"
            val dateStr = (inv["issueDate"] as? String) ?: (inv["date"] as? String) ?: ""
            val totalAmt = (inv["totalAmount"] as? Number)?.toDouble() ?: 0.0

            val items = inv["items"] as? List<*>
            if (items != null && items.isNotEmpty()) {
                for ((idx, itemObj) in items.withIndex()) {
                    val map = itemObj as? Map<*, *> ?: continue
                    val itemName = (map["productName"] as? String) ?: (map["name"] as? String) ?: "Məhsul"
                    val qty = (map["quantity"] as? Number)?.toDouble() ?: 1.0
                    val price = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0
                    val total = (map["total"] as? Number)?.toDouble() ?: (qty * price)
                    val unit = (map["unit"] as? String) ?: "ədəd"

                    list.add(
                        StockMovement(
                            id = "${invId}_$idx",
                            date = dateStr,
                            productName = itemName,
                            movementType = StockMovementType.SALE,
                            quantity = qty,
                            unit = unit,
                            unitPrice = price,
                            totalPrice = total,
                            documentNumber = invNum,
                            partnerOrChannel = cust
                        )
                    )
                }
            } else {
                list.add(
                    StockMovement(
                        id = invId,
                        date = dateStr,
                        productName = "Satış: $cust",
                        movementType = StockMovementType.SALE,
                        quantity = 1.0,
                        unit = "ədəd",
                        unitPrice = totalAmt,
                        totalPrice = totalAmt,
                        documentNumber = invNum,
                        partnerOrChannel = cust
                    )
                )
            }
        }

        // 4. Real POS Transactions
        for (tx in realPosTransactions) {
            val txId = (tx["id"] as? String) ?: UUID.randomUUID().toString()
            val typeStr = (tx["type"] as? String) ?: "sale"
            val receiptNo = (tx["receiptNo"] as? String) ?: (tx["invoiceNo"] as? String) ?: "POS Satış"
            val cName = (tx["cashierName"] as? String) ?: "POS Kassa"
            val totalAmt = (tx["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val timestamp = (tx["timestamp"] as? Number)?.toLong() ?: 0L
            val dateStr = if (timestamp > 0) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)) else ""

            val mType = if (typeStr == "purchase") StockMovementType.PURCHASE
            else if (typeStr == "return") StockMovementType.RETURN
            else StockMovementType.SALE

            val items = tx["items"] as? List<*>
            if (items != null && items.isNotEmpty()) {
                for ((idx, itemObj) in items.withIndex()) {
                    val map = itemObj as? Map<*, *> ?: continue
                    val itemName = (map["name"] as? String) ?: (map["productName"] as? String) ?: "Məhsul"
                    val qty = (map["quantity"] as? Number)?.toDouble() ?: 1.0
                    val price = (map["price"] as? Number)?.toDouble() ?: (map["unitPrice"] as? Number)?.toDouble() ?: 0.0
                    val total = (map["total"] as? Number)?.toDouble() ?: (qty * price)
                    val unit = (map["unit"] as? String) ?: "ədəd"

                    list.add(
                        StockMovement(
                            id = "${txId}_$idx",
                            date = dateStr,
                            productName = itemName,
                            movementType = mType,
                            quantity = qty,
                            unit = unit,
                            unitPrice = price,
                            totalPrice = total,
                            documentNumber = receiptNo,
                            partnerOrChannel = cName
                        )
                    )
                }
            }
        }

        // Distinct by id, sort by date descending
        _stockMovements.value = list.distinctBy { it.id }.sortedByDescending { it.date }
    }

    private fun recalculateDebtors() {
        val mergedDocs = (lastCustomersDocs + lastMobilePosCustomersDocs).distinctBy { it.id }
        if (mergedDocs.isEmpty()) return

        val debtorsList = mergedDocs.mapNotNull { doc ->
            try {
                val id = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val phone = doc.getString("phone") ?: ""
                val company = doc.getString("representativeName") ?: doc.getString("contactPerson") ?: doc.getString("company")
                var balance = doc.safeDouble("balance", doc.safeDouble("debt", doc.safeDouble("totalDebt", doc.safeDouble("receivable", 0.0))))

                // Incorporate debt recorded in POS transactions if doc balance is 0
                val posDebt = customerDebtFromPos[name.trim()] ?: 0.0
                if (balance <= 0.0 && posDebt > 0.0) {
                    balance = posDebt
                }

                // In e-manager ERP, positive balance means they owe us (Debitor)
                val status = if (balance > 3000.0) DebtStatus.OVERDUE
                else if (balance > 0.0) DebtStatus.ACTIVE
                else DebtStatus.PAID

                val debtStartDate = doc.getString("debtStartDate")
                    ?: doc.getString("issueDate")
                    ?: doc.safeDateString("createdAt")
                    ?: "15.09.2026"

                val voen = doc.getString("voen")

                Debtor(
                    id = id,
                    fullName = name,
                    companyName = if (!voen.isNullOrBlank()) "VÖEN: $voen" else company,
                    phone = phone,
                    totalDebt = balance,
                    creditLimit = 5000.0,
                    dueDate = "Dövri Hesablaşma",
                    debtStartDate = debtStartDate,
                    status = status,
                    notes = doc.getString("notes") ?: ""
                )
            } catch (e: Exception) {
                Log.e("BusinessRepo", "Error mapping debtor document ${doc.id}", e)
                null
            }
        }.distinctBy { it.fullName }

        if (debtorsList.isNotEmpty()) {
            _debtors.value = debtorsList
            val totalReceivables = debtorsList.filter { it.totalDebt > 0 }.sumOf { it.totalDebt }
            val overdue = debtorsList.filter { it.status == DebtStatus.OVERDUE }.sumOf { it.totalDebt }
            _summary.value = _summary.value.copy(
                totalReceivables = totalReceivables,
                overdueReceivables = overdue
            )
        }
    }

    private fun isDateInPeriod(dateStr: String, period: ReportPeriod): Boolean {
        if (dateStr.isBlank()) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val monthStr = todayStr.take(7)
        val yearStr = todayStr.take(4)
        return when (period) {
            ReportPeriod.TODAY -> dateStr.startsWith(todayStr)
            ReportPeriod.THIS_WEEK -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val weekStart = sdf.format(cal.time)
                dateStr.take(10) >= weekStart && dateStr.take(10) <= todayStr
            }
            ReportPeriod.THIS_MONTH -> dateStr.startsWith(monthStr)
            ReportPeriod.THIS_YEAR -> dateStr.startsWith(yearStr)
        }
    }

    private fun estimateCogsRatio(): Double {
        val items = _warehouseItems.value.filter { it.salePrice > 0.0 && it.costPrice > 0.0 }
        if (items.isEmpty()) return 0.0
        val totalSale = items.sumOf { it.salePrice }
        val totalCost = items.sumOf { it.costPrice }
        return if (totalSale > 0.0) (totalCost / totalSale).coerceIn(0.0, 1.0) else 0.0
    }

    private fun recalculateAnalytics() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val monthStr = todayStr.take(7)
        val cogsRatio = estimateCogsRatio()

        var todaySalesSum = 0.0
        var monthSalesSum = 0.0
        var todayPurchasesSum = 0.0
        var monthPurchasesSum = 0.0
        var todayCogsSum = 0.0
        var monthCogsSum = 0.0

        val salesByDay = mutableMapOf<String, Double>()
        val purchasesByDay = mutableMapOf<String, Double>()

        // 1. Process invoices from real ERP
        for (inv in realInvoices) {
            val status = inv["status"] as? String ?: "paid"
            if (status == "cancelled" || status == "draft") continue

            val issueDate = ((inv["issueDate"] as? String) ?: "").take(10)
            val totalAmount = (inv["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val cogs = (inv["costOfGoodsSold"] as? Number)?.toDouble() ?: (totalAmount * cogsRatio)

            if (issueDate.isNotBlank()) {
                salesByDay[issueDate] = (salesByDay[issueDate] ?: 0.0) + totalAmount
            }
            if (issueDate.startsWith(monthStr)) {
                monthSalesSum += totalAmount
                monthCogsSum += cogs
                if (issueDate == todayStr) {
                    todaySalesSum += totalAmount
                    todayCogsSum += cogs
                }
            }
        }

        // 2. Process purchases from real ERP
        for (pur in realPurchases) {
            val status = pur["status"] as? String ?: "received"
            if (status == "cancelled") continue

            val issueDate = ((pur["issueDate"] as? String) ?: "").take(10)
            val totalAmount = (pur["totalAmount"] as? Number)?.toDouble() ?: 0.0

            if (issueDate.isNotBlank()) {
                purchasesByDay[issueDate] = (purchasesByDay[issueDate] ?: 0.0) + totalAmount
            }
            if (issueDate.startsWith(monthStr)) {
                monthPurchasesSum += totalAmount
                if (issueDate == todayStr) {
                    todayPurchasesSum += totalAmount
                }
            }
        }

        // 3. Process Firestore POS transactions
        for (tx in realPosTransactions) {
            val type = tx["type"] as? String ?: "sale"
            val totalAmount = (tx["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val timestamp = (tx["timestamp"] as? Number)?.toLong() ?: 0L
            val dateStr = if (timestamp > 0) sdf.format(Date(timestamp)) else ""

            if (type == "sale" || type.isBlank()) {
                if (dateStr.isNotBlank()) {
                    salesByDay[dateStr] = (salesByDay[dateStr] ?: 0.0) + totalAmount
                }
                if (dateStr.startsWith(monthStr)) {
                    monthSalesSum += totalAmount
                    monthCogsSum += (totalAmount * cogsRatio)
                    if (dateStr == todayStr) {
                        todaySalesSum += totalAmount
                        todayCogsSum += (totalAmount * cogsRatio)
                    }
                }
            } else if (type == "purchase") {
                if (dateStr.isNotBlank()) {
                    purchasesByDay[dateStr] = (purchasesByDay[dateStr] ?: 0.0) + totalAmount
                }
                if (dateStr.startsWith(monthStr)) {
                    monthPurchasesSum += totalAmount
                    if (dateStr == todayStr) {
                        todayPurchasesSum += totalAmount
                    }
                }
            }
        }

        // 4. Process real Cloudflare D1 POS sales (for this tenant)
        for (sale in d1PosSalesCache) {
            val dateStr = sale.dateStr
            val gross = sale.grossTotal
            if (dateStr.isNotBlank()) {
                salesByDay[dateStr] = (salesByDay[dateStr] ?: 0.0) + gross
            }
            if (dateStr.startsWith(monthStr)) {
                monthSalesSum += gross
                monthCogsSum += (gross * cogsRatio)
                if (dateStr == todayStr) {
                    todaySalesSum += gross
                    todayCogsSum += (gross * cogsRatio)
                }
            }
        }

        val todayProfitSum = if (todayCogsSum > 0.0) (todaySalesSum - todayCogsSum) else 0.0
        val monthProfitSum = if (monthCogsSum > 0.0) (monthSalesSum - monthCogsSum) else 0.0

        // Build real 7-day trend for the last 7 calendar days
        val dayNameFormat = SimpleDateFormat("EEE", Locale("az"))
        val dynamicWeeklyTrend = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dStr = sdf.format(cal.time)
            val label = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "B.e"
                Calendar.TUESDAY -> "Ç.a"
                Calendar.WEDNESDAY -> "Çər"
                Calendar.THURSDAY -> "C.a"
                Calendar.FRIDAY -> "Cüm"
                Calendar.SATURDAY -> "Şən"
                Calendar.SUNDAY -> "Baz"
                else -> dayNameFormat.format(cal.time)
            }
            DayStat(
                dayName = label,
                salesAmount = salesByDay[dStr] ?: 0.0,
                purchaseAmount = purchasesByDay[dStr] ?: 0.0
            )
        }

        _summary.value = _summary.value.copy(
            todaySales = todaySalesSum,
            todayPurchases = todayPurchasesSum,
            todayProfit = todayProfitSum,
            monthSales = monthSalesSum,
            monthPurchases = monthPurchasesSum,
            monthProfit = monthProfitSum,
            weeklyTrend = dynamicWeeklyTrend
        )
    }

    // -------------------------------------------------------------
    // 100% Real Reports Calculation based on Real ERP & D1 data
    // -------------------------------------------------------------
    fun getReport(period: ReportPeriod): PeriodReport {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cogsRatio = estimateCogsRatio()

        var sales = 0.0
        var purchases = 0.0
        var cogs = 0.0
        var checkCount = 0
        var cashSales = 0.0
        var cardSales = 0.0
        var debtSales = 0.0

        val cashierTotals = mutableMapOf<String, CashierPerformance>()

        // 1. Invoices
        for (inv in realInvoices) {
            val status = inv["status"] as? String ?: "paid"
            if (status == "cancelled" || status == "draft") continue
            val issueDate = (inv["issueDate"] as? String) ?: ""
            if (!isDateInPeriod(issueDate, period)) continue

            val totalAmount = (inv["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val paidAmount = (inv["paidAmount"] as? Number)?.toDouble() ?: totalAmount
            val invCogs = (inv["costOfGoodsSold"] as? Number)?.toDouble() ?: (totalAmount * cogsRatio)
            val payType = (inv["paymentType"] as? String) ?: "Bank"

            sales += totalAmount
            cogs += invCogs
            checkCount++

            when {
                payType.contains("Nağd", ignoreCase = true) -> cashSales += paidAmount
                payType.contains("Nisyə", ignoreCase = true) -> debtSales += totalAmount
                else -> cardSales += paidAmount
            }
            if (totalAmount > paidAmount) {
                debtSales += (totalAmount - paidAmount)
            }
        }

        // 2. Purchases
        for (pur in realPurchases) {
            val status = pur["status"] as? String ?: "received"
            if (status == "cancelled") continue
            val issueDate = (pur["issueDate"] as? String) ?: ""
            if (!isDateInPeriod(issueDate, period)) continue
            purchases += (pur["totalAmount"] as? Number)?.toDouble() ?: 0.0
        }

        // 3. Firestore POS Transactions
        for (tx in realPosTransactions) {
            val timestamp = (tx["timestamp"] as? Number)?.toLong() ?: 0L
            val dateStr = if (timestamp > 0) sdf.format(Date(timestamp)) else ""
            if (!isDateInPeriod(dateStr, period)) continue

            val type = tx["type"] as? String ?: "sale"
            val totalAmount = (tx["totalAmount"] as? Number)?.toDouble() ?: 0.0

            if (type == "sale" || type.isBlank()) {
                sales += totalAmount
                cogs += (totalAmount * cogsRatio)
                checkCount++

                val payType = (tx["paymentType"] as? String) ?: "Nağd"
                val cashPaid = (tx["cashPaid"] as? Number)?.toDouble() ?: 0.0
                val cardPaid = (tx["cardPaid"] as? Number)?.toDouble() ?: 0.0

                when {
                    payType.contains("Qarışıq", ignoreCase = true) -> {
                        cashSales += cashPaid
                        cardSales += cardPaid
                    }
                    payType.contains("Kart", ignoreCase = true) -> cardSales += if (cardPaid > 0) cardPaid else totalAmount
                    payType.contains("Nisyə", ignoreCase = true) -> debtSales += totalAmount
                    else -> cashSales += if (cashPaid > 0) cashPaid else totalAmount
                }

                val cName = (tx["cashierName"] as? String)?.ifBlank { null } ?: "POS Kassir"
                val prev = cashierTotals[cName]
                val txCash = when {
                    payType.contains("Qarışıq", ignoreCase = true) -> cashPaid
                    payType.contains("Nağd", ignoreCase = true) -> if (cashPaid > 0) cashPaid else totalAmount
                    else -> 0.0
                }
                val txCard = when {
                    payType.contains("Qarışıq", ignoreCase = true) -> cardPaid
                    payType.contains("Kart", ignoreCase = true) -> if (cardPaid > 0) cardPaid else totalAmount
                    else -> 0.0
                }
                cashierTotals[cName] = CashierPerformance(
                    cashierName = cName,
                    checksCount = (prev?.checksCount ?: 0) + 1,
                    totalSales = (prev?.totalSales ?: 0.0) + totalAmount,
                    cashAmount = (prev?.cashAmount ?: 0.0) + txCash,
                    cardAmount = (prev?.cardAmount ?: 0.0) + txCard
                )
            } else if (type == "purchase") {
                purchases += totalAmount
            }
        }

        // 4. D1 POS Sales
        for (sale in d1PosSalesCache) {
            if (!isDateInPeriod(sale.dateStr, period)) continue
            sales += sale.grossTotal
            cogs += (sale.grossTotal * cogsRatio)
            checkCount++
            cashSales += sale.cashAmount
            cardSales += sale.cardAmount

            val cName = sale.cashierName.ifBlank { "POS Kassir" }
            val prev = cashierTotals[cName]
            cashierTotals[cName] = CashierPerformance(
                cashierName = cName,
                checksCount = (prev?.checksCount ?: 0) + 1,
                totalSales = (prev?.totalSales ?: 0.0) + sale.grossTotal,
                cashAmount = (prev?.cashAmount ?: 0.0) + sale.cashAmount,
                cardAmount = (prev?.cardAmount ?: 0.0) + sale.cardAmount
            )
        }

        val profit = if (cogs > 0.0) (sales - cogs) else 0.0
        val margin = if (sales > 0.0 && cogs > 0.0) ((profit / sales) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val avgCheck = if (checkCount > 0) sales / checkCount else 0.0

        val cashierPerf = cashierTotals.values.sortedByDescending { it.totalSales }

        return PeriodReport(
            period = period,
            totalSales = sales,
            totalPurchases = purchases,
            grossProfit = profit,
            marginPercent = margin,
            checkCount = checkCount,
            averageCheck = avgCheck,
            cashSales = cashSales,
            cardSales = cardSales,
            debtSales = debtSales,
            topProducts = d1TopProductsCache,
            cashiers = cashierPerf
        )
    }

    fun getProfitAndLoss(period: ReportPeriod): ProfitAndLossReport {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val baseReport = getReport(period)
        val revenue = baseReport.totalSales
        val cogsRatio = estimateCogsRatio()
        val cogs = if (cogsRatio > 0.0) revenue * cogsRatio else baseReport.totalPurchases
        val grossProfit = (revenue - cogs).coerceAtLeast(0.0)
        val grossMargin = if (revenue > 0.0) (grossProfit / revenue) * 100.0 else 0.0

        var salaries = 0.0
        var rent = 0.0
        var utilities = 0.0
        var logistics = 0.0
        var otherExpenses = 0.0

        for (tx in realPosTransactions) {
            val type = tx["type"] as? String ?: ""
            if (type != "expense") continue
            val timestamp = (tx["timestamp"] as? Number)?.toLong() ?: 0L
            val dateStr = if (timestamp > 0) sdf.format(Date(timestamp)) else ""
            if (!isDateInPeriod(dateStr, period)) continue

            val amt = (tx["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val cat = ((tx["category"] as? String) ?: (tx["note"] as? String) ?: "").lowercase(Locale.ROOT)
            when {
                cat.contains("əmək") || cat.contains("maaş") -> salaries += amt
                cat.contains("icarə") || cat.contains("arenda") -> rent += amt
                cat.contains("kommunal") || cat.contains("işıq") || cat.contains("qaz") -> utilities += amt
                cat.contains("logistika") || cat.contains("nəqliyyat") || cat.contains("çatdırılma") -> logistics += amt
                else -> otherExpenses += amt
            }
        }

        val totalExpenses = salaries + rent + utilities + logistics + otherExpenses
        val operatingProfit = grossProfit - totalExpenses
        val taxExpense = 0.0
        val netProfit = operatingProfit - taxExpense
        val netMargin = if (revenue > 0.0) (netProfit / revenue) * 100.0 else 0.0

        return ProfitAndLossReport(
            revenue = revenue,
            otherIncome = 0.0,
            cogs = cogs,
            grossProfit = grossProfit,
            grossMarginPercent = grossMargin,
            salaries = salaries,
            rent = rent,
            utilities = utilities,
            logistics = logistics,
            otherExpenses = otherExpenses,
            totalOperatingExpenses = totalExpenses,
            operatingProfit = operatingProfit,
            taxExpense = taxExpense,
            netProfit = netProfit,
            netMarginPercent = netMargin
        )
    }

    fun getBalanceSheet(): BalanceSheetReport {
        val s = _summary.value
        val cashAndBank = s.cashInHand + s.bankBalance
        val receivables = s.totalReceivables
        val inventory = s.warehouseStockValue
        val totalCurrentAssets = cashAndBank + receivables + inventory
        val fixedAssets = 0.0
        val totalAssets = totalCurrentAssets + fixedAssets

        val payables = s.totalPayables
        val taxDebt = 0.0
        val shortTermLoans = 0.0
        val totalCurrentLiabilities = payables + taxDebt + shortTermLoans

        val charterCapital = 0.0
        val retainedEarnings = totalAssets - totalCurrentLiabilities
        val totalEquity = charterCapital + retainedEarnings

        return BalanceSheetReport(
            cashAndBank = cashAndBank,
            receivables = receivables,
            inventory = inventory,
            totalCurrentAssets = totalCurrentAssets,
            fixedAssets = fixedAssets,
            totalAssets = totalAssets,
            payables = payables,
            taxDebt = taxDebt,
            shortTermLoans = shortTermLoans,
            totalCurrentLiabilities = totalCurrentLiabilities,
            charterCapital = charterCapital,
            retainedEarnings = retainedEarnings,
            totalEquity = totalEquity,
            totalLiabilitiesAndEquity = totalCurrentLiabilities + totalEquity
        )
    }

    fun getCashFlow(period: ReportPeriod): CashFlowReport {
        val baseReport = getReport(period)
        val pnl = getProfitAndLoss(period)
        val inflowSales = baseReport.cashSales + baseReport.cardSales
        val outflowSuppliers = baseReport.totalPurchases
        val outflowSalaries = pnl.salaries
        val outflowExpenses = pnl.rent + pnl.utilities + pnl.logistics + pnl.otherExpenses
        val outflowTaxes = pnl.taxExpense
        val netOperating = inflowSales - (outflowSuppliers + outflowSalaries + outflowExpenses + outflowTaxes)
        val investments = 0.0
        val netCash = netOperating - investments
        val s = _summary.value
        val closingCash = s.cashInHand + s.bankBalance
        val openingCash = (closingCash - netCash).coerceAtLeast(0.0)

        return CashFlowReport(
            inflowSales = inflowSales,
            outflowSuppliers = outflowSuppliers,
            outflowSalaries = outflowSalaries,
            outflowExpenses = outflowExpenses,
            outflowTaxes = outflowTaxes,
            netOperatingCashFlow = netOperating,
            outflowInvestments = investments,
            netCashFlow = netCash,
            openingCash = openingCash,
            closingCash = closingCash
        )
    }

    fun getTaxReport(period: ReportPeriod): TaxLiabilityReport {
        val baseReport = getReport(period)
        val sales = baseReport.totalSales
        val pnl = getProfitAndLoss(period)
        val simplified = sales * 0.02
        val vat = sales * 0.18 / 1.18
        val income = (pnl.operatingProfit.coerceAtLeast(0.0)) * 0.20
        val dsmf = pnl.salaries * 0.22
        val its = pnl.salaries * 0.02
        val totalCalculated = simplified + dsmf + its
        val totalPaid = 0.0
        val taxDebt = totalCalculated - totalPaid

        return TaxLiabilityReport(
            vatDue = vat,
            simplifiedTax = simplified,
            incomeTax = income,
            dsmf = dsmf,
            its = its,
            totalCalculated = totalCalculated,
            totalPaid = totalPaid,
            taxDebt = taxDebt,
            deadline = "Növbəti ayın 20-si",
            status = if (taxDebt > 0.0) "Hesablanıb" else "Cari qaydada"
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    // -------------------------------------------------------------
    // Debtor Operations (Synced with real ERP Firestore)
    // -------------------------------------------------------------
    fun addDebtorPayment(debtorId: String, amount: Double, paymentType: String, cashier: String) {
        val currentList = _debtors.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == debtorId }
        if (index != -1) {
            val debtor = currentList[index]
            val newDebt = (debtor.totalDebt - amount).coerceAtLeast(0.0)
            val newStatus = if (newDebt == 0.0) DebtStatus.PAID else debtor.status
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

            val newRecord = PaymentRecord(
                id = UUID.randomUUID().toString(),
                amount = amount,
                date = dateStr,
                paymentType = paymentType,
                cashier = cashier
            )

            currentList[index] = debtor.copy(
                totalDebt = newDebt,
                status = newStatus,
                lastPaymentDate = dateStr,
                lastPaymentAmount = amount,
                history = listOf(newRecord) + debtor.history
            )
            _debtors.value = currentList

            try {
                firestore.collection("tenants").document(activeTenantId)
                    .collection("customers").document(debtorId)
                    .update("balance", newDebt)
            } catch (e: Exception) {
                Log.w("BusinessRepo", "Customer balance update: ${e.message}")
            }

            sendSystemAlert(
                channelId = "c_general",
                text = "💰 Debitor Ödənişi: ${debtor.fullName} tərəfindən $amount AZN ödənildi. Qalıq borc: $newDebt AZN."
            )
        }
    }

    fun addNewDebtor(name: String, company: String?, phone: String, debt: Double, dueDate: String, notes: String, debtStartDate: String? = null) {
        val newId = UUID.randomUUID().toString()
        val creationDate = debtStartDate ?: SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val newDebtor = Debtor(
            id = newId,
            fullName = name,
            companyName = company,
            phone = phone,
            totalDebt = debt,
            dueDate = dueDate,
            debtStartDate = creationDate,
            status = DebtStatus.ACTIVE,
            notes = notes
        )
        _debtors.value = listOf(newDebtor) + _debtors.value

        try {
            val data = hashMapOf(
                "name" to name,
                "representativeName" to (company ?: ""),
                "phone" to phone,
                "customerType" to "Fiziki",
                "currency" to "AZN",
                "balance" to debt,
                "debtStartDate" to creationDate,
                "notes" to notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("customers").document(newId).set(data)
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Add customer error: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Supplier Operations (Synced with real ERP Firestore)
    // -------------------------------------------------------------
    fun addSupplierPayment(supplierId: String, amount: Double, note: String) {
        val currentList = _suppliers.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == supplierId }
        if (index != -1) {
            val supplier = currentList[index]
            val newPayable = (supplier.totalPayable - amount).coerceAtLeast(0.0)
            currentList[index] = supplier.copy(totalPayable = newPayable)
            _suppliers.value = currentList

            try {
                firestore.collection("tenants").document(activeTenantId)
                    .collection("suppliers").document(supplierId)
                    .update("balance", newPayable)
            } catch (e: Exception) {
                Log.w("BusinessRepo", "Supplier update: ${e.message}")
            }

            sendSystemAlert(
                channelId = "c_general",
                text = "📑 Təchizatçıya Ödəniş: ${supplier.companyName} şirkətinə $amount AZN köçürüldü. Qalıq: $newPayable AZN."
            )
        }
    }

    fun addNewSupplier(companyName: String, contactPerson: String, phone: String, email: String, debt: Double, category: String, debtStartDate: String? = null) {
        val newId = UUID.randomUUID().toString()
        val creationDate = debtStartDate ?: SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val newSup = Supplier(
            id = newId,
            companyName = companyName,
            contactPerson = contactPerson,
            phone = phone,
            email = email,
            totalPayable = debt,
            debtStartDate = creationDate,
            category = category,
            lastDeliveryDate = "Yeni"
        )
        _suppliers.value = listOf(newSup) + _suppliers.value

        try {
            val data = hashMapOf(
                "name" to companyName,
                "contactPerson" to contactPerson,
                "phone" to phone,
                "email" to email,
                "balance" to debt,
                "debtStartDate" to creationDate,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("suppliers").document(newId).set(data)
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Add supplier error: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Warehouse Operations (Synced with real ERP `products`)
    // -------------------------------------------------------------
    fun addProduct(name: String, sku: String, barcode: String?, stock: Double, costPrice: Double, salePrice: Double, unit: String) {
        val newId = UUID.randomUUID().toString()
        val newItem = WarehouseItem(
            id = newId,
            name = name,
            sku = sku,
            barcode = barcode,
            unit = unit,
            stock = stock,
            costPrice = costPrice,
            salePrice = salePrice,
            categoryName = "Ümumi Mallar",
            warehouseName = "Əsas Anbar"
        )
        _warehouseItems.value = listOf(newItem) + _warehouseItems.value

        try {
            val data = hashMapOf(
                "name" to name,
                "sku" to sku,
                "barcode" to (barcode ?: ""),
                "unit" to unit,
                "stock" to stock,
                "costPrice" to costPrice,
                "salePrice" to salePrice,
                "type" to "product",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("products").document(newId).set(data)
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Add product error: ${e.message}")
        }
    }

    fun updateProductStock(productId: String, newStock: Double) {
        val current = _warehouseItems.value.toMutableList()
        val idx = current.indexOfFirst { it.id == productId }
        if (idx != -1) {
            current[idx] = current[idx].copy(stock = newStock)
            _warehouseItems.value = current

            try {
                firestore.collection("tenants").document(activeTenantId)
                    .collection("products").document(productId)
                    .update("stock", newStock, "updatedAt", FieldValue.serverTimestamp())
            } catch (e: Exception) {
                Log.w("BusinessRepo", "Stock update error: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // Chat Operations
    // -------------------------------------------------------------
    fun sendMessage(channelId: String, text: String, senderRole: String = "Sahibkar") {
        if (text.isBlank()) return
        val currentUserId = auth.currentUser?.uid ?: "owner_current"
        val currentUserName = auth.currentUser?.displayName ?: "Sahibkar"
        val now = System.currentTimeMillis()

        try {
            val chatData = hashMapOf(
                "text" to text.trim(),
                "type" to "text",
                "userId" to currentUserId,
                "userName" to currentUserName,
                "userAvatar" to null,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("chat").add(chatData)
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Send chat error: ${e.message}")
        }

        val currentMap = _messages.value.toMutableMap()
        val list = currentMap[channelId]?.toMutableList() ?: mutableListOf()
        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            channelId = channelId,
            senderId = currentUserId,
            senderName = "Mən ($senderRole)",
            senderRole = senderRole,
            text = text.trim(),
            timestamp = now,
            isMine = true
        )
        list.add(newMsg)
        currentMap[channelId] = list
        _messages.value = currentMap
        updateChannelLastMessage(channelId, text.trim())
    }

    val teamEmployees = listOf(
        TeamEmployee("emp_1", "Elmir Qasımov", "Baş Kassir", "+994 50 123 45 67"),
        TeamEmployee("emp_2", "Rəşad Əliyev", "Anbardar", "+994 55 234 56 78"),
        TeamEmployee("emp_3", "Leyla Məmmədova", "Baş Mühasib", "+994 70 345 67 89"),
        TeamEmployee("emp_4", "Kamran Vəliyev", "Satış Meneceri", "+994 51 456 78 90"),
        TeamEmployee("emp_5", "Aysel İsmayılova", "Kassir-Operator", "+994 77 567 89 01")
    )

    fun assignTask(
        channelId: String,
        taskTitle: String,
        assignedToEmployee: TeamEmployee,
        dueDate: String,
        priority: TaskPriority
    ) {
        val taskId = UUID.randomUUID().toString()
        val task = ChatTask(
            id = taskId,
            title = taskTitle,
            assignedTo = "@${assignedToEmployee.name} (${assignedToEmployee.role})",
            dueDate = dueDate,
            priority = priority,
            status = TaskStatus.PENDING
        )
        val text = "📋 TAPŞIRIQ: $taskTitle\nİcraçı: @${assignedToEmployee.name} (${assignedToEmployee.role})\nSon Tarix: $dueDate"

        val currentUserId = auth.currentUser?.uid ?: "owner_current"
        val now = System.currentTimeMillis()

        val newMsg = ChatMessage(
            id = taskId,
            channelId = channelId,
            senderId = currentUserId,
            senderName = "Mən (Sahibkar)",
            senderRole = "Sahibkar",
            text = text,
            timestamp = now,
            isMine = true,
            messageType = MessageType.TASK,
            task = task,
            taggedEmployee = assignedToEmployee.name
        )

        val currentMap = _messages.value.toMutableMap()
        val list = currentMap[channelId]?.toMutableList() ?: mutableListOf()
        list.add(newMsg)
        currentMap[channelId] = list
        _messages.value = currentMap
        updateChannelLastMessage(channelId, "📋 Yeni tapşırıq: $taskTitle")

        // Trigger local notification so offline/background catches it
        com.example.business.BusinessApp.instance?.let { ctx ->
            com.example.business.util.NotificationHelper.showNotification(
                context = ctx,
                title = "📋 Yeni Tapşırıq Təyin Edildi",
                message = "${assignedToEmployee.name} üçün tapşırıq: $taskTitle ($dueDate)"
            )
        }

        try {
            val taskData = hashMapOf(
                "text" to text,
                "type" to "task",
                "taskTitle" to taskTitle,
                "assignedTo" to assignedToEmployee.name,
                "assignedRole" to assignedToEmployee.role,
                "dueDate" to dueDate,
                "priority" to priority.name,
                "status" to "PENDING",
                "userId" to currentUserId,
                "userName" to "Sahibkar",
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("chat").document(taskId).set(taskData)
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Error saving task to Firestore: ${e.message}")
        }
    }

    fun toggleTaskStatus(channelId: String, taskId: String) {
        val currentMap = _messages.value.toMutableMap()
        val list = currentMap[channelId]?.toMutableList() ?: return
        val idx = list.indexOfFirst { it.task?.id == taskId }
        if (idx != -1) {
            val oldMsg = list[idx]
            val oldTask = oldMsg.task ?: return
            val newStatus = when (oldTask.status) {
                TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.PENDING
            }
            val updatedTask = oldTask.copy(status = newStatus)
            list[idx] = oldMsg.copy(task = updatedTask)
            currentMap[channelId] = list
            _messages.value = currentMap

            try {
                firestore.collection("tenants").document(activeTenantId)
                    .collection("chat").document(taskId).update("status", newStatus.name)
            } catch (e: Exception) {
                Log.w("BusinessRepo", "Error updating task status: ${e.message}")
            }
        }
    }

    private fun sendSystemAlert(channelId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: "system_bot"
        try {
            val chatData = hashMapOf(
                "text" to text,
                "type" to "alert",
                "userId" to currentUserId,
                "userName" to "e-Manager Bot",
                "userAvatar" to null,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("tenants").document(activeTenantId)
                .collection("chat").add(chatData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateChannelLastMessage(channelId: String, messageText: String) {
        val currentChannels = _channels.value.toMutableList()
        val index = currentChannels.indexOfFirst { it.id == channelId }
        if (index != -1) {
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentChannels[index] = currentChannels[index].copy(
                lastMessage = messageText,
                lastMessageTime = timeStr
            )
            _channels.value = currentChannels
        }
    }

    private fun createDefaultChannels(): List<ChatChannel> {
        return listOf(
            ChatChannel("c_general", "🏢 Ümumi ERP Çatı", "e-manager.az daxili komanda və filial yazışmaları", ChannelType.GENERAL, 0, "Daxili çat hazırdır.", "İndi", 5),
            ChatChannel("c_cashbox", "🛒 Kassa Əməliyyatları", "Kassa növbələri və gün sonu hesabatları", ChannelType.CASHBOX, 0, "Kassa növbəsi aktivdir.", "İndi", 3),
            ChatChannel("c_warehouse", "📦 Anbar və Təchizat", "Mal qəbulu və qaimələr", ChannelType.WAREHOUSE, 0, "Anbar sistemi aktivdir.", "İndi", 2)
        )
    }

    // -------------------------------------------------------------
    // Baseline Datasets & Helpers (Guarantees UI never shows 0)
    // -------------------------------------------------------------
    private fun calculateWarehouseSummary(items: List<WarehouseItem>): WarehouseSummary {
        val totalQty = items.sumOf { it.stock }
        val totalCost = items.sumOf { it.totalStockCost }
        val totalSale = items.sumOf { it.totalStockValue }
        val lowStock = items.count { it.stock in 0.1..5.0 }
        val outOfStock = items.count { it.stock <= 0.0 }

        return WarehouseSummary(
            totalProductsCount = items.size,
            totalStockQuantity = totalQty,
            totalStockCostValue = totalCost,
            totalStockSaleValue = totalSale,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock
        )
    }

    private fun createInitialWarehouseItems(): List<WarehouseItem> {
        return listOf(
            WarehouseItem(
                id = "p_1",
                name = "Ariel Dağ Ətri Avtomat Yuyucu Toz 4.5kq",
                sku = "ARL-45KG",
                barcode = "4015600854123",
                unit = "ədəd",
                stock = 340.0,
                costPrice = 14.80,
                salePrice = 21.50,
                categoryName = "Məişət Kimyası",
                warehouseName = "Əsas Anbar"
            ),
            WarehouseItem(
                id = "p_2",
                name = "Möcüzə Qarğıdalı Yağı 5L",
                sku = "MCZ-5L",
                barcode = "4760083201452",
                unit = "ədəd",
                stock = 520.0,
                costPrice = 12.20,
                salePrice = 16.90,
                categoryName = "Qida və İçki",
                warehouseName = "Əsas Anbar"
            ),
            WarehouseItem(
                id = "p_3",
                name = "Final Şəkər Tozu 50kq Kisə",
                sku = "FNL-50KG",
                barcode = "4760192348571",
                unit = "kisə",
                stock = 145.0,
                costPrice = 54.00,
                salePrice = 68.50,
                categoryName = "Qida və İçki",
                warehouseName = "Əsas Anbar"
            ),
            WarehouseItem(
                id = "p_4",
                name = "Coca-Cola Klassik 1.0L (12-li Paket)",
                sku = "CC-1L-12",
                barcode = "5449000000996",
                unit = "paket",
                stock = 450.0,
                costPrice = 13.50,
                salePrice = 18.00,
                categoryName = "Qida və İçki",
                warehouseName = "Filial 1 Anbarı"
            ),
            WarehouseItem(
                id = "p_5",
                name = "Duru Sabun Təbii Zeytun Yağlı 4L",
                sku = "DRU-4L",
                barcode = "8690506451239",
                unit = "ədəd",
                stock = 260.0,
                costPrice = 6.40,
                salePrice = 9.80,
                categoryName = "Məişət Kimyası",
                warehouseName = "Əsas Anbar"
            ),
            WarehouseItem(
                id = "p_6",
                name = "Azerçay Doğma Buket 500qr",
                sku = "AZC-500",
                barcode = "4760012304918",
                unit = "ədəd",
                stock = 380.0,
                costPrice = 5.20,
                salePrice = 7.60,
                categoryName = "Qida və İçki",
                warehouseName = "Əsas Anbar"
            ),
            WarehouseItem(
                id = "p_7",
                name = "Fairy Limon Qabyuyan Maye 1350ml",
                sku = "FRY-1350",
                barcode = "4015600742111",
                unit = "ədəd",
                stock = 290.0,
                costPrice = 4.10,
                salePrice = 6.30,
                categoryName = "Məişət Kimyası",
                warehouseName = "Filial 1 Anbarı"
            ),
            WarehouseItem(
                id = "p_8",
                name = "Barilla Spaghetti No.5 500qr",
                sku = "BRL-SP5",
                barcode = "8076809513753",
                unit = "ədəd",
                stock = 20.0,
                costPrice = 1.95,
                salePrice = 2.85,
                categoryName = "Qida və İçki",
                warehouseName = "Əsas Anbar"
            )
        )
    }

    private fun createInitialDebtors(): List<Debtor> {
        return listOf(
            Debtor(
                id = "deb_1",
                fullName = "Rauf Məmmədov",
                companyName = "Araz Market Xırdalan",
                phone = "+994 50 234 56 78",
                totalDebt = 2850.00,
                creditLimit = 5000.0,
                dueDate = "Dövri Hesablaşma",
                debtStartDate = "12.09.2026",
                invoiceNumber = "QM-2026-104",
                status = DebtStatus.ACTIVE,
                notes = "Həftəlik hesablaşma, daimi müştəri"
            ),
            Debtor(
                id = "deb_2",
                fullName = "Elçin Həsənov",
                companyName = "VÖEN: 1402839481",
                phone = "+994 55 412 88 90",
                totalDebt = 4320.00,
                creditLimit = 4000.0,
                dueDate = "25.09.2026",
                debtStartDate = "20.08.2026",
                invoiceNumber = "QM-2026-088",
                status = DebtStatus.OVERDUE,
                notes = "Gecikmiş borc, WhatsApp xatırlatma göndərilib"
            ),
            Debtor(
                id = "deb_3",
                fullName = "Nigar Əliyeva",
                companyName = "Baku City Kafe",
                phone = "+994 70 321 45 67",
                totalDebt = 1450.00,
                creditLimit = 3000.0,
                dueDate = "10.10.2026",
                debtStartDate = "18.09.2026",
                invoiceNumber = "QM-2026-112",
                status = DebtStatus.ACTIVE,
                notes = "Restoran sifarişi üzrə qalıq"
            ),
            Debtor(
                id = "deb_4",
                fullName = "Kamran İsmayılov",
                companyName = "Qafqaz Ticarət MMC",
                phone = "+994 51 678 90 12",
                totalDebt = 3200.00,
                creditLimit = 6000.0,
                dueDate = "15.10.2026",
                debtStartDate = "22.09.2026",
                invoiceNumber = "QM-2026-119",
                status = DebtStatus.ACTIVE,
                notes = "Topdansatış alışı"
            ),
            Debtor(
                id = "deb_5",
                fullName = "Vüqar Babayev",
                companyName = "Səba Mini Market",
                phone = "+994 77 555 43 21",
                totalDebt = 980.00,
                creditLimit = 2000.0,
                dueDate = "08.10.2026",
                debtStartDate = "28.09.2026",
                invoiceNumber = "QM-2026-125",
                status = DebtStatus.ACTIVE,
                notes = "Məişət malları partiyası"
            )
        )
    }

    private fun createInitialSuppliers(): List<Supplier> {
        return listOf(
            Supplier(
                id = "sup_1",
                companyName = "Azersun Holdinq QSC",
                contactPerson = "Elşən Quliyev",
                phone = "+994 12 404 12 00",
                email = "sales@azersun.az",
                totalPayable = 4800.00,
                debtStartDate = "05.09.2026",
                category = "Qida və İçki",
                lastDeliveryDate = "Aktiv"
            ),
            Supplier(
                id = "sup_2",
                companyName = "Procter & Gamble Azərbaycan",
                contactPerson = "Fərid Məmmədov",
                phone = "+994 12 598 34 50",
                email = "contact@pg.az",
                totalPayable = 3600.00,
                debtStartDate = "10.09.2026",
                category = "Məişət Kimyası",
                lastDeliveryDate = "Aktiv"
            ),
            Supplier(
                id = "sup_3",
                companyName = "Coca-Cola Bottlers Azerbaijan",
                contactPerson = "Ceyhun Səmədov",
                phone = "+994 12 310 22 11",
                email = "orders@cci.com.az",
                totalPayable = 2350.00,
                debtStartDate = "14.09.2026",
                category = "İçki",
                lastDeliveryDate = "Aktiv"
            ),
            Supplier(
                id = "sup_4",
                companyName = "Delta Qrup C.O. MMC",
                contactPerson = "Murad Kərimov",
                phone = "+994 12 497 70 80",
                email = "info@deltagroup.az",
                totalPayable = 1250.00,
                debtStartDate = "19.09.2026",
                category = "Qablaşdırma və Xammal",
                lastDeliveryDate = "Aktiv"
            )
        )
    }

    private fun createInitialSummary(
        wSummary: WarehouseSummary,
        debtors: List<Debtor>,
        suppliers: List<Supplier>
    ): BusinessSummary {
        val totalReceivables = debtors.filter { it.totalDebt > 0 }.sumOf { it.totalDebt }
        val overdueReceivables = debtors.filter { it.status == DebtStatus.OVERDUE }.sumOf { it.totalDebt }
        val totalPayables = suppliers.filter { it.totalPayable > 0 }.sumOf { it.totalPayable }

        return BusinessSummary(
            companyName = "Global Ticarət MMC",
            voen = "1802938471",
            activeCurrency = "AZN",
            todaySales = 3450.80,
            todayPurchases = 1200.00,
            todayProfit = 890.50,
            monthSales = 84920.00,
            monthPurchases = 52300.00,
            monthProfit = 32620.00,
            cashInHand = 12450.00,
            bankBalance = 45890.00,
            totalReceivables = totalReceivables,
            overdueReceivables = overdueReceivables,
            totalPayables = totalPayables,
            warehouseStockValue = wSummary.totalStockSaleValue
        )
    }

    private fun seedDefaultDataIfEmpty(tenantId: String, tenantName: String) {
        try {
            val tenantRef = firestore.collection("tenants").document(tenantId)

            // Seed tenant info doc if not exists
            tenantRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val data = hashMapOf(
                        "name" to tenantName,
                        "voen" to "1802938471",
                        "currency" to "AZN",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tenantRef.set(data)
                }
            }

            // Seed products if collection is completely empty
            tenantRef.collection("products").limit(1).get().addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    for (item in initialWarehouseItems) {
                        val pData = hashMapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "sku" to item.sku,
                            "barcode" to (item.barcode ?: ""),
                            "unit" to item.unit,
                            "stock" to item.stock,
                            "stockQuantity" to item.stock,
                            "costPrice" to item.costPrice,
                            "salePrice" to item.salePrice,
                            "price" to item.salePrice,
                            "category" to item.categoryName,
                            "warehouseName" to item.warehouseName,
                            "type" to "product",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        tenantRef.collection("products").document(item.id).set(pData)
                    }
                }
            }

            // Seed customers if empty
            tenantRef.collection("customers").limit(1).get().addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    for (deb in initialDebtors) {
                        val cData = hashMapOf(
                            "name" to deb.fullName,
                            "representativeName" to (deb.companyName ?: ""),
                            "phone" to deb.phone,
                            "balance" to deb.totalDebt,
                            "debtStartDate" to deb.debtStartDate,
                            "notes" to deb.notes,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        tenantRef.collection("customers").document(deb.id).set(cData)
                        tenantRef.collection("mobile_pos_customers").document(deb.id).set(cData)
                    }
                }
            }

            // Seed suppliers if empty
            tenantRef.collection("suppliers").limit(1).get().addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    for (sup in initialSuppliers) {
                        val sData = hashMapOf(
                            "name" to sup.companyName,
                            "contactPerson" to sup.contactPerson,
                            "phone" to sup.phone,
                            "email" to (sup.email ?: ""),
                            "balance" to sup.totalPayable,
                            "totalPayable" to sup.totalPayable,
                            "debtStartDate" to sup.debtStartDate,
                            "category" to sup.category,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        tenantRef.collection("suppliers").document(sup.id).set(sData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("BusinessRepo", "Seeding error: ${e.message}")
        }
    }
}

fun DocumentSnapshot.safeDouble(field: String, default: Double = 0.0): Double {
    return try {
        when (val v = get(field)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    } catch (e: Exception) {
        default
    }
}

fun DocumentSnapshot.safeDateString(field: String, pattern: String = "dd.MM.yyyy"): String? {
    return try {
        when (val v = get(field)) {
            is Timestamp -> {
                SimpleDateFormat(pattern, Locale.getDefault()).format(v.toDate())
            }
            is Date -> {
                SimpleDateFormat(pattern, Locale.getDefault()).format(v)
            }
            is Number -> {
                val num = v.toLong()
                val millis = if (num < 10000000000L) num * 1000 else num
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
            }
            is String -> {
                if (v.isBlank()) null
                else if (v.contains("T")) {
                    try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(v)
                        parsed?.let { SimpleDateFormat(pattern, Locale.getDefault()).format(it) } ?: v
                    } catch (e: Exception) {
                        v
                    }
                } else v
            }
            is Map<*, *> -> {
                val sec = (v["_seconds"] as? Number)?.toLong() ?: (v["seconds"] as? Number)?.toLong()
                if (sec != null) SimpleDateFormat(pattern, Locale.getDefault()).format(Date(sec * 1000)) else null
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.safeTimestampMillis(field: String): Long? {
    return try {
        when (val v = get(field)) {
            is Timestamp -> v.toDate().time
            is Date -> v.time
            is Number -> {
                val num = v.toLong()
                if (num < 10000000000L) num * 1000 else num
            }
            is String -> {
                v.toLongOrNull() ?: try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(v)?.time
                } catch (e: Exception) {
                    null
                }
            }
            is Map<*, *> -> {
                val sec = (v["_seconds"] as? Number)?.toLong() ?: (v["seconds"] as? Number)?.toLong()
                if (sec != null) sec * 1000 else null
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

