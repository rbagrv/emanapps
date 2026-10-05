package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Product
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val totalStockQty by viewModel.totalStockQty.collectAsState()
    val totalStockValue by viewModel.totalStockValue.collectAsState()
    val totalDamagedQty by viewModel.totalDamagedQty.collectAsState()
    val totalDamagedValue by viewModel.totalDamagedValue.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Dialog toggles
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showDamagedDialog by remember { mutableStateOf(false) }
    var selectedProductForDamage by remember { mutableStateOf<Product?>(null) }

    // Dialog inputs
    var newProdName by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdCost by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("Ümumi") }
    var newProdBarcode by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("100") }

    var newCategoryName by remember { mutableStateOf("") }

    var selectedSupplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Bolluq MMC") }
    var selectedPurchaseProdId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var purchaseQty by remember { mutableStateOf("10") }
    var purchasePrice by remember { mutableStateOf("") }

    var newSupName by remember { mutableStateOf("") }
    var newSupPhone by remember { mutableStateOf("") }
    var newSupCompany by remember { mutableStateOf("") }

    var damagedQtyInput by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        products.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || (it.barcode?.contains(searchQuery) == true)
        }
    }

    // 1. ADD PRODUCT DIALOG
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddBox, contentDescription = null, tint = EmanagerBlue)
                    Text("Yeni Məhsul Yarat", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
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
                        value = newProdStock,
                        onValueChange = { newProdStock = it },
                        label = { Text("İlkin Qalıq Miqdarı") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        label = { Text("Barkod (Boşdursa avtomatik)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = newProdPrice.toDoubleOrNull() ?: 0.0
                        val c = newProdCost.toDoubleOrNull() ?: 0.0
                        val s = newProdStock.toDoubleOrNull() ?: 100.0
                        if (newProdName.isNotBlank() && p > 0.0) {
                            viewModel.addNewProduct(
                                name = newProdName,
                                price = p,
                                costPrice = c,
                                category = newProdCategory,
                                barcode = newProdBarcode,
                                stockQuantity = s
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

    // 2. ADD CATEGORY DIALOG
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Yeni Kateqoriya Yarat", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Kateqoriya Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Əlavə Et") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 3. PURCHASE OPERATION DIALOG (ALIŞ ƏMƏLİYYATI)
    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = EmanagerOrange)
                    Text("Alış Əməliyyatı İcra Et", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Təchizatçı:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedSupplierName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text("Məhsul:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val prod = products.find { it.id == selectedPurchaseProdId } ?: products.firstOrNull()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = prod?.name ?: "Məhsul seçilməyib",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchaseQty,
                            onValueChange = { purchaseQty = it },
                            label = { Text("Miqdar (əd) *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("Alış Qiyməti (₼) *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = purchaseQty.toDoubleOrNull() ?: 0.0
                        val pr = purchasePrice.toDoubleOrNull() ?: 0.0
                        val targetProdId = selectedPurchaseProdId.ifBlank { products.firstOrNull()?.id ?: "" }
                        if (targetProdId.isNotBlank() && qty > 0.0 && pr > 0.0) {
                            viewModel.addPurchaseOperation(
                                supplierName = selectedSupplierName,
                                productId = targetProdId,
                                quantity = qty,
                                purchasePrice = pr
                            )
                            showPurchaseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange)
                ) { Text("Alışı Təsdiqlə") }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 4. ADD SUPPLIER DIALOG (TƏCHİZATÇI YARAT)
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = EmanagerBlue)
                    Text("Yeni Təchizatçı Yarat", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newSupName,
                        onValueChange = { newSupName = it },
                        label = { Text("Təchizatçının Adı *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupPhone,
                        onValueChange = { newSupPhone = it },
                        label = { Text("Telefon") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupCompany,
                        onValueChange = { newSupCompany = it },
                        label = { Text("Şirkət / Firma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSupName.isNotBlank()) {
                            viewModel.addSupplier(newSupName, newSupPhone, newSupCompany)
                            newSupName = ""
                            newSupPhone = ""
                            newSupCompany = ""
                            showAddSupplierDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Yadda Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 5. DAMAGED / VOZVRAT PRODUCT LOGGING DIALOG
    if (showDamagedDialog && selectedProductForDamage != null) {
        val prod = selectedProductForDamage!!
        AlertDialog(
            onDismissRequest = { showDamagedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmanagerRose)
                    Text("Xarab Mal Qeydiyyatı", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Cari qalıq: ${prod.stockQuantity.toInt()} ${prod.unit}", fontSize = 12.sp, color = Slate500)
                    OutlinedTextField(
                        value = damagedQtyInput,
                        onValueChange = { damagedQtyInput = it },
                        label = { Text("Xarab / Vozvrat Miqdarı (${prod.unit})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dQty = damagedQtyInput.toDoubleOrNull() ?: 0.0
                        if (dQty > 0.0) {
                            viewModel.markProductAsDamaged(prod.id, dQty)
                            damagedQtyInput = ""
                            showDamagedDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerRose)
                ) { Text("Xarab kimi Qeyd Et") }
            },
            dismissButton = {
                TextButton(onClick = { showDamagedDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // MAIN CONTENT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER
        Surface(
            color = EmanagerBlue,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("ANBAR VƏ EHTİYAT İDARƏETMƏSİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDBEAFE))
                Spacer(modifier = Modifier.height(10.dp))

                // TOP USER DIRECTIVE: MƏHSUL QALIĞI PUL VƏ MİQDARLA, YANINDA XARAB MAL MİQDARI VƏ MƏBLƏĞİ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Məhsul Qalığı Card (AZN & Qty)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Məhsul Qalığı", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%.2f ₼", totalStockValue),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${totalStockQty.toInt()} ədəd ehtiyat",
                                fontSize = 11.sp,
                                color = Color(0xFFDBEAFE)
                            )
                        }
                    }

                    // 2. Xarab Mal Card (AZN & Qty)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFD1D1), modifier = Modifier.size(16.dp))
                                Text("Xarab Mal (Vozvrat)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD1D1))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%.2f ₼", totalDamagedValue),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD1D1)
                            )
                            Text(
                                text = "${totalDamagedQty.toInt()} ədəd xarab",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 ACTION BUTTONS: + MƏHSUL, + KATEQORİYA, + ALIŞ ƏMƏLİYYATI, + TƏCHİZATÇI
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddProductDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Məhsul Yarat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kateqoriya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (products.isNotEmpty()) selectedPurchaseProdId = products.first().id
                        if (suppliers.isNotEmpty()) selectedSupplierName = suppliers.first().name
                        showPurchaseDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Alış Əməliyyatı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddSupplierDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Təchizatçı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Anbar məhsullarında axtarın...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // PRODUCTS LIST WITH DETAILS & DAMAGED ACTIONS
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredProducts, key = { it.id }) { product ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(product.category, fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    if (!product.barcode.isNullOrBlank()) {
                                        Text(text = "Barkod: ${product.barcode}", fontSize = 10.sp, color = Slate500)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.2f ₼", product.price),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = EmanagerBlue
                                )
                                Text(
                                    text = "Maya: ${String.format("%.2f ₼", product.costPrice)}",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Slate200.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Qalıq: ${product.stockQuantity.toInt()} ${product.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.stockQuantity < 5.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                )
                                if (product.damagedQuantity > 0.0) {
                                    Text(
                                        text = "Xarab: ${product.damagedQuantity.toInt()} ${product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmanagerRose
                                    )
                                }
                            }

                            // Log Damaged Item Button
                            TextButton(
                                onClick = {
                                    selectedProductForDamage = product
                                    showDamagedDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = EmanagerRose, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xarab Mal Yaz", fontSize = 11.sp, color = EmanagerRose, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
