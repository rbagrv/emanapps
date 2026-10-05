package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.CartItem
import com.example.myapplication.ui.PosViewModel

@Composable
fun CartScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saleResult by viewModel.saleResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastCheckoutId by remember { mutableStateOf("") }
    
    // Show success dialog when saleResult updates
    LaunchedEffect(saleResult) {
        saleResult?.let { result ->
            if (result.success) {
                lastCheckoutId = result.data?.invoiceNumber ?: "Gözləyir..."
                showSuccessDialog = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Səbət",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Səbətiniz boşdur",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
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

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Yekun məbləğ:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("₼%.2f".format(totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearCart() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Səbəti təmizlə")
                }
                Button(
                    onClick = {
                        viewModel.submitSale(paymentType = "Nağd", cashierName = "Operator")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Gözləyin..." else "Ödənişi tamamla")
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Ödəniş uğurludur!") },
            text = { Text("Tranzaksiya kodu: $lastCheckoutId\nMəbləğ: ₼%.2f".format(totalAmount)) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.product.name, fontWeight = FontWeight.Bold)
                Text(text = "₼%.2f x ${item.quantity}".format(item.product.price), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(text = "${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                IconButton(onClick = onAdd) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
