package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.HeldOrder
import com.example.myapplication.data.Product
import com.example.myapplication.data.ReceiptData
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.components.BarcodeScanner
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate100
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500
import com.example.myapplication.util.BluetoothPrinterHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMainScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cashierName by viewModel.cashierName.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val isErpConnected by viewModel.isErpConnected.collectAsState()
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotalAmount by viewModel.subtotalAmount.collectAsState()
    val discountTotal by viewModel.discountTotal.collectAsState()
    val cartDiscountPercent by viewModel.cartDiscountPercent.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val customersList by viewModel.customers.collectAsState()
    val heldOrders by viewModel.heldOrders.collectAsState()
    val latestReceipt by viewModel.latestReceipt.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Daily KPIs
    val todaySalesTotal by viewModel.todaySalesTotal.collectAsState()
    val todaySalesCount by viewModel.todaySalesCount.collectAsState()
    val todayCashTotal by viewModel.todayCashTotal.collectAsState()
    val todayCardTotal by viewModel.todayCardTotal.collectAsState()
    val todayAvgBasket by viewModel.todayAvgBasket.collectAsState()

    var showAnalyticsBar by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showManualBarcodeDialog by remember { mutableStateOf(false) }
    var showSelectCustomerDialog by remember { mutableStateOf(false) }
    var showQuickCreateModal by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showHeldOrdersDialog by remember { mutableStateOf(false) }
    var showHoldPromptDialog by remember { mutableStateOf(false) }
    var holdNoteInput by remember { mutableStateOf("") }
    var manualBarcodeInput by remember { mutableStateOf("") }

    // Dialog state
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newProdName by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdCost by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("Ümumi") }
    var newProdBarcode by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("100") }

    val allCategories = remember(categoriesList, products) {
        val prodCats = products.map { it.category }.filter { it.isNotBlank() }
        (listOf("Hamısı", "⭐ Sevimlilər") + categoriesList.filter { it != "Hamısı" } + prodCats).distinct()
    }

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { product ->
            val matchesCategory = when (selectedCategory) {
                "Hamısı" -> true
                "⭐ Sevimlilər" -> product.isFavorite
                else -> product.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    (product.barcode?.contains(searchQuery) == true)
            matchesCategory && matchesSearch
        }
    }

    // ==========================================
    // DIALOGS & OVERLAYS
    // ==========================================

    // 1. Digital Receipt Modal (Triggered automatically on sale completion)
    if (latestReceipt != null) {
        DigitalReceiptDialog(
            receipt = latestReceipt!!,
            onDismiss = { viewModel.dismissLatestReceipt() },
            onShare = {
                BluetoothPrinterHelper.shareReceiptText(context, latestReceipt!!)
            },
            onPrint = {
                coroutineScope.launch {
                    val result = BluetoothPrinterHelper.printSaleReceipt(null, latestReceipt!!)
                    Toast.makeText(context, "Çap göndərildi", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 2. Barcode Camera Scanner Dialog
    if (showCameraScanner) {
        AlertDialog(
            onDismissRequest = { showCameraScanner = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmanagerBlue)
                    Text("Barkod Skaner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp))) {
                    BarcodeScanner(
                        onBarcodeScanned = { scannedCode ->
                            showCameraScanner = false
                            viewModel.onBarcodeScanned(scannedCode)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCameraScanner = false }) { Text("Bağla") }
            }
        )
    }

    // 3. Manual Barcode Entry Dialog
    if (showManualBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showManualBarcodeDialog = false },
            title = { Text("Barkod və ya SKU Daxil Edin", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = manualBarcodeInput,
                    onValueChange = { manualBarcodeInput = it },
                    label = { Text("Barkod nömrəsi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = manualBarcodeInput.trim()
                        if (code.isNotEmpty()) {
                            viewModel.onBarcodeScanned(code)
                            manualBarcodeInput = ""
                            showManualBarcodeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Axtar və Əlavə Et") }
            },
            dismissButton = {
                TextButton(onClick = { showManualBarcodeDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 4. Held Orders (Saxlanılan Səbətlər) Dialog
    if (showHeldOrdersDialog) {
        HeldOrdersDialog(
            orders = heldOrders,
            onResume = { id ->
                viewModel.resumeHeldOrder(id)
                showHeldOrdersDialog = false
                Toast.makeText(context, "Səbət bərpa edildi", Toast.LENGTH_SHORT).show()
            },
            onDelete = { id ->
                viewModel.deleteHeldOrder(id)
            },
            onDismiss = { showHeldOrdersDialog = false }
        )
    }

    // 5. Hold Cart Prompt Dialog
    if (showHoldPromptDialog) {
        AlertDialog(
            onDismissRequest = { showHoldPromptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = EmanagerOrange)
                    Text("Səbəti Saxla", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bu səbəti müvəqqəti saxlamaq üçün qeyd və ya müştəri adı yazın:", fontSize = 13.sp, color = Slate500)
                    OutlinedTextField(
                        value = holdNoteInput,
                        onValueChange = { holdNoteInput = it },
                        placeholder = { Text("Məs: Müştəri #1, Əli bəy, Masa 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.holdCurrentCart(holdNoteInput)
                        holdNoteInput = ""
                        showHoldPromptDialog = false
                        Toast.makeText(context, "Səbət saxlanıldı", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange)
                ) { Text("Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showHoldPromptDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 6. Customer Selection Dialog
    if (showSelectCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showSelectCustomerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = EmanagerBlue)
                    Text("Alıcı Seçin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(customersList) { cust ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCustomer == cust.name) EmanagerBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setSelectedCustomer(cust.name)
                                showSelectCustomerDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(cust.phone ?: "-", fontSize = 11.sp, color = Slate500)
                                }
                                if (cust.balance > 0) {
                                    Text("Borc: %.2f ₼".format(cust.balance), color = EmanagerRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSelectCustomerDialog = false
                    showAddCustomerDialog = true
                }) { Text("+ Yeni Müştəri") }
            },
            dismissButton = {
                TextButton(onClick = { showSelectCustomerDialog = false }) { Text("Bağla") }
            }
        )
    }

    // 7. Quick Create Modal (Add Product / Customer)
    if (showQuickCreateModal) {
        AlertDialog(
            onDismissRequest = { showQuickCreateModal = false },
            title = { Text("Yeni Əlavə Et", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showQuickCreateModal = false
                            showAddProductDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Məhsul Yarat")
                    }

                    Button(
                        onClick = {
                            showQuickCreateModal = false
                            showAddCustomerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Alıcı Əlavə Et")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickCreateModal = false }) { Text("Bağla") }
            }
        )
    }

    // 8. Add Customer Dialog
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Yeni Alıcı Əlavə Et", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustomerPhone,
                        onValueChange = { newCustomerPhone = it },
                        label = { Text("Telefon nömrəsi") },
                        placeholder = { Text("+99450xxxxxxx") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomerName.isNotBlank()) {
                            viewModel.addCustomer(newCustomerName, newCustomerPhone)
                            newCustomerName = ""
                            newCustomerPhone = ""
                            showAddCustomerDialog = false
                            Toast.makeText(context, "Alıcı əlavə olundu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Yadda Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 9. Add Product Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Yeni Məhsul Qeydiyyatı", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Məhsulun Adı *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = { newProdPrice = it },
                            label = { Text("Satış Qiyməti (₼) *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProdCost,
                            onValueChange = { newProdCost = it },
                            label = { Text("Maya Qiyməti (₼)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newProdCategory,
                        onValueChange = { newProdCategory = it },
                        label = { Text("Kateqoriya") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProdBarcode,
                        onValueChange = { newProdBarcode = it },
                        label = { Text("Barkod (Boş qalsa avtomatik)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pPrice = newProdPrice.toDoubleOrNull() ?: 0.0
                        val pCost = newProdCost.toDoubleOrNull() ?: 0.0
                        if (newProdName.isNotBlank() && pPrice > 0.0) {
                            viewModel.addNewProduct(
                                name = newProdName,
                                price = pPrice,
                                costPrice = pCost,
                                category = newProdCategory,
                                barcode = newProdBarcode,
                                stockQuantity = newProdStock.toDoubleOrNull() ?: 100.0
                            )
                            newProdName = ""
                            newProdPrice = ""
                            newProdCost = ""
                            newProdBarcode = ""
                            showAddProductDialog = false
                            Toast.makeText(context, "Məhsul əlavə edildi", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Əlavə Et") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 10. Advanced Fast Checkout Dialog (Split, Change Calculator & Discounts)
    if (showCheckoutDialog) {
        AdvancedCheckoutDialog(
            subtotal = subtotalAmount,
            discountPercent = cartDiscountPercent,
            discountAmount = discountTotal,
            totalAmount = totalAmount,
            customerName = selectedCustomer ?: "Pərakəndə Alıcı",
            onDiscountPercentChanged = { viewModel.setCartDiscountPercent(it) },
            onConfirmSale = { paymentType, cashPaid, cardPaid ->
                viewModel.submitSale(
                    paymentType = paymentType,
                    cashierName = cashierName,
                    cashPaid = cashPaid,
                    cardPaid = cardPaid
                )
                showCheckoutDialog = false
            },
            onDismiss = { showCheckoutDialog = false }
        )
    }

    // ==========================================
    // MAIN SCREEN CONTENT
    // ==========================================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. TOP HEADER (BRAND ROYAL BLUE)
            Surface(
                color = EmanagerBlue,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cashierName.firstOrNull()?.uppercase() ?: "K",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(text = companyName, fontSize = 11.sp, color = Color(0xFFDBEAFE))
                                Text(
                                    text = cashierName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Held Orders Badge Button
                            if (heldOrders.isNotEmpty()) {
                                Surface(
                                    onClick = { showHeldOrdersDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmanagerOrange,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("${heldOrders.size}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Analytics toggle button
                            IconButton(
                                onClick = { showAnalyticsBar = !showAnalyticsBar },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (showAnalyticsBar) Icons.Default.Analytics else Icons.Outlined.Analytics,
                                    contentDescription = "Günün Xülasəsi",
                                    tint = Color.White
                                )
                            }

                            // Online status badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isErpConnected) Color(0xFF10B981).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isErpConnected) Color(0xFF10B981) else Color.Yellow)
                                    )
                                    Text(
                                        text = if (isErpConnected) "Canlı" else "Oflayn",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Collapsible Daily KPI strip
                    AnimatedVisibility(visible = showAnalyticsBar) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Bugünkü Satış", fontSize = 10.sp, color = Color(0xFFDBEAFE))
                                        Text(String.format("%.2f ₼", todaySalesTotal), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Qəbz Sayı", fontSize = 10.sp, color = Color(0xFFDBEAFE))
                                        Text("$todaySalesCount çek", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Nağd / Kart", fontSize = 10.sp, color = Color(0xFFDBEAFE))
                                        Text(String.format("%.0f / %.0f ₼", todayCashTotal, todayCardTotal), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Orta Qəbz", fontSize = 10.sp, color = Color(0xFFDBEAFE))
                                        Text(String.format("%.2f ₼", todayAvgBasket), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CUSTOMER SELECTION + "+ YENİ YARAT" BUTTON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Customer Selection Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSelectCustomerDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text("Alıcı", fontSize = 9.sp, color = Color(0xFFBFDBFE))
                                        Text(
                                            text = selectedCustomer ?: "Pərakəndə Alıcı",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // "+ Yeni Yarat" Button
                        Button(
                            onClick = { showQuickCreateModal = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Text("Yeni Yarat", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. SEARCH BAR & SCANNER / KEYPAD ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Məhsul adı və ya barkod axtarın...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmanagerBlue,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Manual Barcode entry button
                Surface(
                    onClick = { showManualBarcodeDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Əllə daxil et", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                // Camera Scanner Button
                Surface(
                    onClick = { showCameraScanner = true },
                    shape = RoundedCornerShape(14.dp),
                    color = EmanagerBlue.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Skaner", tint = EmanagerBlue, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. CATEGORY HORIZONTAL PILLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allCategories.forEach { category ->
                    val isSelected = category.equals(selectedCategory, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.8.dp, Slate200),
                        modifier = Modifier.clickable { viewModel.selectCategory(category) }
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. PRODUCTS CARDS GRID (RESPONSIVE 2-COLUMN)
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (selectedCategory == "⭐ Sevimlilər") "Hələ ki sevimli məhsul seçilməyib.\nMəhsul kartındakı ulduz simvoluna toxunaraq əlavə edin." else "Bu kateqoriyada məhsul tapılmadı",
                            fontSize = 13.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Yeni Məhsul Əlavə Et")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = if (cartItems.isNotEmpty()) 96.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val itemInCart = cartItems.find { it.product.id == product.id }
                        ModernProductCard(
                            product = product,
                            quantityInCart = itemInCart?.quantity ?: 0,
                            onAddToCart = { viewModel.addToCart(product) },
                            onIncrement = { viewModel.updateQuantity(product.id, 1) },
                            onDecrement = { viewModel.updateQuantity(product.id, -1) },
                            onToggleFavorite = { viewModel.toggleFavoriteProduct(product.id) }
                        )
                    }
                }
            }
        }

        // 5. FLOATING BOTTOM CART BAR (IF ITEMS IN CART)
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmanagerBlue)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${cartItems.sumOf { it.quantity }}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("Məhsul seçilib", fontSize = 11.sp, color = Slate500)
                            if (cartDiscountPercent > 0.0) {
                                Text(
                                    "-${cartDiscountPercent.toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmanagerOrange
                                )
                            }
                        }
                        Text(
                            text = String.format("%.2f ₼", totalAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            color = EmanagerBlue
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Hold order button
                        OutlinedButton(
                            onClick = { showHoldPromptDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmanagerOrange)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saxla", fontSize = 12.sp, color = EmanagerOrange, fontWeight = FontWeight.Bold)
                        }

                        // Clear Cart
                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Təmizlə", tint = Slate500)
                        }

                        // Pay Button
                        Button(
                            onClick = { showCheckoutDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Ödəniş", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernProductCard(
    product: Product,
    quantityInCart: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (quantityInCart > 0) EmanagerBlue.copy(alpha = 0.5f) else Slate200
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (quantityInCart > 0) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Row: Category tag + Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmanagerBlue.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 10.sp,
                        color = EmanagerBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Sevimli",
                        tint = if (product.isFavorite) Color(0xFFF59E0B) else Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
                modifier = Modifier.heightIn(min = 34.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Barcode or SKU
            if (!product.barcode.isNullOrBlank()) {
                Text(
                    text = product.barcode,
                    fontSize = 10.sp,
                    color = Slate500,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price & Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format("%.2f ₼", product.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = EmanagerBlue
                    )
                    Text(
                        text = "Stok: ${product.stockQuantity.toInt()} ${product.unit}",
                        fontSize = 10.sp,
                        color = if (product.stockQuantity <= 5) EmanagerRose else Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }

                // If in cart: Stepper (- Qty +) | If not: 1-tap add icon
                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            onClick = onDecrement,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = "$quantityInCart",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Surface(
                            onClick = onIncrement,
                            shape = CircleShape,
                            color = EmanagerBlue,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onAddToCart,
                        shape = CircleShape,
                        color = EmanagerBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "Əlavə et", tint = EmanagerBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedCheckoutDialog(
    subtotal: Double,
    discountPercent: Double,
    discountAmount: Double,
    totalAmount: Double,
    customerName: String,
    onDiscountPercentChanged: (Double) -> Unit,
    onConfirmSale: (paymentType: String, cashPaid: Double, cardPaid: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Nağd") } // "Nağd", "Kart", "Nisyə", "Qarışıq"
    var cashTenderedInput by remember { mutableStateOf("") }
    var splitCashInput by remember { mutableStateOf("") }

    val tenderedCash = cashTenderedInput.toDoubleOrNull() ?: totalAmount
    val changeAmount = (tenderedCash - totalAmount).coerceAtLeast(0.0)
    val shortageAmount = (totalAmount - tenderedCash).coerceAtLeast(0.0)

    val splitCash = splitCashInput.toDoubleOrNull() ?: (totalAmount / 2.0)
    val splitCard = (totalAmount - splitCash).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = EmanagerBlue)
                Text("Ödənişin İcrası", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Receipt summary box
                Surface(
                    color = EmanagerBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Alıcı:", fontSize = 12.sp, color = Slate500)
                            Text(customerName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("İlkin Cəmi:", fontSize = 12.sp, color = Slate500)
                            Text(String.format("%.2f ₼", subtotal), fontSize = 12.sp)
                        }
                        if (discountAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Endirim (${discountPercent.toInt()}%):", fontSize = 12.sp, color = EmanagerOrange)
                                Text("-%.2f ₼".format(discountAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmanagerOrange)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = EmanagerBlue.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Yekun Ödəniş:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                String.format("%.2f ₼", totalAmount),
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = EmanagerBlue
                            )
                        }
                    }
                }

                // Discount Selector Chips
                Text("Endirim Tətbiq Et:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { disc ->
                        val isSel = discountPercent == disc
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) EmanagerOrange else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).clickable { onDiscountPercentChanged(disc) }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (disc == 0.0) "0%" else "${disc.toInt()}%",
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Payment Method Selector
                Text("Ödəniş növü:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Nağd", "Kart", "Qarışıq", "Nisyə").forEach { method ->
                        val isSel = selectedPaymentMethod == method
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).clickable { selectedPaymentMethod = method }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = method,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Specific Method Controls:
                when (selectedPaymentMethod) {
                    "Nağd" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Müştəri Verdi (₼):", fontSize = 11.sp, color = Slate500)

                            OutlinedTextField(
                                value = cashTenderedInput,
                                onValueChange = { cashTenderedInput = it },
                                placeholder = { Text(String.format("%.2f", totalAmount)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick Presets: Exact, +1, 10, 20, 50, 100
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "Dəqiq" to totalAmount,
                                    "10₼" to 10.0,
                                    "20₼" to 20.0,
                                    "50₼" to 50.0,
                                    "100₼" to 100.0
                                ).forEach { (label, amt) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f).clickable {
                                            cashTenderedInput = String.format(Locale.US, "%.2f", amt)
                                        }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Change Indicator Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (shortageAmount <= 0.01) Color(0xFF10B981).copy(alpha = 0.15f) else EmanagerRose.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (shortageAmount <= 0.01) "Qaytarılacaq Qalıq:" else "Çatışmayan:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (shortageAmount <= 0.01) Color(0xFF047857) else EmanagerRose
                                    )
                                    Text(
                                        text = String.format("%.2f ₼", if (shortageAmount <= 0.01) changeAmount else shortageAmount),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = if (shortageAmount <= 0.01) Color(0xFF047857) else EmanagerRose
                                    )
                                }
                            }
                        }
                    }

                    "Qarışıq" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nağd hissəsi (₼):", fontSize = 11.sp, color = Slate500)
                            OutlinedTextField(
                                value = splitCashInput,
                                onValueChange = { splitCashInput = it },
                                placeholder = { Text(String.format("%.2f", totalAmount / 2.0)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmanagerBlue.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Kartla çıxılacaq məbləğ:", fontSize = 12.sp, color = EmanagerBlue)
                                    Text(String.format("%.2f ₼", splitCard), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmanagerBlue)
                                }
                            }
                        }
                    }

                    "Nisyə" -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmanagerRose.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Məbləğ alıcının (${customerName}) nisyə/borc hesabına yazılacaq.",
                                fontSize = 11.sp,
                                color = EmanagerRose,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmanagerBlue.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "POS terminal vasitəsilə kartla tam ödəniş (${String.format("%.2f ₼", totalAmount)}).",
                                fontSize = 11.sp,
                                color = EmanagerBlue,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashPaid = when (selectedPaymentMethod) {
                        "Nağd" -> tenderedCash
                        "Qarışıq" -> splitCash
                        else -> 0.0
                    }
                    val cardPaid = when (selectedPaymentMethod) {
                        "Kart" -> totalAmount
                        "Qarışıq" -> splitCard
                        else -> 0.0
                    }
                    onConfirmSale(selectedPaymentMethod, cashPaid, cardPaid)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen)
            ) {
                Text("Təsdiq Et və Çap")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ləğv") }
        }
    )
}

@Composable
fun DigitalReceiptDialog(
    receipt: ReceiptData,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(receipt.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Text("Satış Uğurla Tamamlandı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(receipt.companyName, fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("Qəbz №: ${receipt.invoiceNumber}", fontSize = 11.sp, color = Slate500, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(dateStr, fontSize = 10.sp, color = Slate500, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(receipt.items) { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}x ${item.product.name}", fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(String.format("%.2f ₼", item.totalPrice), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    if (receipt.discountAmount > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Endirim:", fontSize = 11.sp, color = EmanagerOrange)
                            Text("-%.2f ₼".format(receipt.discountAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmanagerOrange)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Yekun:", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(String.format("%.2f ₼", receipt.totalAmount), fontWeight = FontWeight.Black, fontSize = 15.sp, color = EmanagerBlue)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ödəniş:", fontSize = 11.sp, color = Slate500)
                        Text(receipt.paymentType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (receipt.changeGiven > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Qaytarılan Qalıq:", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                            Text(String.format("%.2f ₼", receipt.changeGiven), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
            ) {
                Text("Yeni Satış")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paylaş")
                }
                OutlinedButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Çap")
                }
            }
        }
    )
}

@Composable
fun HeldOrdersDialog(
    orders: List<HeldOrder>,
    onResume: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = EmanagerOrange)
                Text("Saxlanılan Səbətlər (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Hazırda saxlanılan səbət yoxdur.", color = Slate500, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders) { order ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(order.note, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(String.format("%.2f ₼", order.totalAmount), fontWeight = FontWeight.Black, fontSize = 13.sp, color = EmanagerBlue)
                                }
                                Text(
                                    text = "${order.customerName ?: "Pərakəndə Alıcı"} • ${order.itemCount} məhsul",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onDelete(order.id) }) {
                                        Text("Sil", color = EmanagerRose, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = { onResume(order.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Bərpa Et", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Bağla") }
        }
    )
}
