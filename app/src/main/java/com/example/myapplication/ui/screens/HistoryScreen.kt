package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.ReceiptData
import com.example.myapplication.data.TransactionRecord
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500
import com.example.myapplication.util.BluetoothPrinterHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val transactions by viewModel.transactions.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val cashierName by viewModel.cashierName.collectAsState()

    var selectedFilter by remember { mutableStateOf("Hamısı") }
    var selectedPeriod by remember { mutableStateOf("Hamısı") } // "Bugün", "Dünən", "Bu ay", "Hamısı"
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var selectedTxForDetails by remember { mutableStateOf<TransactionRecord?>(null) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionRecord?>(null) }
    var selectedTxForDelete by remember { mutableStateOf<TransactionRecord?>(null) }

    // Edit inputs
    var editCustomerName by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editPaymentType by remember { mutableStateOf("Nağd") }

    val filteredTransactions = remember(transactions, selectedFilter, selectedPeriod, searchQuery) {
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = startOfToday - 86400000L
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "Satış" -> tx.type == "sale" || tx.type.isBlank()
                "İadə" -> tx.type == "sale_return" || tx.type == "return"
                "Alış" -> tx.type == "purchase"
                "Mədaxil" -> tx.type == "income"
                "Məxaric" -> tx.type == "expense"
                else -> true
            }

            val matchesPeriod = when (selectedPeriod) {
                "Bugün" -> tx.timestamp >= startOfToday
                "Dünən" -> tx.timestamp in startOfYesterday until startOfToday
                "Bu ay" -> tx.timestamp >= startOfMonth
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    tx.id.contains(searchQuery, ignoreCase = true) ||
                    (tx.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true)

            matchesFilter && matchesPeriod && matchesSearch
        }
    }

    val totalTurnover = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "sale" || it.type.isBlank() }.sumOf { it.totalAmount }
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
                        Text("Məhsul siyahısı yoxdur (Kassa/Digər)", fontSize = 12.sp, color = Slate500)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = {
                        val receiptData = ReceiptData(
                            invoiceNumber = tx.id,
                            companyName = companyName,
                            cashierName = cashierName,
                            customerName = tx.customerName ?: "Pərakəndə Alıcı",
                            items = tx.items,
                            subtotal = if (tx.subtotalAmount > 0) tx.subtotalAmount else tx.totalAmount,
                            discountPercent = 0.0,
                            discountAmount = tx.discountAmount,
                            totalAmount = tx.totalAmount,
                            paymentType = tx.paymentType,
                            cashPaid = tx.cashPaid,
                            cardPaid = tx.cardPaid,
                            changeGiven = tx.changeGiven,
                            timestamp = tx.timestamp
                        )
                        BluetoothPrinterHelper.shareReceiptText(context, receiptData)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paylaş", fontSize = 12.sp)
                    }

                    OutlinedButton(onClick = {
                        val receiptData = ReceiptData(
                            invoiceNumber = tx.id,
                            companyName = companyName,
                            cashierName = cashierName,
                            customerName = tx.customerName ?: "Pərakəndə Alıcı",
                            items = tx.items,
                            subtotal = if (tx.subtotalAmount > 0) tx.subtotalAmount else tx.totalAmount,
                            discountPercent = 0.0,
                            discountAmount = tx.discountAmount,
                            totalAmount = tx.totalAmount,
                            paymentType = tx.paymentType,
                            cashPaid = tx.cashPaid,
                            cardPaid = tx.cardPaid,
                            changeGiven = tx.changeGiven,
                            timestamp = tx.timestamp
                        )
                        coroutineScope.launch {
                            BluetoothPrinterHelper.printSaleReceipt(null, receiptData)
                            Toast.makeText(context, "Çap göndərildi", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Çap", fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // 2. EDIT TRANSACTION DIALOG
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
                            newCustomerName = editCustomerName,
                            newPaymentType = editPaymentType,
                            newNote = editNote
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

    // 3. DELETE TRANSACTION DIALOG
    if (selectedTxForDelete != null) {
        val tx = selectedTxForDelete!!
        AlertDialog(
            onDismissRequest = { selectedTxForDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmanagerRose)
                    Text("Sənədi Silmək?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Tranzaksiya #${tx.id} (${String.format("%.2f ₼", tx.totalAmount)}) silinəcək və maliyyə balansından çıxarılacaq.",
                    fontSize = 13.sp,
                    color = Slate500
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
                TextButton(onClick = { selectedTxForDelete = null }) { Text("Ləğv") }
            }
        )
    }

    // MAIN SCREEN CONTENT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER
        Surface(
            color = EmanagerBlue,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Əməliyyat Tarixçəsi", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Period Summary Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filtr üzrə Satış Dövriyyəsi:", fontSize = 12.sp, color = Color(0xFFDBEAFE))
                        Text(String.format("%.2f ₼", totalTurnover), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Qəbz №, Alıcı və ya Qeyd axtarın...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // PERIOD CHIPS (Bugün, Dünən, Bu ay, Hamısı)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Hamısı", "Bugün", "Dünən", "Bu ay").forEach { p ->
                val isSel = selectedPeriod == p
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).clickable { selectedPeriod = p }
                ) {
                    Box(modifier = Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = p,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TYPE FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Hamısı", "Satış", "İadə", "Alış", "Mədaxil", "Məxaric").forEach { filter ->
                val isSel = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) EmanagerOrange else MaterialTheme.colorScheme.surfaceVariant,
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

        Spacer(modifier = Modifier.height(8.dp))

        // TRANSACTIONS LIST
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                    Text("Tarixçədə əməliyyat tapılmadı", fontSize = 13.sp, color = Slate500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    val isReturn = tx.type == "sale_return" || tx.type == "return"
                    val isPurchase = tx.type == "purchase"
                    val isExpense = tx.type == "expense"
                    val isIncome = tx.type == "income"

                    val badgeColor = when {
                        isReturn -> EmanagerRose
                        isPurchase -> EmanagerOrange
                        isExpense -> EmanagerRose
                        isIncome -> Color(0xFF10B981)
                        else -> EmanagerBlue
                    }

                    val badgeText = when {
                        isReturn -> "İADƏ"
                        isPurchase -> "ALIŞ"
                        isExpense -> "MƏXARİC"
                        isIncome -> "MƏDAXİL"
                        else -> "SATIŞ"
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(text = "#${tx.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Text(
                                    text = String.format("%.2f ₼", tx.totalAmount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (isReturn || isExpense) EmanagerRose else EmanagerBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tx.customerName ?: "Pərakəndə Alıcı"} • ${tx.paymentType}",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                                Text(text = dateStr, fontSize = 11.sp, color = Slate500)
                            }

                            if (!tx.note.isNullOrBlank()) {
                                Text(
                                    text = "Qeyd: ${tx.note}",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { selectedTxForDetails = tx },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Qəbz", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                OutlinedButton(
                                    onClick = {
                                        editCustomerName = tx.customerName ?: ""
                                        editNote = tx.note ?: ""
                                        editPaymentType = tx.paymentType
                                        selectedTxForEdit = tx
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Düzəliş", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { selectedTxForDelete = tx },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = EmanagerRose, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
