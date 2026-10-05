package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.TransactionRecord
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    var selectedFilter by remember { mutableStateOf("Hamısı") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var selectedTxForDetails by remember { mutableStateOf<TransactionRecord?>(null) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionRecord?>(null) }
    var selectedTxForDelete by remember { mutableStateOf<TransactionRecord?>(null) }

    // Edit inputs
    var editCustomerName by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editPaymentType by remember { mutableStateOf("Nağd") }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "Satış" -> tx.type == "sale" || tx.type.isBlank()
                "İadə" -> tx.type == "sale_return" || tx.type == "return"
                "Alış" -> tx.type == "purchase"
                "Mədaxil" -> tx.type == "income"
                "Məxaric" -> tx.type == "expense"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.id.contains(searchQuery, ignoreCase = true) ||
                    (tx.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }
    }

    // 1. DETAIL / RECEIPT VIEW DIALOG
    if (selectedTxForDetails != null) {
        val tx = selectedTxForDetails!!
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(tx.timestamp))
        AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = EmanagerBlue)
                    Text("Əməliyyat Çeki #${tx.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tarix:", fontSize = 12.sp, color = Slate500)
                        Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Alıcı / Tərəf:", fontSize = 12.sp, color = Slate500)
                        Text(tx.customerName ?: "Pərakəndə Alıcı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ödəniş növü:", fontSize = 12.sp, color = Slate500)
                        Text(tx.paymentType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmanagerBlue)
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Qeyd:", fontSize = 12.sp, color = Slate500)
                            Text(tx.note, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    Text("Məhsullar:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (tx.items.isEmpty()) {
                        Text("Məhsul siyahısı mövcud deyil (Kassa əməliyyatı)", fontSize = 12.sp, color = Slate500)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tx.items) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("${item.quantity} x ${String.format("%.2f ₼", item.product.price)}", fontSize = 11.sp, color = Slate500)
                                    }
                                    Text(String.format("%.2f ₼", item.totalPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yekun Məbləğ:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            String.format("%.2f ₼", tx.totalAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = EmanagerBlue
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxForDetails = null }) { Text("Bağla") }
            }
        )
    }

    // 2. EDIT TRANSACTION DIALOG (REDAKTƏ ET)
    if (selectedTxForEdit != null) {
        val tx = selectedTxForEdit!!
        AlertDialog(
            onDismissRequest = { selectedTxForEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = EmanagerBlue)
                    Text("Əməliyyatı Redaktə Et", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Sənəd: #${tx.id} (${String.format("%.2f ₼", tx.totalAmount)})", fontSize = 12.sp, color = Slate500)
                    OutlinedTextField(
                        value = editCustomerName,
                        onValueChange = { editCustomerName = it },
                        label = { Text("Alıcı / Müştəri Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Qeyd") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Ödəniş Növü:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Nağd", "Kart", "Nisyə").forEach { opt ->
                            val isSel = editPaymentType == opt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { editPaymentType = opt }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
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
                        viewModel.editTransaction(
                            transactionId = tx.id,
                            newCustomerName = editCustomerName.ifBlank { null },
                            newNote = editNote.ifBlank { null },
                            newPaymentType = editPaymentType
                        )
                        selectedTxForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Yadda Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForEdit = null }) { Text("Ləğv") }
            }
        )
    }

    // 3. DELETE TRANSACTION CONFIRMATION DIALOG (SİL / LƏĞV ET)
    if (selectedTxForDelete != null) {
        val tx = selectedTxForDelete!!
        AlertDialog(
            onDismissRequest = { selectedTxForDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = EmanagerRose)
                    Text("Əməliyyatı Sil", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    text = "Diqqət: #${tx.id} nömrəli ${String.format("%.2f ₼", tx.totalAmount)} məbləğində əməliyyat silinəcək və sistemdən ləğv ediləcək. Davam edilsin?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        selectedTxForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerRose)
                ) { Text("Bəli, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForDelete = null }) { Text("İmtina") }
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
                Text("SATİŞ VƏ ƏMƏLİYYAT TARİXÇƏSİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDBEAFE))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bütün Əməliyyatlar", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) {
                        Text(
                            text = "${filteredTransactions.size} sənəd",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Faktura No, Alıcı və ya Qeyd axtarın...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Hamısı", "Satış", "İadə", "Alış", "Mədaxil", "Məxaric").forEach { filter ->
                val isSel = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TRANSACTIONS LIST WITH VIEW, EDIT, AND DELETE
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                    Text("Tarixçədə heç bir əməliyyat tapılmadı", fontSize = 13.sp, color = Slate500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    val isReturn = tx.type == "sale_return" || tx.type == "return"
                    val isPurchase = tx.type == "purchase"
                    val isIncome = tx.type == "income"
                    val isExpense = tx.type == "expense"

                    val typeLabel = when {
                        isReturn -> "İadə"
                        isPurchase -> "Alış"
                        isIncome -> "Mədaxil"
                        isExpense -> "Məxaric"
                        else -> "Satış"
                    }

                    val typeColor = when {
                        isReturn || isExpense -> EmanagerRose
                        isIncome || (!isPurchase && !isReturn) -> EmanagerGreen
                        isPurchase -> EmanagerOrange
                        else -> EmanagerBlue
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row: ID, Type Badge, Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = typeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = typeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(text = "#${tx.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = dateStr, fontSize = 11.sp, color = Slate500)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Customer & Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tx.customerName ?: "Pərakəndə Alıcı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Ödəniş: ${tx.paymentType}" + (if (!tx.note.isNullOrBlank()) " • ${tx.note}" else ""),
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }

                                Text(
                                    text = String.format("%.2f ₼", tx.totalAmount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = if (isReturn || isExpense) EmanagerRose else EmanagerBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Slate200.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // USER DIRECTIVE: BAXMAQ, REDAKTƏ ETMƏK, SİLMƏK
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { selectedTxForDetails = tx },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmanagerBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bax", fontSize = 12.sp, color = EmanagerBlue, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        editCustomerName = tx.customerName ?: ""
                                        editNote = tx.note ?: ""
                                        editPaymentType = tx.paymentType
                                        selectedTxForEdit = tx
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Redaktə", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { selectedTxForDelete = tx },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmanagerRose)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sil", fontSize = 12.sp, color = EmanagerRose, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
