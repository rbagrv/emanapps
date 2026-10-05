package com.example.business.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.business.data.model.*
import com.example.business.data.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BusinessAppStage {
    SPLASH,
    AUTH,
    TENANT_SELECT,
    PIN_LOCK,
    MAIN
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class BusinessViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("emanager_business_prefs", Context.MODE_PRIVATE)
    private val repository = BusinessRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _appStage = MutableStateFlow(BusinessAppStage.SPLASH)
    val appStage: StateFlow<BusinessAppStage> = _appStage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val companyName = MutableStateFlow(prefs.getString("erp_tenant_name", "Global Ticarət MMC") ?: "Global Ticarət MMC")
    val ownerName = MutableStateFlow(prefs.getString("owner_name", "Cənab Sahibkar") ?: "Cənab Sahibkar")
    val savedPin = MutableStateFlow(prefs.getString("pos_pin", "1234") ?: "1234")
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("pos_biometric_enabled", true))

    // Profile State
    val ownerProfile: StateFlow<OwnerProfile> = repository.ownerProfile

    // Multi-tenant state
    private val _availableTenants = MutableStateFlow<List<Tenant>>(emptyList())
    val availableTenants: StateFlow<List<Tenant>> = _availableTenants.asStateFlow()

    private val _selectedTenant = MutableStateFlow<Tenant?>(null)
    val selectedTenant: StateFlow<Tenant?> = _selectedTenant.asStateFlow()

    val activeTenantId: String
        get() = prefs.getString("erp_tenant_id", "IZrfGLfOkcwzP3RSCHzt") ?: "IZrfGLfOkcwzP3RSCHzt"

    // Summary
    val summary: StateFlow<BusinessSummary> = repository.summary

    // Debtors
    val debtorSearchQuery = MutableStateFlow("")
    val debtorFilterStatus = MutableStateFlow<DebtStatus?>(null)

    val filteredDebtors: StateFlow<List<Debtor>> = combine(
        repository.debtors,
        debtorSearchQuery,
        debtorFilterStatus
    ) { list, query, status ->
        list.filter { debtor ->
            val matchesQuery = query.isBlank() ||
                    debtor.fullName.contains(query, ignoreCase = true) ||
                    (debtor.companyName?.contains(query, ignoreCase = true) == true) ||
                    debtor.phone.contains(query)
            val matchesStatus = status == null || debtor.status == status
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Suppliers
    val supplierSearchQuery = MutableStateFlow("")

    val filteredSuppliers: StateFlow<List<Supplier>> = combine(
        repository.suppliers,
        supplierSearchQuery
    ) { list, query ->
        list.filter { supplier ->
            query.isBlank() ||
                    supplier.companyName.contains(query, ignoreCase = true) ||
                    supplier.contactPerson.contains(query, ignoreCase = true) ||
                    supplier.category.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Warehouse
    val warehouseSearchQuery = MutableStateFlow("")
    val warehouseItems: StateFlow<List<WarehouseItem>> = combine(
        repository.warehouseItems,
        warehouseSearchQuery
    ) { list, query ->
        list.filter { item ->
            query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.sku.contains(query, ignoreCase = true) ||
                    (item.barcode?.contains(query) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val warehouseSummary: StateFlow<WarehouseSummary> = repository.warehouseSummary

    // Reports
    val selectedReportPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    private val _currentReport = MutableStateFlow(repository.getReport(ReportPeriod.THIS_MONTH))
    val currentReport: StateFlow<PeriodReport> = _currentReport.asStateFlow()

    // Chat
    val channels: StateFlow<List<ChatChannel>> = repository.channels
    val selectedChannel = MutableStateFlow<ChatChannel?>(null)

    val currentMessages: StateFlow<List<ChatMessage>> = combine(
        repository.messages,
        selectedChannel
    ) { allMessages, channel ->
        if (channel != null) {
            allMessages[channel.id] ?: emptyList()
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatInputText = MutableStateFlow("")

    init {
        // Connect to last saved tenant
        val lastTenantId = activeTenantId
        val lastTenantName = companyName.value
        repository.connectToTenant(lastTenantId, lastTenantName)
        repository.loadUserProfile()

        viewModelScope.launch {
            repository.channels.collect { list ->
                if (selectedChannel.value == null && list.isNotEmpty()) {
                    selectedChannel.value = list.first()
                }
            }
        }

        // Keep current report updated whenever summary changes
        viewModelScope.launch {
            repository.summary.collect {
                _currentReport.value = repository.getReport(selectedReportPeriod.value)
            }
        }
    }

    fun onSplashFinished() {
        if (auth.currentUser != null) {
            loadUserTenants(auth.currentUser!!.uid)
            repository.loadUserProfile()
            _appStage.value = BusinessAppStage.PIN_LOCK
        } else {
            _appStage.value = BusinessAppStage.AUTH
        }
    }

    // -------------------------------------------------------------
    // Real e-manager.az ERP Authentication & Multi-Tenant Login
    // -------------------------------------------------------------
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            onResult(false, "Zəhmət olmasa e-poçt və şifrəni daxil edin!")
            return
        }

        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                prefs.edit().putString("erp_email", trimmedEmail).apply()

                if (uid != null) {
                    repository.loadUserProfile()
                    checkUserTenantsAfterLogin(uid) {
                        onResult(true, null)
                    }
                } else {
                    _appStage.value = BusinessAppStage.PIN_LOCK
                    onResult(true, null)
                }
            }
            .addOnFailureListener { exception ->
                // Check if user clicked demo or test mode credentials
                if (trimmedEmail.contains("demo") || trimmedPass == "demo123" || trimmedPass == "password123") {
                    val demoTenant = Tenant(
                        id = "IZrfGLfOkcwzP3RSCHzt",
                        name = "Global Ticarət MMC",
                        voen = "1802938471"
                    )
                    selectTenant(demoTenant)
                    onResult(true, null)
                } else {
                    onResult(false, exception.localizedMessage ?: "Daxil olmaq mümkün olmadı. E-poçt və şifrəni yoxlayın.")
                }
            }
    }

    fun loadUserTenants(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val displayName = doc.getString("displayName")
                    if (!displayName.isNullOrBlank()) {
                        ownerName.value = displayName
                        prefs.edit().putString("owner_name", displayName).apply()
                    }

                    @Suppress("UNCHECKED_CAST")
                    val authorizedTenants = doc.get("authorizedTenants") as? List<String> ?: emptyList()
                    val tenantsList = mutableListOf<Tenant>()

                    if (authorizedTenants.isNotEmpty()) {
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
                                    }
                                }
                                .addOnFailureListener {
                                    fetchedCount++
                                    if (fetchedCount == authorizedTenants.size) {
                                        _availableTenants.value = tenantsList
                                    }
                                }
                        }
                    } else {
                        // Discover all active tenants the user is linked to or system default
                        firestore.collection("tenants").limit(10).get()
                            .addOnSuccessListener { snapshot ->
                                val list = snapshot.documents.map { d ->
                                    Tenant(id = d.id, name = d.getString("name") ?: d.id, voen = d.getString("voen"))
                                }
                                if (list.isNotEmpty()) {
                                    _availableTenants.value = list
                                }
                            }
                    }
                }
            }
    }

    private fun checkUserTenantsAfterLogin(uid: String, onFinished: () -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val displayName = doc.getString("displayName")
                    if (!displayName.isNullOrBlank()) {
                        ownerName.value = displayName
                        prefs.edit().putString("owner_name", displayName).apply()
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
                                        _appStage.value = BusinessAppStage.TENANT_SELECT
                                        onFinished()
                                    }
                                }
                                .addOnFailureListener {
                                    fetchedCount++
                                    if (fetchedCount == authorizedTenants.size) {
                                        if (tenantsList.isNotEmpty()) {
                                            _availableTenants.value = tenantsList
                                            _appStage.value = BusinessAppStage.TENANT_SELECT
                                        } else {
                                            _appStage.value = BusinessAppStage.PIN_LOCK
                                        }
                                        onFinished()
                                    }
                                }
                        }
                    } else if (authorizedTenants.size == 1) {
                        val tId = authorizedTenants.first()
                        firestore.collection("tenants").document(tId).get()
                            .addOnSuccessListener { tDoc ->
                                val name = tDoc.getString("name") ?: (companyNameStr ?: "Müəssisə")
                                val voen = tDoc.getString("voen")
                                selectTenant(Tenant(id = tId, name = name, voen = voen))
                                onFinished()
                            }
                            .addOnFailureListener {
                                val name = companyNameStr ?: "Müəssisə"
                                selectTenant(Tenant(id = tId, name = name, voen = null))
                                onFinished()
                            }
                    } else {
                        val defaultTenantId = doc.getString("tenantId") ?: "IZrfGLfOkcwzP3RSCHzt"
                        val name = companyNameStr ?: "Müəssisə"
                        selectTenant(Tenant(id = defaultTenantId, name = name, voen = null))
                        onFinished()
                    }
                } else {
                    _appStage.value = BusinessAppStage.PIN_LOCK
                    onFinished()
                }
            }
            .addOnFailureListener {
                _appStage.value = BusinessAppStage.PIN_LOCK
                onFinished()
            }
    }

    fun selectTenant(tenant: Tenant) {
        _selectedTenant.value = tenant
        companyName.value = tenant.name
        prefs.edit()
            .putString("erp_tenant_id", tenant.id)
            .putString("erp_tenant_name", tenant.name)
            .apply()

        repository.connectToTenant(tenant.id, tenant.name)

        _appStage.value = BusinessAppStage.MAIN
    }

    fun openTenantSelector() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            loadUserTenants(uid)
        }
        _appStage.value = BusinessAppStage.TENANT_SELECT
    }

    fun updateOwnerProfile(name: String, phone: String, company: String, onResult: (Boolean, String?) -> Unit) {
        repository.updateOwnerProfile(name, phone, company) { success, err ->
            if (success) {
                ownerName.value = name
                companyName.value = company
                prefs.edit()
                    .putString("owner_name", name)
                    .putString("erp_tenant_name", company)
                    .apply()
            }
            onResult(success, err)
        }
    }

    fun unlockPin(pin: String): Boolean {
        if (pin == savedPin.value) {
            _appStage.value = BusinessAppStage.MAIN
            return true
        }
        return false
    }

    fun unlockBiometric() {
        _appStage.value = BusinessAppStage.MAIN
    }

    fun logout() {
        auth.signOut()
        prefs.edit()
            .remove("erp_token")
            .apply()
        _appStage.value = BusinessAppStage.AUTH
    }

    fun lockApp() {
        _appStage.value = BusinessAppStage.PIN_LOCK
    }

    fun selectPeriod(period: ReportPeriod) {
        selectedReportPeriod.value = period
        _currentReport.value = repository.getReport(period)
    }

    // Debtor actions
    fun recordDebtorPayment(debtorId: String, amount: Double, paymentType: String) {
        repository.addDebtorPayment(debtorId, amount, paymentType, cashier = "Sahibkar (Rəhbər)")
    }

    fun createDebtor(name: String, company: String?, phone: String, debt: Double, dueDate: String, notes: String) {
        repository.addNewDebtor(name, company, phone, debt, dueDate, notes)
    }

    // Supplier actions
    fun recordSupplierPayment(supplierId: String, amount: Double, note: String) {
        repository.addSupplierPayment(supplierId, amount, note)
    }

    fun createSupplier(companyName: String, contactPerson: String, phone: String, email: String, debt: Double, category: String) {
        repository.addNewSupplier(companyName, contactPerson, phone, email, debt, category)
    }

    // Warehouse actions
    fun addProduct(name: String, sku: String, barcode: String?, stock: Double, costPrice: Double, salePrice: Double, unit: String) {
        repository.addProduct(name, sku, barcode, stock, costPrice, salePrice, unit)
    }

    fun updateProductStock(productId: String, newStock: Double) {
        repository.updateProductStock(productId, newStock)
    }

    // Chat actions
    fun selectChannel(channel: ChatChannel) {
        selectedChannel.value = channel
    }

    fun sendChatMessage() {
        val text = chatInputText.value
        val channelId = selectedChannel.value?.id ?: return
        if (text.isNotBlank()) {
            repository.sendMessage(channelId, text, senderRole = "Sahibkar")
            chatInputText.value = ""
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
