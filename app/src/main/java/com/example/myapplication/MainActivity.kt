package com.example.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.ui.AppStage
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.ThemeMode
import com.example.myapplication.ui.screens.AuthScreen
import com.example.myapplication.ui.screens.CashScreen
import com.example.myapplication.ui.screens.HistoryScreen
import com.example.myapplication.ui.screens.MoreScreen
import com.example.myapplication.ui.screens.PinLockScreen
import com.example.myapplication.ui.screens.PinScreenMode
import com.example.myapplication.ui.screens.PosMainScreen
import com.example.myapplication.ui.screens.SplashScreen
import com.example.myapplication.ui.screens.TenantSelectDialog
import com.example.myapplication.ui.screens.WarehouseScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Check for POS updates from GitHub in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.myapplication.util.PosAppUpdateManager.checkForUpdates(forceSilent = true)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                POSApp(viewModel = viewModel)
                com.example.myapplication.ui.components.PosAppUpdateDialog()
            }
        }
    }
}

@Composable
fun POSApp(viewModel: PosViewModel) {
    val appStage by viewModel.appStage.collectAsState()
    val isConnected by viewModel.isErpConnected.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val cashierName by viewModel.cashierName.collectAsState()
    val savedPin by viewModel.savedPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val availableTenants by viewModel.availableTenants.collectAsState()
    val selectedTenant by viewModel.selectedTenant.collectAsState()

    when (appStage) {
        AppStage.SPLASH -> {
            SplashScreen(
                isOnline = isConnected,
                onComplete = {
                    viewModel.onSplashComplete()
                }
            )
        }

        AppStage.AUTH -> {
            AuthScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    // PosViewModel handles next stage automatically
                }
            )
        }

        AppStage.TENANT_SELECT -> {
            TenantSelectDialog(
                tenants = availableTenants,
                selectedTenantId = selectedTenant?.id,
                onSelectTenant = { tenant ->
                    viewModel.selectTenant(tenant)
                },
                onDismiss = {
                    // If dismissed without choosing, pick the first tenant
                    if (availableTenants.isNotEmpty()) {
                        viewModel.selectTenant(availableTenants.first())
                    }
                }
            )
        }

        AppStage.PIN_SETUP -> {
            PinLockScreen(
                mode = PinScreenMode.SETUP,
                companyName = companyName,
                cashierName = cashierName,
                savedPin = null,
                isBiometricEnabled = false,
                onPinSet = { pin, bio ->
                    viewModel.savePin(pin, bio)
                },
                onPinSuccess = { },
                onSwitchAccount = {
                    viewModel.switchAccount()
                }
            )
        }

        AppStage.PIN_LOCK -> {
            PinLockScreen(
                mode = PinScreenMode.UNLOCK,
                companyName = companyName,
                cashierName = cashierName,
                savedPin = savedPin,
                isBiometricEnabled = isBiometricEnabled,
                onPinSet = { _, _ -> },
                onPinSuccess = {
                    viewModel.unlockWithPin()
                },
                onSwitchAccount = {
                    viewModel.switchAccount()
                }
            )
        }

        AppStage.MAIN_POS -> {
            var currentDestination by rememberSaveable { mutableStateOf(PosDestinations.POS) }

            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    PosDestinations.entries.forEach { destination ->
                        item(
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            selected = destination == currentDestination,
                            onClick = { currentDestination = destination }
                        )
                    }
                }
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentDestination) {
                        PosDestinations.POS -> PosMainScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                        PosDestinations.CASH -> CashScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                        PosDestinations.WAREHOUSE -> WarehouseScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                        PosDestinations.HISTORY -> HistoryScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                        PosDestinations.MORE -> MoreScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

enum class PosDestinations(
    val label: String,
    val icon: ImageVector,
) {
    POS("Əsas", Icons.Default.PointOfSale),
    CASH("Kassa", Icons.Default.AccountBalanceWallet),
    WAREHOUSE("Anbar", Icons.Default.Inventory2),
    HISTORY("Tarixçə", Icons.Default.History),
    MORE("Daha çox", Icons.Default.MoreHoriz),
}
