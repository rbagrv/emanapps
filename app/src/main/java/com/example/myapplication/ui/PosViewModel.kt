package com.example.myapplication.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CartItem
import com.example.myapplication.data.CashOperation
import com.example.myapplication.data.Customer
import com.example.myapplication.data.HeldOrder
import com.example.myapplication.data.PosRepository
import com.example.myapplication.data.Product
import com.example.myapplication.data.ReceiptData
import com.example.myapplication.data.Supplier
import com.example.myapplication.data.TransactionRecord
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.api.LoginRequest
import com.example.myapplication.data.api.SaleItem
import com.example.myapplication.data.api.SaleRequest
import com.example.myapplication.data.api.SaleResponse
import com.example.myapplication.data.api.Tenant
import com.example.myapplication.data.db.PosDatabase
import com.example.myapplication.data.db.ProductEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppStage {
    SPLASH,
    AUTH,
    TENANT_SELECT,
    PIN_SETUP,
    PIN_LOCK,
    MAIN_POS
}

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PosDatabase.getDatabase(application)
    private val repository = PosRepository(database)
    private val apiService = ApiClient.erpApiService
    private val prefs = application.getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()

    // Firestore Listeners
    private var transactionsListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var customersListener: ListenerRegistration? = null
    private var suppliersListener: ListenerRegistration? = null

    // Base Products & Transactions
    private val _favoriteProductIds = MutableStateFlow(
        prefs.getStringSet("pos_favorites", emptySet()) ?: emptySet()
    )
    val favoriteProductIds: StateFlow<Set<String>> = _favoriteProductIds.asStateFlow()

    val products: StateFlow<List<Product>> = combine(
        repository.products,
        _favoriteProductIds
    ) { prods, favs ->
        prods.map { it.copy(isFavorite = favs.contains(it.id)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _firestoreTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = combine(
        repository.transactions,
        _firestoreTransactions
    ) { localTx, remoteTx ->
        // Merge and distinct by ID, newest first
        (remoteTx + localTx).distinctBy { it.id }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // HELD / PARKED CARTS
    private val _heldOrders = MutableStateFlow<List<HeldOrder>>(emptyList())
    val heldOrders: StateFlow<List<HeldOrder>> = _heldOrders.asStateFlow()

    // LATEST RECEIPT FOR INTERACTIVE RECEIPT MODAL
    private val _latestReceipt = MutableStateFlow<ReceiptData?>(null)
    val latestReceipt: StateFlow<ReceiptData?> = _latestReceipt.asStateFlow()

    fun dismissLatestReceipt() {
        _latestReceipt.value = null
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // TODAY ANALYTICS KPIS
    val todaySalesTotal: StateFlow<Double> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val startOfDay = getStartOfToday()
        txs.filter { (it.type == "sale" || it.type.isBlank()) && it.timestamp >= startOfDay }
            .sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todaySalesCount: StateFlow<Int> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val startOfDay = getStartOfToday()
        txs.count { (it.type == "sale" || it.type.isBlank()) && it.timestamp >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val todayCashTotal: StateFlow<Double> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val startOfDay = getStartOfToday()
        txs.filter { (it.type == "sale" || it.type.isBlank()) && it.paymentType.contains("Nağd", ignoreCase = true) && it.timestamp >= startOfDay }
            .sumOf { if (it.cashPaid > 0) it.cashPaid else it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todayCardTotal: StateFlow<Double> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val startOfDay = getStartOfToday()
        txs.filter { (it.type == "sale" || it.type.isBlank()) && it.paymentType.contains("Kart", ignoreCase = true) && it.timestamp >= startOfDay }
            .sumOf { if (it.cardPaid > 0) it.cardPaid else it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val todayAvgBasket: StateFlow<Double> = combine(todaySalesTotal, todaySalesCount) { total, count ->
        if (count > 0) total / count else 0.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // APP STAGES
    private val _appStage = MutableStateFlow(AppStage.SPLASH)
    val appStage: StateFlow<AppStage> = _appStage.asStateFlow()

    // PIN & BIOMETRIC AUTH
    private val _savedPin = MutableStateFlow(prefs.getString("pos_pin", null))
    val savedPin: StateFlow<String?> = _savedPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("pos_biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _cashierName = MutableStateFlow(prefs.getString("cashier_name", "Kassir Operator") ?: "Kassir Operator")
    val cashierName: StateFlow<String> = _cashierName.asStateFlow()

    // MULTI-TENANT SELECTION
    private val _availableTenants = MutableStateFlow<List<Tenant>>(emptyList())
    val availableTenants: StateFlow<List<Tenant>> = _availableTenants.asStateFlow()

    private val _selectedTenant = MutableStateFlow<Tenant?>(null)
    val selectedTenant: StateFlow<Tenant?> = _selectedTenant.asStateFlow()

    // Active Tenant ID for Firestore sync
    val activeTenantId: String
        get() = prefs.getString("erp_tenant_id", "IZrfGLfOkcwzP3RSCHzt") ?: "IZrfGLfOkcwzP3RSCHzt"

    // CATEGORIES
    private val _categories = MutableStateFlow(listOf("Hamısı", "İçkilər", "Qida", "Şirniyyat", "Tütün", "Xırdavat"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Hamısı")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Theme Mode State
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // ERP Settings & Login State
    private val _erpUrl = MutableStateFlow("https://www.e-manager.az/")
    val erpUrl: StateFlow<String> = _erpUrl.asStateFlow()

    private val _erpEmail = MutableStateFlow(prefs.getString("erp_email", "") ?: "")
    val erpEmail: StateFlow<String> = _erpEmail.asStateFlow()

    private val _erpPassword = MutableStateFlow(prefs.getString("erp_password", "") ?: "")
    val erpPassword: StateFlow<String> = _erpPassword.asStateFlow()

    private val _isErpConnected = MutableStateFlow(false)
    val isErpConnected: StateFlow<Boolean> = _isErpConnected.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Hələ ki daxil olunmayıb")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private val _companyName = MutableStateFlow(prefs.getString("erp_tenant_name", "e-Manager POS") ?: "e-Manager POS")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    // CART STATE
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _posPaymentType = MutableStateFlow("Nağd")
    val posPaymentType: StateFlow<String> = _posPaymentType.asStateFlow()

    private val _itemInputMode = MutableStateFlow("sale")
    val itemInputMode: StateFlow<String> = _itemInputMode.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<String?>("Pərakəndə Alıcı")
    val selectedCustomer: StateFlow<String?> = _selectedCustomer.asStateFlow()

    // CUSTOMERS LIST
    private val _customers = MutableStateFlow<List<Customer>>(
        listOf(
            Customer("cust_0", "Pərakəndə Alıcı", "+994500000000", 0.0),
            Customer("cust_1", "Rauf Əliyev", "+994501234567", 125.50),
            Customer("cust_2", "Vüqar Həsənov", "+994559876543", 0.0),
            Customer("cust_3", "Leyla İsmayılova", "+994703216549", 45.00)
        )
    )
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    // SUPPLIERS
    private val _suppliers = MutableStateFlow<List<Supplier>>(
        listOf(
            Supplier("sup_1", "Bolluq MMC", "+994125001122", "Bolluq Dağıtım", 450.0),
            Supplier("sup_2", "Azersun Qrup", "+994124003344", "Azersun Holding", 1200.0),
            Supplier("sup_3", "Avrora Distribusiya", "+994124998877", "Avrora MMC", 0.0)
        )
    )
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    // CASH (KASSA) OPERATIONS
    private val _cashOperations = MutableStateFlow<List<CashOperation>>(
        listOf(
            CashOperation("cash_init", "income", "Kassa Qalıq Devri", 500.0, "İlkin Qalıq", System.currentTimeMillis() - 86400000)
        )
    )
    val cashOperations: StateFlow<List<CashOperation>> = _cashOperations.asStateFlow()

    // FINANCIAL TOTALS
    val totalIncome: StateFlow<Double> = combine(_cashOperations, transactions) { cashOps, txs ->
        val opsIncome = cashOps.filter { it.type == "income" }.sumOf { it.amount }
        val salesIncome = txs.filter { (it.type == "sale" || it.type.isBlank()) && it.paymentType == "Nağd" }.sumOf { it.totalAmount }
        opsIncome + salesIncome
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 500.0)

    val totalExpense: StateFlow<Double> = combine(_cashOperations, transactions) { cashOps, txs ->
        val opsExpense = cashOps.filter { it.type == "expense" }.sumOf { it.amount }
        val purchaseExpense = txs.filter { it.type == "purchase" && it.paymentType == "Nağd" }.sumOf { it.totalAmount }
        opsExpense + purchaseExpense
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val cashBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 500.0)

    // WAREHOUSE TOTALS
    val totalStockQty: StateFlow<Double> = products.combine(MutableStateFlow(Unit)) { prods, _ ->
        prods.sumOf { it.stockQuantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalStockValue: StateFlow<Double> = products.combine(MutableStateFlow(Unit)) { prods, _ ->
        prods.sumOf { it.stockQuantity * it.price }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalDamagedQty: StateFlow<Double> = products.combine(MutableStateFlow(Unit)) { prods, _ ->
        prods.sumOf { it.damagedQuantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalDamagedValue: StateFlow<Double> = products.combine(MutableStateFlow(Unit)) { prods, _ ->
        prods.sumOf { it.damagedQuantity * (if (it.costPrice > 0.0) it.costPrice else it.price) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _cartDiscountPercent = MutableStateFlow(0.0)
    val cartDiscountPercent: StateFlow<Double> = _cartDiscountPercent.asStateFlow()

    private val _subtotalAmount = MutableStateFlow(0.0)
    val subtotalAmount: StateFlow<Double> = _subtotalAmount.asStateFlow()

    private val _discountTotal = MutableStateFlow(0.0)
    val discountTotal: StateFlow<Double> = _discountTotal.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saleResult = MutableStateFlow<SaleResponse?>(null)
    val saleResult: StateFlow<SaleResponse?> = _saleResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val savedToken = prefs.getString("erp_token", null)
        if (!savedToken.isNullOrBlank()) {
            ApiClient.setAuthToken(savedToken)
            _isErpConnected.value = true
            _connectionStatusMessage.value = "Uğurla qoşuldu: ${_companyName.value}"
            viewModelScope.launch {
                repository.fetchProductsFromErp()
            }
        }
        setupFirestoreSync()
    }

    override fun onCleared() {
        super.onCleared()
        transactionsListener?.remove()
        productsListener?.remove()
        customersListener?.remove()
        suppliersListener?.remove()
    }

    // -------------------------------------------------------------
    // FIRESTORE SYNCHRONIZATION WITH E-MANAGER.AZ
    // -------------------------------------------------------------
    fun setupFirestoreSync() {
        val tId = activeTenantId
        if (tId.isBlank()) return

        // 1. Sync Mobile POS Transactions
        transactionsListener?.remove()
        transactionsListener = firestore.collection("tenants")
            .document(tId)
            .collection("mobile_pos_transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val type = doc.getString("type") ?: "sale"
                        val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                        val customerName = doc.getString("customerName")
                        val paymentType = when (doc.getString("paymentType")) {
                            "cash" -> "Nağd"
                            "debt" -> "Nisyə"
                            "card" -> "Kart"
                            else -> doc.getString("paymentType") ?: "Nağd"
                        }
                        val note = doc.getString("note")
                        val createdAtStr = doc.getString("createdAt")
                        val timestamp = if (!createdAtStr.isNullOrBlank()) {
                            try {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(createdAtStr)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                doc.getDate("timestamp")?.time ?: System.currentTimeMillis()
                            }
                        } else {
                            doc.getDate("timestamp")?.time ?: System.currentTimeMillis()
                        }

                        // Parse items if available
                        @Suppress("UNCHECKED_CAST")
                        val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                        val items = itemsRaw.map { itemMap ->
                            val pId = itemMap["productId"]?.toString() ?: ""
                            val pName = itemMap["productName"]?.toString() ?: "Məhsul"
                            val pPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0
                            val qty = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                            val isReturn = (itemMap["isReturn"] as? Boolean) ?: false
                            CartItem(
                                product = Product(id = pId, name = pName, price = pPrice),
                                quantity = qty,
                                isReturn = isReturn
                            )
                        }

                        TransactionRecord(
                            id = id,
                            type = type,
                            items = items,
                            totalAmount = totalAmount,
                            customerName = customerName,
                            paymentType = paymentType,
                            note = note,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _firestoreTransactions.value = list
            }

        // 2. Sync Customers
        customersListener?.remove()
        customersListener = firestore.collection("tenants")
            .document(tId)
            .collection("mobile_pos_customers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val custs = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val phone = doc.getString("phone")
                        val balance = doc.getDouble("balance") ?: 0.0
                        Customer(id = id, name = name, phone = phone, balance = balance)
                    }
                    if (custs.isNotEmpty()) {
                        val base = listOf(Customer("cust_0", "Pərakəndə Alıcı", "+994500000000", 0.0))
                        _customers.value = (base + custs).distinctBy { it.name }
                    }
                }
            }

        // 3. Sync Suppliers
        suppliersListener?.remove()
        suppliersListener = firestore.collection("tenants")
            .document(tId)
            .collection("suppliers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val sups = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val phone = doc.getString("phone")
                        val company = doc.getString("company") ?: name
                        val balance = doc.getDouble("balance") ?: 0.0
                        Supplier(id = id, name = name, phone = phone, company = company, balance = balance)
                    }
                    if (sups.isNotEmpty()) {
                        _suppliers.value = sups
                    }
                }
            }

        // 4. Sync Products from Firestore
        productsListener?.remove()
        productsListener = firestore.collection("tenants")
            .document(tId)
            .collection("products")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val price = doc.getDouble("salePrice") ?: doc.getDouble("price") ?: 0.0
                        val cost = doc.getDouble("costPrice") ?: 0.0
                        val barcode = doc.getString("barcode") ?: doc.getString("sku")
                        val cat = doc.getString("category") ?: "Ümumi"
                        val stock = doc.getDouble("stockQuantity") ?: doc.getDouble("stock") ?: 100.0
                        val damaged = doc.getDouble("damagedQuantity") ?: 0.0
                        val unit = doc.getString("unit") ?: "əd"
                        ProductEntity(
                            id = id,
                            productNumber = doc.getString("productNumber"),
                            name = name,
                            sku = barcode,
                            barcode = barcode,
                            price = price,
                            salePrice = price,
                            costPrice = cost,
                            category = cat,
                            unit = unit,
                            stock = stock,
                            stockQuantity = stock,
                            damagedQuantity = damaged,
                            taxRate = 18.0,
                            isVatApplicable = true
                        )
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            database.productDao().insertAll(entities)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    // -------------------------------------------------------------
    // APP LIFECYCLE & NAVIGATION
    // -------------------------------------------------------------
    fun onSplashComplete() {
        try {
            val auth = FirebaseAuth.getInstance()
            val isAuth = auth.currentUser != null || !prefs.getString("erp_token", null).isNullOrBlank()

            if (!isAuth) {
                _appStage.value = AppStage.AUTH
            } else {
                auth.currentUser?.let { user ->
                    if (_cashierName.value == "Kassir Operator" && !user.displayName.isNullOrBlank()) {
                        _cashierName.value = user.displayName!!
                    }
                }
                if (!_savedPin.value.isNullOrBlank()) {
                    _appStage.value = AppStage.PIN_LOCK
                } else {
                    _appStage.value = AppStage.PIN_SETUP
                }
            }
        } catch (e: Exception) {
            _appStage.value = AppStage.AUTH
        }
    }

    fun savePin(pin: String, enableBiometric: Boolean) {
        prefs.edit()
            .putString("pos_pin", pin)
            .putBoolean("pos_biometric_enabled", enableBiometric)
            .apply()
        _savedPin.value = pin
        _isBiometricEnabled.value = enableBiometric
        _appStage.value = AppStage.MAIN_POS
        setupFirestoreSync()
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String?) -> Unit) {
        if (_savedPin.value != null && _savedPin.value != oldPin) {
            onResult(false, "Köhnə PIN kod yanlışdır!")
            return
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            onResult(false, "Yeni PIN 4 rəqəmdən ibarət olmalıdır!")
            return
        }
        prefs.edit().putString("pos_pin", newPin).apply()
        _savedPin.value = newPin
        onResult(true, "PIN kod uğurla dəyişdirildi!")
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pos_biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun unlockWithPin() {
        _appStage.value = AppStage.MAIN_POS
        setupFirestoreSync()
    }

    fun selectTenant(tenant: Tenant) {
        _selectedTenant.value = tenant
        _companyName.value = tenant.name
        prefs.edit()
            .putString("erp_tenant_id", tenant.id)
            .putString("erp_tenant_name", tenant.name)
            .apply()

        setupFirestoreSync()

        if (_savedPin.value.isNullOrBlank()) {
            _appStage.value = AppStage.PIN_SETUP
        } else {
            _appStage.value = AppStage.MAIN_POS
        }
    }

    private fun proceedToPinOrMain() {
        if (_savedPin.value.isNullOrBlank()) {
            _appStage.value = AppStage.PIN_SETUP
        } else {
            _appStage.value = AppStage.MAIN_POS
        }
    }

    fun checkUserTenantsAfterLogin(uid: String, onFinished: () -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val displayName = doc.getString("displayName") ?: ""
                    if (displayName.isNotBlank()) {
                        _cashierName.value = displayName
                        prefs.edit().putString("cashier_name", displayName).apply()
                    }

                    @Suppress("UNCHECKED_CAST")
                    val authorizedTenants = doc.get("authorizedTenants") as? List<String> ?: emptyList()
                    val companyNameStr = doc.getString("companyName") ?: doc.getString("tenantName")

                    if (authorizedTenants.size > 1) {
                        val tenantsList = mutableListOf<Tenant>()
                        var fetchedCount = 0
                        for (tId in authorizedTenants) {
                            firestore.collection("tenants").document(tId).get()
                                .addOnSuccessListener { tDoc ->
                                    val name = tDoc.getString("name") ?: tId
                                    val voen = tDoc.getString("voen")
                                    tenantsList.add(Tenant(id = tId, name = name, voen = voen))
                                    fetchedCount++
                                    if (fetchedCount == authorizedTenants.size) {
                                        _availableTenants.value = tenantsList
                                        _appStage.value = AppStage.TENANT_SELECT
                                        onFinished()
                                    }
                                }
                                .addOnFailureListener {
                                    fetchedCount++
                                    if (fetchedCount == authorizedTenants.size) {
                                        if (tenantsList.isNotEmpty()) {
                                            _availableTenants.value = tenantsList
                                            _appStage.value = AppStage.TENANT_SELECT
                                        } else {
                                            proceedToPinOrMain()
                                        }
                                        onFinished()
                                    }
                                }
                        }
                    } else if (authorizedTenants.size == 1) {
                        val tId = authorizedTenants.first()
                        firestore.collection("tenants").document(tId).get()
                            .addOnSuccessListener { tDoc ->
                                val name = tDoc.getString("name") ?: (companyNameStr ?: "Şirkət")
                                val voen = tDoc.getString("voen")
                                selectTenant(Tenant(id = tId, name = name, voen = voen))
                                onFinished()
                            }
                            .addOnFailureListener {
                                if (!companyNameStr.isNullOrBlank()) _companyName.value = companyNameStr
                                proceedToPinOrMain()
                                onFinished()
                            }
                    } else {
                        if (!companyNameStr.isNullOrBlank()) {
                            _companyName.value = companyNameStr
                        }
                        proceedToPinOrMain()
                        onFinished()
                    }
                } else {
                    proceedToPinOrMain()
                    onFinished()
                }
            }
            .addOnFailureListener {
                proceedToPinOrMain()
                onFinished()
            }
    }

    // -------------------------------------------------------------
    // POS MAIN & CART OPERATIONS
    // -------------------------------------------------------------
    fun setPosPaymentType(type: String) {
        _posPaymentType.value = type
    }

    fun setItemInputMode(mode: String) {
        _itemInputMode.value = mode
    }

    fun setSelectedCustomer(customerName: String?) {
        _selectedCustomer.value = customerName
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_categories.value.contains(trimmed)) {
            _categories.value = _categories.value + trimmed
        }
    }

    fun addCustomer(name: String, phone: String = "") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val newCust = Customer(
            id = "cust_${System.currentTimeMillis()}",
            name = trimmed,
            phone = phone.ifBlank { "+99450" + (1000000..9999999).random() },
            balance = 0.0
        )
        _customers.value = listOf(newCust) + _customers.value.filter { it.name != trimmed }
        _selectedCustomer.value = trimmed

        // Push to Firestore
        val tId = activeTenantId
        val payload = hashMapOf(
            "id" to newCust.id,
            "tenantId" to tId,
            "name" to newCust.name,
            "phone" to newCust.phone,
            "balance" to 0.0,
            "createdAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_customers")
            .document(newCust.id)
            .set(payload)
    }

    fun addSupplier(name: String, phone: String = "", company: String = "") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val newSup = Supplier(
            id = "sup_${System.currentTimeMillis()}",
            name = trimmed,
            phone = phone.ifBlank { "+99412" + (1000000..9999999).random() },
            company = company.ifBlank { trimmed },
            balance = 0.0
        )
        _suppliers.value = listOf(newSup) + _suppliers.value

        val tId = activeTenantId
        val payload = hashMapOf(
            "id" to newSup.id,
            "tenantId" to tId,
            "name" to newSup.name,
            "phone" to newSup.phone,
            "company" to newSup.company,
            "balance" to 0.0
        )
        firestore.collection("tenants").document(tId)
            .collection("suppliers")
            .document(newSup.id)
            .set(payload)
    }

    fun addNewProduct(
        name: String,
        price: Double,
        costPrice: Double = 0.0,
        category: String = "Ümumi",
        barcode: String = "",
        stockQuantity: Double = 100.0
    ) {
        val prodId = "prod_${System.currentTimeMillis()}"
        val cleanBarcode = barcode.ifBlank { "869${(100000000..999999999).random()}" }
        val newProd = Product(
            id = prodId,
            name = name.trim(),
            price = price,
            costPrice = costPrice,
            category = category.ifBlank { "Ümumi" },
            stockQuantity = stockQuantity,
            damagedQuantity = 0.0,
            barcode = cleanBarcode,
            unit = "əd"
        )

        // Save local Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.productDao().insertAll(
                listOf(
                    ProductEntity(
                        id = newProd.id,
                        productNumber = "MP-${System.currentTimeMillis() % 100000}",
                        name = newProd.name,
                        sku = cleanBarcode,
                        barcode = cleanBarcode,
                        price = price,
                        salePrice = price,
                        costPrice = costPrice,
                        category = newProd.category,
                        unit = "əd",
                        stock = stockQuantity,
                        stockQuantity = stockQuantity,
                        damagedQuantity = 0.0,
                        taxRate = 18.0,
                        isVatApplicable = true
                    )
                )
            )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Push to Firestore tenants/${tenantId}/products
        val tId = activeTenantId
        val payload = hashMapOf(
            "id" to newProd.id,
            "name" to newProd.name,
            "price" to newProd.price,
            "salePrice" to newProd.price,
            "costPrice" to newProd.costPrice,
            "category" to newProd.category,
            "barcode" to newProd.barcode,
            "sku" to newProd.barcode,
            "stockQuantity" to newProd.stockQuantity,
            "damagedQuantity" to 0.0,
            "unit" to "əd",
            "updatedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
        firestore.collection("tenants").document(tId)
            .collection("products")
            .document(newProd.id)
            .set(payload)
    }

    fun markProductAsDamaged(productId: String, quantity: Double) {
        if (quantity <= 0.0) return
        val currentProduct = products.value.find { it.id == productId } ?: return
        val newDamaged = currentProduct.damagedQuantity + quantity
        val newStock = (currentProduct.stockQuantity - quantity).coerceAtLeast(0.0)

        // Update local Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.productDao().insertAll(
                    listOf(
                        ProductEntity(
                            id = currentProduct.id,
                            productNumber = null,
                            name = currentProduct.name,
                            sku = currentProduct.barcode,
                            barcode = currentProduct.barcode,
                            price = currentProduct.price,
                            salePrice = currentProduct.price,
                            costPrice = currentProduct.costPrice,
                            unit = currentProduct.unit,
                            stock = newStock,
                            stockQuantity = newStock,
                            taxRate = 18.0,
                            isVatApplicable = true,
                            category = currentProduct.category,
                            damagedQuantity = newDamaged
                        )
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Update Firestore
        val tId = activeTenantId
        firestore.collection("tenants").document(tId)
            .collection("products")
            .document(productId)
            .update(mapOf(
                "stockQuantity" to newStock,
                "damagedQuantity" to newDamaged
            ))
    }

    fun toggleFavoriteProduct(productId: String) {
        val current = _favoriteProductIds.value.toMutableSet()
        if (current.contains(productId)) {
            current.remove(productId)
        } else {
            current.add(productId)
        }
        _favoriteProductIds.value = current
        prefs.edit().putStringSet("pos_favorites", current).apply()
    }

    fun holdCurrentCart(note: String = "") {
        val items = _cartItems.value
        if (items.isEmpty()) return
        val cleanNote = note.trim().ifBlank { "Səbət #${_heldOrders.value.size + 1}" }
        val newHeld = HeldOrder(
            id = "HELD_${System.currentTimeMillis()}",
            note = cleanNote,
            items = items,
            customerName = _selectedCustomer.value,
            discountPercent = _cartDiscountPercent.value
        )
        _heldOrders.value = listOf(newHeld) + _heldOrders.value
        _cartItems.value = emptyList()
        _cartDiscountPercent.value = 0.0
        _selectedCustomer.value = "Pərakəndə Alıcı"
        recalculateTotal()
    }

    fun resumeHeldOrder(orderId: String) {
        val order = _heldOrders.value.find { it.id == orderId } ?: return
        _heldOrders.value = _heldOrders.value.filter { it.id != orderId }
        _cartItems.value = order.items
        _selectedCustomer.value = order.customerName ?: "Pərakəndə Alıcı"
        _cartDiscountPercent.value = order.discountPercent
        recalculateTotal()
    }

    fun deleteHeldOrder(orderId: String) {
        _heldOrders.value = _heldOrders.value.filter { it.id != orderId }
    }

    fun setCartDiscountPercent(percent: Double) {
        _cartDiscountPercent.value = percent.coerceIn(0.0, 100.0)
        recalculateTotal()
    }

    fun addToCart(product: Product) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = currentList[index].quantity + 1)
        } else {
            currentList.add(CartItem(product, 1, isReturn = _itemInputMode.value == "return"))
        }
        _cartItems.value = currentList
        recalculateTotal()
    }

    fun updateQuantity(productId: String, delta: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val newQuantity = currentList[index].quantity + delta
            if (newQuantity > 0) {
                currentList[index] = currentList[index].copy(quantity = newQuantity)
            } else {
                currentList.removeAt(index)
            }
            _cartItems.value = currentList
            recalculateTotal()
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
        recalculateTotal()
    }

    fun toggleCartItemReturn(productId: String) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isReturn = !currentList[index].isReturn)
            _cartItems.value = currentList
            recalculateTotal()
        }
    }

    fun setCartItemDiscount(productId: String, discountPercent: Double) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(discountPercent = discountPercent.coerceIn(0.0, 100.0))
            _cartItems.value = currentList
            recalculateTotal()
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cartDiscountPercent.value = 0.0
        recalculateTotal()
    }

    private fun recalculateTotal() {
        val sub = _cartItems.value.sumOf { it.totalPrice }
        val discPercent = _cartDiscountPercent.value
        val discAmt = (sub * (discPercent / 100.0)).coerceAtLeast(0.0)
        val net = (sub - discAmt).coerceAtLeast(0.0)

        _subtotalAmount.value = sub
        _discountTotal.value = discAmt
        _totalAmount.value = net
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product)
            } else {
                _errorMessage.value = "Məhsul tapılmadı: $barcode"
            }
            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------
    // SUBMIT SALE & SYNC TO E-MANAGER
    // -------------------------------------------------------------
    fun submitSale(
        paymentType: String,
        cashierName: String,
        cashPaid: Double = 0.0,
        cardPaid: Double = 0.0
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        _isLoading.value = true
        _errorMessage.value = null
        _saleResult.value = null

        val currentSubtotal = _subtotalAmount.value
        val currentDiscount = _discountTotal.value
        val currentTotal = _totalAmount.value
        val custName = _selectedCustomer.value ?: "Pərakəndə Alıcı"
        val invoiceNumber = "INV-${System.currentTimeMillis() % 1000000}"
        val tId = activeTenantId
        val now = System.currentTimeMillis()
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(now))

        val actualCashPaid = if (paymentType == "Nağd" && cashPaid == 0.0) currentTotal else cashPaid
        val actualCardPaid = if (paymentType == "Kart") currentTotal else cardPaid
        val changeGiven = if (paymentType == "Nağd" || paymentType == "Qarışıq") {
            (actualCashPaid - (currentTotal - actualCardPaid)).coerceAtLeast(0.0)
        } else 0.0

        // Create interactive receipt
        val receipt = ReceiptData(
            invoiceNumber = invoiceNumber,
            companyName = _companyName.value,
            voen = _selectedTenant.value?.voen,
            cashierName = cashierName,
            customerName = custName,
            items = items,
            subtotal = currentSubtotal,
            discountPercent = _cartDiscountPercent.value,
            discountAmount = currentDiscount,
            totalAmount = currentTotal,
            paymentType = paymentType,
            cashPaid = actualCashPaid,
            cardPaid = actualCardPaid,
            changeGiven = changeGiven,
            timestamp = now
        )
        _latestReceipt.value = receipt

        // Record in local Transactions
        val txRecord = TransactionRecord(
            id = invoiceNumber,
            type = if (items.any { it.isReturn }) "sale_return" else "sale",
            items = items,
            subtotalAmount = currentSubtotal,
            discountAmount = currentDiscount,
            totalAmount = currentTotal,
            customerName = custName,
            paymentType = paymentType,
            cashPaid = actualCashPaid,
            cardPaid = actualCardPaid,
            changeGiven = changeGiven,
            timestamp = now
        )
        _firestoreTransactions.value = listOf(txRecord) + _firestoreTransactions.value

        // 1. Prepare and send to Firestore mobile_pos_transactions
        val txType = if (items.any { it.isReturn }) "sale_return" else "sale"
        val firestoreTx = hashMapOf(
            "id" to invoiceNumber,
            "tenantId" to tId,
            "mobileUserId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "pos_cashier"),
            "type" to txType,
            "customerId" to (_customers.value.find { it.name == custName }?.id ?: ""),
            "customerName" to custName,
            "paymentType" to (if (paymentType == "Nağd") "cash" else if (paymentType == "Nisyə") "debt" else if (paymentType == "Qarışıq") "split" else "card"),
            "items" to items.map { cartItem ->
                mapOf(
                    "productId" to cartItem.product.id,
                    "productName" to cartItem.product.name,
                    "quantity" to cartItem.quantity,
                    "unitPrice" to cartItem.product.price,
                    "total" to cartItem.totalPrice,
                    "isReturn" to cartItem.isReturn
                )
            },
            "subtotal" to currentSubtotal,
            "discountTotal" to currentDiscount,
            "totalAmount" to currentTotal,
            "paidAmount" to (if (paymentType == "Nisyə") 0.0 else currentTotal),
            "debtAmount" to (if (paymentType == "Nisyə") currentTotal else 0.0),
            "cashPaid" to actualCashPaid,
            "cardPaid" to actualCardPaid,
            "changeGiven" to changeGiven,
            "createdAt" to isoNow
        )

        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_transactions")
            .document(invoiceNumber)
            .set(firestoreTx)

        // 2. Reduce stock for items in Room and Firestore
        items.forEach { cartItem ->
            val p = cartItem.product
            val newQty = (p.stockQuantity - cartItem.quantity).coerceAtLeast(0.0)
            firestore.collection("tenants").document(tId)
                .collection("products")
                .document(p.id)
                .update("stockQuantity", newQty)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    database.productDao().updateStock(p.id, newQty)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Submit Sale to REST API / Offline DB
        val saleRequest = SaleRequest(
            paymentType = paymentType,
            totalAmount = currentTotal,
            cashierName = cashierName,
            cashboxCode = "POS-ANDROID-01",
            customerName = custName,
            notes = "Mobil POS satışı",
            items = items.map { cartItem ->
                SaleItem(
                    productId = cartItem.product.id,
                    barcode = cartItem.product.barcode,
                    name = cartItem.product.name,
                    quantity = cartItem.quantity.toDouble(),
                    price = cartItem.product.price,
                    unit = cartItem.product.unit
                )
            }
        )

        viewModelScope.launch {
            val response = repository.submitSale(saleRequest)
            _saleResult.value = response
            _cartItems.value = emptyList()
            _cartDiscountPercent.value = 0.0
            recalculateTotal()
            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------
    // KASSA (CASH IN / CASH OUT) OPERATIONS
    // -------------------------------------------------------------
    fun addCashOperation(type: String, title: String, amount: Double, category: String, note: String? = null) {
        val id = "CASH_${System.currentTimeMillis()}"
        val op = CashOperation(
            id = id,
            type = type, // "income" or "expense"
            title = title.ifBlank { if (type == "income") "Mədaxil" else "Məxaric" },
            amount = amount,
            category = category.ifBlank { "Ümumi Kassa" },
            timestamp = System.currentTimeMillis()
        )
        _cashOperations.value = listOf(op) + _cashOperations.value

        // Sync to Firestore mobile_pos_transactions & cashTransactions
        val tId = activeTenantId
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        val txData = hashMapOf(
            "id" to id,
            "tenantId" to tId,
            "type" to type,
            "paymentType" to "cash",
            "totalAmount" to amount,
            "paidAmount" to amount,
            "debtAmount" to 0.0,
            "expenseCategory" to category,
            "note" to (note ?: title),
            "createdAt" to isoNow
        )

        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_transactions")
            .document(id)
            .set(txData)

        firestore.collection("tenants").document(tId)
            .collection("cashTransactions")
            .document(id)
            .set(mapOf(
                "id" to id,
                "type" to type,
                "amount" to amount,
                "category" to category,
                "description" to title,
                "date" to isoNow
            ))
    }

    // -------------------------------------------------------------
    // WAREHOUSE PURCHASE OPERATIONS (ALIŞ ƏMƏLİYYATLARI)
    // -------------------------------------------------------------
    fun addPurchaseOperation(
        supplierName: String,
        productId: String,
        quantity: Double,
        purchasePrice: Double
    ) {
        val product = products.value.find { it.id == productId } ?: return
        val totalCost = quantity * purchasePrice
        val id = "PURCHASE_${System.currentTimeMillis()}"
        val tId = activeTenantId
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        // 1. Increase stock in Room and Firestore
        val newStock = product.stockQuantity + quantity
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.productDao().updateStock(productId, newStock)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        firestore.collection("tenants").document(tId)
            .collection("products")
            .document(productId)
            .update(mapOf(
                "stockQuantity" to newStock,
                "costPrice" to purchasePrice
            ))

        // 2. Add purchase transaction to mobile_pos_transactions
        val purchaseTx = hashMapOf(
            "id" to id,
            "tenantId" to tId,
            "type" to "purchase",
            "customerName" to supplierName,
            "paymentType" to "cash",
            "totalAmount" to totalCost,
            "paidAmount" to totalCost,
            "debtAmount" to 0.0,
            "items" to listOf(
                mapOf(
                    "productId" to product.id,
                    "productName" to product.name,
                    "quantity" to quantity,
                    "unitPrice" to purchasePrice,
                    "total" to totalCost
                )
            ),
            "note" to "Anbar Alışı: $supplierName",
            "createdAt" to isoNow
        )

        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_transactions")
            .document(id)
            .set(purchaseTx)
    }

    // -------------------------------------------------------------
    // TRANSACTION MANAGEMENT (EDIT & DELETE)
    // -------------------------------------------------------------
    fun deleteTransaction(transactionId: String) {
        // Delete locally
        _firestoreTransactions.value = _firestoreTransactions.value.filter { it.id != transactionId }
        _cashOperations.value = _cashOperations.value.filter { it.id != transactionId }

        // Delete from Firestore
        val tId = activeTenantId
        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_transactions")
            .document(transactionId)
            .delete()

        firestore.collection("tenants").document(tId)
            .collection("cashTransactions")
            .document(transactionId)
            .delete()
    }

    fun editTransaction(
        transactionId: String,
        newCustomerName: String?,
        newNote: String?,
        newPaymentType: String
    ) {
        val tId = activeTenantId
        val updates = mutableMapOf<String, Any>()
        if (newCustomerName != null) updates["customerName"] = newCustomerName
        if (newNote != null) updates["note"] = newNote
        updates["paymentType"] = if (newPaymentType == "Nağd") "cash" else if (newPaymentType == "Nisyə") "debt" else "card"

        firestore.collection("tenants").document(tId)
            .collection("mobile_pos_transactions")
            .document(transactionId)
            .update(updates)

        // Update local list
        _firestoreTransactions.value = _firestoreTransactions.value.map { tx ->
            if (tx.id == transactionId) {
                tx.copy(
                    customerName = newCustomerName ?: tx.customerName,
                    note = newNote ?: tx.note,
                    paymentType = newPaymentType
                )
            } else tx
        }
    }

    // -------------------------------------------------------------
    // SEARCH & FILTER
    // -------------------------------------------------------------
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (_isErpConnected.value) {
            viewModelScope.launch {
                repository.fetchProductsFromErp(search = query.takeIf { it.isNotBlank() })
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun updateErpUrl(url: String) {
        _erpUrl.value = url
    }

    fun updateErpEmail(email: String) {
        _erpEmail.value = email
    }

    fun updateErpPassword(password: String) {
        _erpPassword.value = password
    }

    // -------------------------------------------------------------
    // AUTHENTICATION & ERP LOGIN
    // -------------------------------------------------------------
    fun loginAndConnectErp() {
        if (_erpEmail.value.isBlank() || _erpPassword.value.isBlank()) {
            _connectionStatusMessage.value = "Xəta: E-poçt və şifrə boş ola bilməz"
            _isErpConnected.value = false
            return
        }

        _connectionStatusMessage.value = "ERP sisteminə qoşulur..."
        viewModelScope.launch {
            try {
                val response = apiService.login(
                    LoginRequest(
                        email = _erpEmail.value,
                        password = _erpPassword.value,
                        apiKey = activeTenantId
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val token = body.token

                    if (token != null) {
                        ApiClient.setAuthToken(token)
                        _isErpConnected.value = true
                        _companyName.value = body.tenant?.name ?: "Şirkət"
                        _connectionStatusMessage.value = "Uğurla qoşuldu: ${_companyName.value}"

                        prefs.edit()
                            .putString("erp_token", token)
                            .putString("erp_email", _erpEmail.value)
                            .putString("erp_password", _erpPassword.value)
                            .putString("erp_tenant_name", _companyName.value)
                            .apply()

                        repository.fetchProductsFromErp()
                    } else {
                        _isErpConnected.value = false
                        _connectionStatusMessage.value = "Xəta: Token tapılmadı"
                    }
                } else {
                    _isErpConnected.value = false
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = try {
                        val jsonObject = JSONObject(errorBodyString ?: "")
                        jsonObject.optString("error", "Giriş uğursuz oldu")
                    } catch (e: Exception) {
                        "Giriş uğursuz oldu: ${response.code()}"
                    }
                    _connectionStatusMessage.value = "Xəta: $errorMsg"
                }
            } catch (e: Exception) {
                _isErpConnected.value = false
                _connectionStatusMessage.value = "Şəbəkə xətası: ${e.localizedMessage}"
            }
        }
    }

    private fun formatFirebaseAuthError(exception: Exception?): String {
        val msg = exception?.message ?: ""
        return when {
            msg.contains("credential is incorrect", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("RecaptchaAction", ignoreCase = true) ||
            msg.contains("Initial task failed", ignoreCase = true) ||
            msg.contains("malformed or has expired", ignoreCase = true) ->
                "Daxil edilən e-poçt və ya şifrə yanlışdır (və ya bu hesab qeydiyyatdan keçməyib). Zəhmət olmasa məlumatları yoxlayın, yeni hesab üçün 'Qeydiyyat' bölməsinə keçin və ya 'Sınaq Rejimi' ilə daxil olun."

            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ->
                "Bu e-poçt ünvanı ilə istifadəçi tapılmadı. Zəhmət olmasa 'Qeydiyyat' bölməsindən yeni hesab açın."

            msg.contains("email-already-in-use", ignoreCase = true) ||
            msg.contains("already in use", ignoreCase = true) ->
                "Bu e-poçt ünvanı ilə artıq hesab mövcuddur. 'Daxil Ol' bölməsinə keçin."

            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true) ->
                "E-poçt ünvanı düzgün formatda deyil."

            msg.contains("weak-password", ignoreCase = true) ||
            msg.contains("at least 6 characters", ignoreCase = true) ->
                "Şifrə ən azı 6 simvoldan ibarət olmalıdır."

            msg.contains("network error", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "İnternet bağlantısı xətası baş verdi. Zəhmət olmasa şəbəkəni yoxlayın."

            msg.contains("too-many-requests", ignoreCase = true) ->
                "Çox sayda cəhd edildi. Zəhmət olmasa bir neçə dəqiqə sonra yenidən cəhd edin."

            else -> exception?.localizedMessage ?: "Giriş uğursuz oldu. Məlumatları yoxlayın və ya 'Qeydiyyat' bölməsindən yeni hesab yaradın."
        }
    }

    fun loginAsDemoCashier() {
        val demoName = "Kassir Operator"
        val demoCompany = "e-Manager POS"
        _cashierName.value = demoName
        _companyName.value = demoCompany
        _isErpConnected.value = false
        _connectionStatusMessage.value = "Oflayn Kassa Rejimi"
        prefs.edit()
            .putString("cashier_name", demoName)
            .putString("erp_tenant_name", demoCompany)
            .putString("erp_tenant_id", "demo_tenant_01")
            .apply()
        proceedToPinOrMain()
    }

    fun firebaseEmailLogin(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        _erpEmail.value = trimmedEmail
        _erpPassword.value = trimmedPassword

        // Fast path for demo credentials
        if (trimmedEmail.contains("demo", ignoreCase = true) || 
            trimmedEmail.contains("kassa", ignoreCase = true) ||
            trimmedEmail.contains("admin", ignoreCase = true) && trimmedPassword == "admin" ||
            trimmedPassword == "demo123") {
            loginAsDemoCashier()
            onResult(true, null)
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    loginAndConnectErp()
                    checkUserTenantsAfterLogin(uid) {
                        onResult(true, null)
                    }
                } else {
                    viewModelScope.launch {
                        try {
                            val response = apiService.login(
                                LoginRequest(email = trimmedEmail, password = trimmedPassword, apiKey = "IZrfGLfOkcwzP3RSCHzt")
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                val body = response.body()!!
                                val token = body.token
                                if (token != null) {
                                    ApiClient.setAuthToken(token)
                                    _isErpConnected.value = true
                                    _companyName.value = body.tenant?.name ?: "Şirkət"
                                    _connectionStatusMessage.value = "Uğurla qoşuldu: ${_companyName.value}"
                                    prefs.edit()
                                        .putString("erp_token", token)
                                        .putString("erp_email", trimmedEmail)
                                        .putString("erp_password", trimmedPassword)
                                        .putString("erp_tenant_name", _companyName.value)
                                        .apply()
                                    repository.fetchProductsFromErp()
                                    proceedToPinOrMain()
                                    onResult(true, null)
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore and proceed to format error
                        }
                        val friendlyError = formatFirebaseAuthError(task.exception)
                        onResult(false, friendlyError)
                    }
                }
            }
    }

    fun firebaseRegister(
        displayName: String,
        companyName: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user?.updateProfile(profileUpdates)

                    _cashierName.value = displayName
                    _companyName.value = companyName
                    _erpEmail.value = email
                    _erpPassword.value = password

                    val tenantId = "tenant_${System.currentTimeMillis()}"

                    val userData = hashMapOf(
                        "uid" to uid,
                        "email" to email,
                        "displayName" to displayName,
                        "companyName" to companyName,
                        "role" to "admin",
                        "authorizedTenants" to listOf(tenantId),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    val tenantData = hashMapOf(
                        "id" to tenantId,
                        "name" to companyName,
                        "authorizedUsers" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("users").document(uid).set(userData)
                    firestore.collection("tenants").document(tenantId).set(tenantData)

                    prefs.edit()
                        .putString("erp_email", email)
                        .putString("erp_password", password)
                        .putString("erp_tenant_name", companyName)
                        .putString("erp_tenant_id", tenantId)
                        .putString("cashier_name", displayName)
                        .apply()

                    _isErpConnected.value = true
                    _connectionStatusMessage.value = "Uğurla qeydiyyatdan keçdi: $companyName"

                    _appStage.value = AppStage.PIN_SETUP
                    onResult(true, null)
                } else {
                    val friendlyError = formatFirebaseAuthError(task.exception)
                    onResult(false, friendlyError)
                }
            }
    }

    fun firebaseGoogleLogin(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    _erpEmail.value = user?.email ?: ""
                    _isErpConnected.value = true
                    _companyName.value = user?.displayName ?: "Google İstifadəçisi"
                    _connectionStatusMessage.value = "Google ilə daxil olundu: ${_companyName.value}"

                    checkUserTenantsAfterLogin(uid) {
                        onResult(true, null)
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Google giriş xətası")
                }
            }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        ApiClient.setAuthToken(null)
        _isErpConnected.value = false
        _connectionStatusMessage.value = "Daxil olunmayıb"
        _appStage.value = AppStage.AUTH
        prefs.edit().remove("erp_token").apply()
    }

    fun switchAccount() {
        FirebaseAuth.getInstance().signOut()
        ApiClient.setAuthToken(null)
        _isErpConnected.value = false
        _appStage.value = AppStage.AUTH
    }
}
