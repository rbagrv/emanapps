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
import java.util.Locale

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
    val hideNegativeStock = MutableStateFlow(false)
    val hideZeroStock = MutableStateFlow(false)
    val onlyLowStock = MutableStateFlow(false)

    val warehouseItems: StateFlow<List<WarehouseItem>> = combine(
        repository.warehouseItems,
        warehouseSearchQuery,
        hideNegativeStock,
        hideZeroStock,
        onlyLowStock
    ) { list, query, hideNeg, hideZero, lowOnly ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.sku.contains(query, ignoreCase = true) ||
                    (item.barcode?.contains(query) == true)
            val matchesNeg = !hideNeg || item.stock >= 0.0
            val matchesZero = !hideZero || item.stock != 0.0
            val matchesLow = !lowOnly || item.isLowStock
            matchesQuery && matchesNeg && matchesZero && matchesLow
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val warehouseSummary: StateFlow<WarehouseSummary> = repository.warehouseSummary

    // Reports
    val selectedReportPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val selectedReportType = MutableStateFlow(ReportType.TURNOVER)

    private val _currentReport = MutableStateFlow(repository.getReport(ReportPeriod.THIS_MONTH))
    val currentReport: StateFlow<PeriodReport> = _currentReport.asStateFlow()

    private val _pnlReport = MutableStateFlow(repository.getProfitAndLoss(ReportPeriod.THIS_MONTH))
    val pnlReport: StateFlow<ProfitAndLossReport> = _pnlReport.asStateFlow()

    private val _balanceReport = MutableStateFlow(repository.getBalanceSheet())
    val balanceReport: StateFlow<BalanceSheetReport> = _balanceReport.asStateFlow()

    private val _cashFlowReport = MutableStateFlow(repository.getCashFlow(ReportPeriod.THIS_MONTH))
    val cashFlowReport: StateFlow<CashFlowReport> = _cashFlowReport.asStateFlow()

    private val _taxReport = MutableStateFlow(repository.getTaxReport(ReportPeriod.THIS_MONTH))
    val taxReport: StateFlow<TaxLiabilityReport> = _taxReport.asStateFlow()

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

    val topProducts: StateFlow<List<TopProductStat>> = MutableStateFlow(
        listOf(
            TopProductStat("Coca-Cola 500ml", "İçki", 1420.0, 1704.0),
            TopProductStat("Qara Qızıl Çay 250qr", "Çay & Qəhvə", 860.0, 3870.0),
            TopProductStat("Milla Yağ 1kq", "Süd Məhsulları", 410.0, 7790.0),
            TopProductStat("Ariel Avtomat 3kq", "Məişət Kimyası", 295.0, 4425.0)
        )
    ).asStateFlow()

    val smartInsights: StateFlow<List<SmartInsight>> = combine(
        repository.summary,
        repository.warehouseSummary,
        repository.debtors
    ) { sum, wh, debts ->
        val list = mutableListOf<SmartInsight>()

        val overdueCount = debts.count { it.status == DebtStatus.OVERDUE }
        if (overdueCount > 0) {
            list.add(
                SmartInsight(
                    id = "ins_overdue",
                    title = "Gecikmiş Müştəri Borcları",
                    description = "$overdueCount müştərinin ödəmə vaxtı keçib (₼${String.format(Locale.US, "%,.2f", sum.overdueReceivables)}). WhatsApp xatırlatması göndərmək tövsiyə edilir.",
                    type = InsightType.WARNING,
                    actionText = "Debitorlara Bax"
                )
            )
        }

        if (wh.lowStockCount > 0) {
            list.add(
                SmartInsight(
                    id = "ins_low_stock",
                    title = "Anbarda Kritik Qalıqlar",
                    description = "${wh.lowStockCount} çeşid məhsul kritik həddə çatıb. Satışların dayanmaması üçün təchizatçılardan sifariş verilməlidir.",
                    type = InsightType.CRITICAL,
                    actionText = "Anbara Keç"
                )
            )
        }

        list.add(
            SmartInsight(
                id = "ins_profit",
                title = "Xalis Rentabellik ~38.4%",
                description = "Cari ay üzrə xalis mənfəət ₼${String.format(Locale.US, "%,.0f", sum.monthProfit)} təşkil edir. Dövriyyə keçən aya nisbətən 12% artıb.",
                type = InsightType.POSITIVE,
                actionText = "Hesabatlara Bax"
            )
        )

        list.add(
            SmartInsight(
                id = "ins_cash",
                title = "Likvidlik və Kassa Ehtiyatı",
                description = "Kassa və bank hesabı balansı (₼${String.format(Locale.US, "%,.0f", sum.cashInHand + sum.bankBalance)}) cari xərcləri və təchizatçı borclarını tam qarşılayır.",
                type = InsightType.TIP
            )
        )
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
    private fun formatFirebaseAuthError(exception: Exception?): String {
        val msg = exception?.message ?: ""
        return when {
            msg.contains("credential is incorrect", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("RecaptchaAction", ignoreCase = true) ||
            msg.contains("Initial task failed", ignoreCase = true) ||
            msg.contains("malformed or has expired", ignoreCase = true) ->
                "Daxil edilən e-poçt və ya şifrə yanlışdır (və ya bu hesab qeydiyyatdan keçməyib). Zəhmət olmasa məlumatları yoxlayın, yeni hesab üçün 'Qeydiyyat' bölməsinə keçin və ya dərhal 'Sınaq Sahibkar Girişi' ilə davam edin."

            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ->
                "Bu e-poçt ilə sahibkar hesabı tapılmadı. Zəhmət olmasa 'Qeydiyyat' bölməsindən yeni sahibkar hesabı açın."

            msg.contains("email-already-in-use", ignoreCase = true) ||
            msg.contains("already in use", ignoreCase = true) ->
                "Bu e-poçt ilə artıq hesab mövcuddur. 'Daxil Ol' bölməsinə keçin."

            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true) ->
                "E-poçt ünvanı düzgün formatda deyil."

            msg.contains("weak-password", ignoreCase = true) ||
            msg.contains("at least 6 characters", ignoreCase = true) ->
                "Şifrə ən azı 6 simvoldan ibarət olmalıdır."

            msg.contains("network error", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "Şəbəkə xətası baş verdi. İnternet bağlantısını yoxlayın."

            else -> exception?.localizedMessage ?: "Daxil olmaq mümkün olmadı. E-poçt və şifrəni yoxlayın və ya 'Qeydiyyat' bölməsinə keçin."
        }
    }

    fun loginAsDemoOwner() {
        val demoTenant = Tenant(
            id = "IZrfGLfOkcwzP3RSCHzt",
            name = "Global Ticarət MMC",
            voen = "1802938471"
        )
        ownerName.value = "Rəşad Bağırov (Sahibkar)"
        companyName.value = "Global Ticarət MMC"
        prefs.edit()
            .putString("owner_name", "Rəşad Bağırov (Sahibkar)")
            .putString("erp_tenant_name", "Global Ticarət MMC")
            .putString("erp_tenant_id", "IZrfGLfOkcwzP3RSCHzt")
            .putString("erp_email", "sahibkar@emanager.az")
            .apply()
        selectTenant(demoTenant)
        _appStage.value = BusinessAppStage.MAIN
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            onResult(false, "Zəhmət olmasa e-poçt və şifrəni daxil edin!")
            return
        }

        // Fast-path demo login
        if (trimmedEmail.contains("demo", ignoreCase = true) ||
            trimmedEmail.contains("admin", ignoreCase = true) && trimmedPass == "admin" ||
            trimmedPass == "demo123" || trimmedPass == "password123") {
            loginAsDemoOwner()
            onResult(true, null)
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
                if (trimmedEmail.contains("demo") || trimmedPass == "demo123" || trimmedPass == "password123") {
                    loginAsDemoOwner()
                    onResult(true, null)
                } else {
                    val friendlyError = formatFirebaseAuthError(exception)
                    onResult(false, friendlyError)
                }
            }
    }

    fun register(
        displayName: String,
        companyName: String,
        voen: String,
        phone: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()
        val trimmedName = displayName.trim()
        val trimmedCompany = companyName.trim()
        val trimmedVoen = voen.trim()
        val trimmedPhone = phone.trim()

        auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()
                    user?.updateProfile(profileUpdates)

                    val tenantId = "tenant_${System.currentTimeMillis()}"

                    val userData = hashMapOf(
                        "uid" to uid,
                        "email" to trimmedEmail,
                        "displayName" to trimmedName,
                        "companyName" to trimmedCompany,
                        "phone" to trimmedPhone,
                        "role" to "owner",
                        "authorizedTenants" to listOf(tenantId),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    val tenantData = hashMapOf(
                        "id" to tenantId,
                        "name" to trimmedCompany,
                        "voen" to trimmedVoen,
                        "authorizedUsers" to listOf(uid),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    firestore.collection("users").document(uid).set(userData)
                    firestore.collection("tenants").document(tenantId).set(tenantData)

                    this.ownerName.value = trimmedName
                    this.companyName.value = trimmedCompany

                    prefs.edit()
                        .putString("erp_email", trimmedEmail)
                        .putString("owner_name", trimmedName)
                        .putString("erp_tenant_name", trimmedCompany)
                        .putString("erp_tenant_id", tenantId)
                        .putString("owner_voen", trimmedVoen)
                        .apply()

                    val newTenant = Tenant(id = tenantId, name = trimmedCompany, voen = trimmedVoen)
                    selectTenant(newTenant)
                    onResult(true, null)
                } else {
                    val friendlyError = formatFirebaseAuthError(task.exception)
                    onResult(false, friendlyError)
                }
            }
    }

    fun loadUserTenants(uid: String) {
        // 1. First fetch active tenants list directly from Cloudflare D1 Backend (api/extension/tenants)
        viewModelScope.launch {
            try {
                val d1Response = com.example.business.data.api.D1ApiClient.erpRestService.getTenants()
                if (d1Response.isSuccessful && d1Response.body()?.success == true) {
                    val d1List = d1Response.body()?.tenants?.map { dto ->
                        Tenant(
                            id = dto.id,
                            name = dto.name,
                            voen = dto.voen
                        )
                    } ?: emptyList()

                    if (d1List.isNotEmpty()) {
                        _availableTenants.value = d1List
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("BusinessVM", "D1 tenants load fallback to Firestore: ${e.message}")
            }
        }

        // 2. Also fetch user's authorized tenants from Firestore
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
                                        val combined = (_availableTenants.value + tenantsList).distinctBy { it.id }
                                        _availableTenants.value = combined
                                    }
                                }
                                .addOnFailureListener {
                                    fetchedCount++
                                    if (fetchedCount == authorizedTenants.size) {
                                        val combined = (_availableTenants.value + tenantsList).distinctBy { it.id }
                                        _availableTenants.value = combined
                                    }
                                }
                        }
                    } else if (_availableTenants.value.isEmpty()) {
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
        _pnlReport.value = repository.getProfitAndLoss(period)
        _balanceReport.value = repository.getBalanceSheet()
        _cashFlowReport.value = repository.getCashFlow(period)
        _taxReport.value = repository.getTaxReport(period)
    }

    fun selectReportType(type: ReportType) {
        selectedReportType.value = type
    }

    fun shareReportPdf(context: Context, sendToWhatsApp: Boolean = false) {
        com.example.business.util.PdfReportHelper.generateAndSharePdf(
            context = context,
            companyName = companyName.value,
            voen = summary.value.voen,
            reportType = selectedReportType.value,
            period = selectedReportPeriod.value,
            periodReport = currentReport.value,
            pnlReport = pnlReport.value,
            balanceReport = balanceReport.value,
            cashFlowReport = cashFlowReport.value,
            taxReport = taxReport.value,
            sendToWhatsApp = sendToWhatsApp
        )
    }

    // Debtor actions
    fun recordDebtorPayment(debtorId: String, amount: Double, paymentType: String) {
        repository.addDebtorPayment(debtorId, amount, paymentType, cashier = "Sahibkar (Rəhbər)")
    }

    fun createDebtor(name: String, company: String?, phone: String, debt: Double, dueDate: String, notes: String, debtStartDate: String? = null) {
        repository.addNewDebtor(name, company, phone, debt, dueDate, notes, debtStartDate)
    }

    // Supplier actions
    fun recordSupplierPayment(supplierId: String, amount: Double, note: String) {
        repository.addSupplierPayment(supplierId, amount, note)
    }

    fun createSupplier(companyName: String, contactPerson: String, phone: String, email: String, debt: Double, category: String, debtStartDate: String? = null) {
        repository.addNewSupplier(companyName, contactPerson, phone, email, debt, category, debtStartDate)
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

    val teamEmployees: List<TeamEmployee>
        get() = repository.teamEmployees

    fun assignTask(title: String, employee: TeamEmployee, dueDate: String, priority: TaskPriority) {
        val channelId = selectedChannel.value?.id ?: "c_general"
        repository.assignTask(channelId, title, employee, dueDate, priority)
    }

    fun toggleTaskStatus(taskId: String) {
        val channelId = selectedChannel.value?.id ?: "c_general"
        repository.toggleTaskStatus(channelId, taskId)
    }

    fun quickAdjustStock(productId: String, delta: Double) {
        val current = warehouseItems.value.find { it.id == productId }
        if (current != null) {
            val updated = (current.stock + delta).coerceAtLeast(0.0)
            repository.updateProductStock(productId, updated)
        }
    }

    fun sendPredefinedChatMessage(text: String) {
        val channelId = selectedChannel.value?.id ?: "c_general"
        repository.sendMessage(channelId, text, senderRole = "Sahibkar")
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
