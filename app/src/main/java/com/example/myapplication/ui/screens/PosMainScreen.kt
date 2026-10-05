package com.example.myapplication.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Product
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.components.BarcodeScanner
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.Slate100
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMainScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val cashierName by viewModel.cashierName.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val isErpConnected by viewModel.isErpConnected.collectAsState()
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val customersList by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showSelectCustomerDialog by remember { mutableStateOf(false) }
    var showQuickCreateModal by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

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
        (listOf("Hamısı") + categoriesList.filter { it != "Hamısı" } + prodCats).distinct()
    }

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "Hamısı" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true) || (product.barcode?.contains(searchQuery) == true)
            matchesCategory && matchesSearch
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Barcode Camera Scanner Dialog
    if (showCameraScanner) {
        AlertDialog(
            onDismissRequest = { showCameraScanner = false },
            title = { Text("Barkod Skaner", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp))) {
                    BarcodeScanner(
                        modifier = Modifier.fillMaxSize(),
                        onBarcodeScanned = { barcode ->
                            viewModel.onBarcodeScanned(barcode)
                            showCameraScanner = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCameraScanner = false }) { Text("Bağla") }
            }
        )
    }

    // 2. Select Customer Dialog
    if (showSelectCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showSelectCustomerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = EmanagerBlue)
                    Text("Alıcı Seçin", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(customersList) { c ->
                        val isSelected = c.name == selectedCustomer
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmanagerBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EmanagerBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedCustomer(c.name)
                                    showSelectCustomerDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = c.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (!c.phone.isNullOrBlank()) {
                                        Text(text = c.phone, fontSize = 11.sp, color = Slate500)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmanagerBlue, modifier = Modifier.size(18.dp))
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
                }) {
                    Text("+ Yeni Müştəri", color = EmanagerBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectCustomerDialog = false }) { Text("Bağla") }
            }
        )
    }

    // 3. Quick Create Modal Dialog ("+ Yeni Yarat")
    if (showQuickCreateModal) {
        AlertDialog(
            onDismissRequest = { showQuickCreateModal = false },
            title = {
                Text("Yeni Əməliyyat Yarat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Nə yaratmaq istəyirsiniz?", fontSize = 13.sp, color = Slate500)

                    // Option 1: New Product
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickCreateModal = false
                                showAddProductDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(EmanagerBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddBox, contentDescription = null, tint = EmanagerBlue)
                            }
                            Column {
                                Text("Yeni Məhsul Yarat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Anbara yeni mal və ya xidmət əlavə edin", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }

                    // Option 2: New Customer
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickCreateModal = false
                                showAddCustomerDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(EmanagerGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = EmanagerGreen)
                            }
                            Column {
                                Text("Yeni Alıcı / Müştəri", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Siyahıya yeni müştəri qeyd edin", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }

                    // Option 3: Reset Cart
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.clearCart()
                                showQuickCreateModal = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(EmanagerOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = EmanagerOrange)
                            }
                            Column {
                                Text("Səbəti Sıfırla", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Cari seçilmiş məhsulları təmizləyin", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickCreateModal = false }) { Text("Bağla") }
            }
        )
    }

    // 4. Add Customer Dialog
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Yeni Alıcı Qeydiyyatı", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Alıcının Adı və Soyadı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustomerPhone,
                        onValueChange = { newCustomerPhone = it },
                        label = { Text("Telefon Nömrəsi (İstəyə bağlı)") },
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

    // 5. Add Product Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Yeni Məhsul Yarat", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
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
                            label = { Text("Maya Dəyəri (₼)") },
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
                        label = { Text("Barkod / Skan (Boşdursa avtomatik)") },
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
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Yadda Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 6. Fast Checkout Dialog
    if (showCheckoutDialog) {
        var selectedPaymentMethod by remember { mutableStateOf("Nağd") }
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = EmanagerBlue)
                    Text("Ödənişin İcrası", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = EmanagerBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Alıcı: ${selectedCustomer ?: "Pərakəndə Alıcı"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Yekun Məbləğ:", fontSize = 14.sp)
                                Text(
                                    String.format("%.2f ₼", totalAmount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = EmanagerBlue
                                )
                            }
                        }
                    }

                    Text("Ödəniş növünü seçin:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Nağd", "Kart", "Nisyə").forEach { method ->
                            val isSel = selectedPaymentMethod == method
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPaymentMethod = method }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitSale(selectedPaymentMethod, cashierName)
                        showCheckoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen)
                ) {
                    Text("Təsdiq Et və Çap Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) { Text("Ləğv") }
            }
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
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    .size(40.dp)
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

                        // Online / Sync status badge
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
                                    text = if (isErpConnected) "e-Manager Canlı" else "Oflayn",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. USER DIRECTIVE: TOP CUSTOMER SELECTION + "+ YENİ YARAT" BUTTON SIDE-BY-SIDE
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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

                        // "+ Yeni Yarat" Button (Modern vibrant accent)
                        Button(
                            onClick = { showQuickCreateModal = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Text("Yeni Yarat", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. SEARCH BAR & SCANNER ROW
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

                // Camera Scanner Button
                Surface(
                    onClick = { showCameraScanner = true },
                    shape = RoundedCornerShape(14.dp),
                    color = EmanagerBlue.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Skaner", tint = EmanagerBlue, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. CATEGORY HORIZONTAL PILLS
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

            Spacer(modifier = Modifier.height(8.dp))

            // 5. PRODUCTS CARDS GRID (RESPONSIVE 2-COLUMN)
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                        Text("Bu kateqoriyada məhsul tapılmadı", fontSize = 13.sp, color = Slate500)
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
                    contentPadding = PaddingValues(bottom = if (cartItems.isNotEmpty()) 80.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val itemInCart = cartItems.find { it.product.id == product.id }
                        ProductCard(
                            product = product,
                            quantityInCart = itemInCart?.quantity ?: 0,
                            onAddToCart = { viewModel.addToCart(product) },
                            onIncrement = { viewModel.updateQuantity(product.id, 1) },
                            onDecrement = { viewModel.updateQuantity(product.id, -1) }
                        )
                    }
                }
            }
        }

        // 6. FLOATING BOTTOM CART SUMMARY (IF ITEMS IN CART)
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerBlue.copy(alpha = 0.2f))
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
                            Text("Məhsul seçilib", fontSize = 12.sp, color = Slate500)
                        }
                        Text(
                            text = String.format("%.2f ₼", totalAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = EmanagerBlue
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Təmizlə", tint = Slate500)
                        }

                        Button(
                            onClick = { showCheckoutDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Ödəniş", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    quantityInCart: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
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
            // Category Tag & Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = product.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${product.stockQuantity.toInt()} ${product.unit}",
                    fontSize = 10.sp,
                    color = if (product.stockQuantity < 5.0) Color(0xFFEF4444) else Slate500,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Price & Counter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.2f ₼", product.price),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = EmanagerBlue
                )

                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = onDecrement,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Text("$quantityInCart", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Surface(
                            onClick = onIncrement,
                            shape = CircleShape,
                            color = EmanagerBlue,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onAddToCart,
                        shape = CircleShape,
                        color = EmanagerBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EmanagerBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
