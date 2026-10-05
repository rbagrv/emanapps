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
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val cashBalance by viewModel.cashBalance.collectAsState()
    val cashOperations by viewModel.cashOperations.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Hamısı") }

    // Dialog state
    var opTitle by remember { mutableStateOf("") }
    var opAmount by remember { mutableStateOf("") }
    var opCategory by remember { mutableStateOf("") }
    var opNote by remember { mutableStateOf("") }

    // Unified Cash Flow items
    data class CashFlowItem(
        val id: String,
        val type: String, // "income" or "expense"
        val title: String,
        val category: String,
        val amount: Double,
        val timestamp: Long,
        val isSale: Boolean = false
    )

    val allCashFlows = remember(cashOperations, transactions) {
        val ops = cashOperations.map {
            CashFlowItem(
                id = it.id,
                type = it.type,
                title = it.title,
                category = it.category,
                amount = it.amount,
                timestamp = it.timestamp
            )
        }
        val sales = transactions.filter { (it.type == "sale" || it.type.isBlank()) && it.paymentType == "Nağd" }.map {
            CashFlowItem(
                id = it.id,
                type = "income",
                title = "Nağd Satış (${it.customerName ?: "Pərakəndə"})",
                category = "Satış Kassa",
                amount = it.totalAmount,
                timestamp = it.timestamp,
                isSale = true
            )
        }
        val purchases = transactions.filter { it.type == "purchase" && it.paymentType == "Nağd" }.map {
            CashFlowItem(
                id = it.id,
                type = "expense",
                title = "Mal Alışı (${it.customerName ?: "Təchizatçı"})",
                category = "Alış Xərci",
                amount = it.totalAmount,
                timestamp = it.timestamp,
                isSale = false
            )
        }
        (ops + sales + purchases).sortedByDescending { it.timestamp }
    }

    val filteredCashFlows = remember(allCashFlows, selectedFilter) {
        when (selectedFilter) {
            "Mədaxillər" -> allCashFlows.filter { it.type == "income" }
            "Məxariclər" -> allCashFlows.filter { it.type == "expense" }
            else -> allCashFlows
        }
    }

    // 1. MƏDAXİL (CASH IN) DIALOG
    if (showIncomeDialog) {
        AlertDialog(
            onDismissRequest = { showIncomeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmanagerGreen)
                    Text("Yeni Kassa Mədaxili (+)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = opTitle,
                        onValueChange = { opTitle = it },
                        label = { Text("Təyinat / Başlıq *") },
                        placeholder = { Text("Məs: Təsisçi yatırımı, Əlavə gəlir") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opAmount,
                        onValueChange = { opAmount = it },
                        label = { Text("Mədaxil Məbləği (₼) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opCategory,
                        onValueChange = { opCategory = it },
                        label = { Text("Kateqoriya") },
                        placeholder = { Text("Məs: Təsisçi, Digər gəlir, Borc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opNote,
                        onValueChange = { opNote = it },
                        label = { Text("Qeyd (İstəyə bağlı)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = opAmount.toDoubleOrNull() ?: 0.0
                        if (opTitle.isNotBlank() && amount > 0.0) {
                            viewModel.addCashOperation(
                                type = "income",
                                title = opTitle,
                                amount = amount,
                                category = opCategory.ifBlank { "Ümumi Mədaxil" },
                                note = opNote
                            )
                            opTitle = ""
                            opAmount = ""
                            opCategory = ""
                            opNote = ""
                            showIncomeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen)
                ) {
                    Text("Mədaxil Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncomeDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // 2. MƏXARİC (CASH OUT) DIALOG
    if (showExpenseDialog) {
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = EmanagerRose)
                    Text("Yeni Kassa Məxarici (-)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = opTitle,
                        onValueChange = { opTitle = it },
                        label = { Text("Təyinat / Məqsəd *") },
                        placeholder = { Text("Məs: İcarə haqqı, Əmək haqqı, Kommunal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opAmount,
                        onValueChange = { opAmount = it },
                        label = { Text("Məxaric Məbləği (₼) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opCategory,
                        onValueChange = { opCategory = it },
                        label = { Text("Xərc Kateqoriyası") },
                        placeholder = { Text("Məs: İcarə, Əmək haqqı, Təchizat, Ofis") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opNote,
                        onValueChange = { opNote = it },
                        label = { Text("Qeyd (İstəyə bağlı)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = opAmount.toDoubleOrNull() ?: 0.0
                        if (opTitle.isNotBlank() && amount > 0.0) {
                            viewModel.addCashOperation(
                                type = "expense",
                                title = opTitle,
                                amount = amount,
                                category = opCategory.ifBlank { "Ümumi Məxaric" },
                                note = opNote
                            )
                            opTitle = ""
                            opAmount = ""
                            opCategory = ""
                            opNote = ""
                            showExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerRose)
                ) {
                    Text("Məxaric Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // MAIN KASSA CONTENT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BANNER
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
                Text("KASSA VƏ PUL DÖVRİYYƏSİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDBEAFE))
                Spacer(modifier = Modifier.height(6.dp))

                // Kassa Qalığı Main Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kassa Qalığı (Cari Nağd)", fontSize = 12.sp, color = Color(0xFFDBEAFE))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format("%.2f ₼", cashBalance),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // GƏLİRLƏR & XƏRCLƏR (SIDE-BY-SIDE CARDS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Income Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmanagerGreen.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerGreen.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmanagerGreen, modifier = Modifier.size(16.dp))
                        Text("Gəlirlər", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmanagerGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("+ %.2f ₼", totalIncome),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = EmanagerGreen
                    )
                }
            }

            // Expense Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmanagerRose.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerRose.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = EmanagerRose, modifier = Modifier.size(16.dp))
                        Text("Xərclər", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmanagerRose)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("- %.2f ₼", totalExpense),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = EmanagerRose
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ACTION BUTTONS: + MƏDAXİL & - MƏXARİC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showIncomeDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("+ Mədaxil Yarat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Button(
                onClick = { showExpenseDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerRose),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("- Məxaric Yarat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FILTER TABS & TRANSACTIONS LIST
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Hamısı", "Mədaxillər", "Məxariclər").forEach { filter ->
                val isSel = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TRANSACTIONS LIST
        if (filteredCashFlows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Slate500, modifier = Modifier.size(42.dp))
                    Text("Kassa əməliyyatı yoxdur", fontSize = 13.sp, color = Slate500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredCashFlows, key = { it.id }) { item ->
                    val isIncome = item.type == "income"
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                        .background(if (isIncome) EmanagerGreen.copy(alpha = 0.15f) else EmanagerRose.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isIncome) EmanagerGreen else EmanagerRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 10.sp,
                                                color = Slate500,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        val dateStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                        Text(text = dateStr, fontSize = 10.sp, color = Slate500)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = String.format("%s %.2f ₼", if (isIncome) "+" else "-", item.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isIncome) EmanagerGreen else EmanagerRose
                                )
                                if (!item.isSale) {
                                    IconButton(
                                        onClick = { viewModel.deleteTransaction(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Sil", tint = Slate500, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
