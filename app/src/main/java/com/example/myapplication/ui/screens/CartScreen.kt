package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.CartItem
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerOrange
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500

@Composable
fun CartScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotalAmount by viewModel.subtotalAmount.collectAsState()
    val discountTotal by viewModel.discountTotal.collectAsState()
    val cartDiscountPercent by viewModel.cartDiscountPercent.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val cashierName by viewModel.cashierName.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showHoldPromptDialog by remember { mutableStateOf(false) }
    var holdNoteInput by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf("Nağd") }

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
                    Text("Bu səbəti saxlamaq üçün qeyd daxil edin:", fontSize = 13.sp, color = Slate500)
                    OutlinedTextField(
                        value = holdNoteInput,
                        onValueChange = { holdNoteInput = it },
                        placeholder = { Text("Məs: Masa 1, Əli bəy") },
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerOrange)
                ) { Text("Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showHoldPromptDialog = false }) { Text("Ləğv") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aktiv Səbət",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Müştəri: ${selectedCustomer ?: "Pərakəndə Alıcı"}",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            if (cartItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showHoldPromptDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = EmanagerOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Saxla", color = EmanagerOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Slate500, modifier = Modifier.size(56.dp))
                    Text(
                        text = "Səbətiniz hazırda boşdur",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Slate500
                    )
                    Text(
                        text = "Əsas ekrandan məhsul seçin və ya barkodu skan edin.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onAdd = { viewModel.updateQuantity(item.product.id, 1) },
                        onRemove = { viewModel.updateQuantity(item.product.id, -1) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Discount Chips selector
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Səbətə Endirim:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { disc ->
                        val isSel = cartDiscountPercent == disc
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) EmanagerOrange else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).clickable { viewModel.setCartDiscountPercent(disc) }
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("İlkin Cəmi:", fontSize = 12.sp, color = Slate500)
                        Text(String.format("%.2f ₼", subtotalAmount), fontSize = 12.sp)
                    }

                    if (discountTotal > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Endirim (${cartDiscountPercent.toInt()}%):", fontSize = 12.sp, color = EmanagerOrange)
                            Text("-%.2f ₼".format(discountTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmanagerOrange)
                        }
                    }

                    val vatVal = totalAmount * 0.18 / 1.18
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ƏDV (18% daxil):", fontSize = 11.sp, color = Slate500)
                        Text(String.format("%.2f ₼", vatVal), fontSize = 11.sp, color = Slate500)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yekun məbləğ:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            String.format("%.2f ₼", totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = EmanagerBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Method Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Nağd", "Kart", "Nisyə").forEach { type ->
                    val isSel = selectedPaymentType == type
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).clickable { selectedPaymentType = type }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = type,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearCart() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Təmizlə")
                }
                Button(
                    onClick = {
                        viewModel.submitSale(paymentType = selectedPaymentType, cashierName = cashierName)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                    modifier = Modifier.weight(1.5f),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Gözləyin..." else "Ödənişi Tamamla ($selectedPaymentType)")
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = String.format("%.2f ₼ x %d", item.unitPriceAfterDiscount, item.quantity),
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    if (item.isReturn) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmanagerRose.copy(alpha = 0.15f)
                        ) {
                            Text("İadə", fontSize = 10.sp, color = EmanagerRose, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "${item.quantity}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                Surface(
                    onClick = onAdd,
                    shape = CircleShape,
                    color = EmanagerBlue,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = String.format("%.2f ₼", item.totalPrice),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = EmanagerBlue
                )
            }
        }
    }
}
