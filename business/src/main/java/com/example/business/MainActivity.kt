package com.example.business

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.business.ui.BusinessAppStage
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.ThemeMode
import com.example.business.ui.screens.*
import com.example.business.ui.theme.BusinessTheme
import com.example.business.util.NotificationHelper

enum class ActiveScreen {
    MAIN_TABS,
    ADD_PRODUCT,
    ADD_DEBTOR,
    ADD_SUPPLIER,
    SETTINGS
}

class MainActivity : FragmentActivity() {
    private val viewModel: BusinessViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification channel and permission setup
        NotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check for updates from D1 / GitHub in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.business.util.AppUpdateManager.checkForUpdates(
                forceSilent = true,
                context = applicationContext
            )
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            BusinessTheme(darkTheme = darkTheme) {
                BusinessAppRoot(viewModel = viewModel)
                com.example.business.ui.components.AppUpdateDialog()
            }
        }
    }
}

@Composable
fun BusinessAppRoot(viewModel: BusinessViewModel) {
    val appStage by viewModel.appStage.collectAsState()
    var activeSubScreen by rememberSaveable { mutableStateOf(ActiveScreen.MAIN_TABS) }

    when (appStage) {
        BusinessAppStage.SPLASH -> {
            SplashScreen(onComplete = { viewModel.onSplashFinished() })
        }
        BusinessAppStage.AUTH -> {
            AuthScreen(viewModel = viewModel)
        }
        BusinessAppStage.TENANT_SELECT -> {
            val tenants by viewModel.availableTenants.collectAsState()
            val selectedTenant by viewModel.selectedTenant.collectAsState()

            TenantSelectDialog(
                tenants = tenants,
                selectedTenantId = selectedTenant?.id,
                onSelectTenant = { tenant ->
                    viewModel.selectTenant(tenant)
                },
                onDismiss = {
                    if (tenants.isNotEmpty()) {
                        viewModel.selectTenant(tenants.first())
                    }
                }
            )
        }
        BusinessAppStage.PIN_LOCK -> {
            PinLockScreen(viewModel = viewModel)
        }
        BusinessAppStage.MAIN -> {
            when (activeSubScreen) {
                ActiveScreen.ADD_PRODUCT -> {
                    AddProductScreen(
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = ActiveScreen.MAIN_TABS }
                    )
                }
                ActiveScreen.ADD_DEBTOR -> {
                    AddDebtorScreen(
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = ActiveScreen.MAIN_TABS }
                    )
                }
                ActiveScreen.ADD_SUPPLIER -> {
                    AddSupplierScreen(
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = ActiveScreen.MAIN_TABS }
                    )
                }
                ActiveScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = ActiveScreen.MAIN_TABS }
                    )
                }
                ActiveScreen.MAIN_TABS -> {
                    var currentTab by rememberSaveable { mutableStateOf(BusinessNavTab.DASHBOARD) }
                    val channels by viewModel.channels.collectAsState()
                    val totalUnread = remember(channels) { channels.sumOf { it.unreadCount } }

                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            BusinessNavTab.entries.forEach { tab ->
                                item(
                                    icon = {
                                        if (tab == BusinessNavTab.CHAT && totalUnread > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text("$totalUnread") }
                                                }
                                            ) {
                                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    selected = tab == currentTab,
                                    onClick = { currentTab = tab }
                                )
                            }
                        }
                    ) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            when (currentTab) {
                                BusinessNavTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToDebtors = { currentTab = BusinessNavTab.PARTNERS },
                                    onNavigateToSuppliers = { currentTab = BusinessNavTab.PARTNERS },
                                    onNavigateToReports = { currentTab = BusinessNavTab.REPORTS },
                                    onNavigateToChat = { currentTab = BusinessNavTab.CHAT },
                                    onNavigateToSettings = { activeSubScreen = ActiveScreen.SETTINGS },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                BusinessNavTab.WAREHOUSE -> WarehouseScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddProduct = { activeSubScreen = ActiveScreen.ADD_PRODUCT },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                BusinessNavTab.PARTNERS -> PartnersScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddDebtor = { activeSubScreen = ActiveScreen.ADD_DEBTOR },
                                    onNavigateToAddSupplier = { activeSubScreen = ActiveScreen.ADD_SUPPLIER },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                BusinessNavTab.REPORTS -> ReportsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                                BusinessNavTab.CHAT -> ChatScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class BusinessNavTab(
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("İcmal", Icons.Default.Dashboard),
    WAREHOUSE("Anbar", Icons.Default.Inventory2),
    PARTNERS("Tərəfdaşlar", Icons.Default.PeopleAlt),
    REPORTS("Hesabatlar", Icons.Default.BarChart),
    CHAT("Daxili Çat", Icons.Default.Forum)
}
