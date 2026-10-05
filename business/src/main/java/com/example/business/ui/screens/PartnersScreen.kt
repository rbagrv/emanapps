package com.example.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.EmeraldPrimary

enum class PartnerTypeTab {
    DEBTORS,
    SUPPLIERS
}

@Composable
fun PartnersScreen(
    viewModel: BusinessViewModel,
    onNavigateToAddDebtor: () -> Unit,
    onNavigateToAddSupplier: () -> Unit,
    initialTab: PartnerTypeTab = PartnerTypeTab.DEBTORS,
    modifier: Modifier = Modifier
) {
    var selectedPartnerTab by remember { mutableStateOf(initialTab) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedPartnerTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            Tab(
                selected = selectedPartnerTab == PartnerTypeTab.DEBTORS,
                onClick = { selectedPartnerTab = PartnerTypeTab.DEBTORS },
                text = { Text("Müştəri Borcları (Debitor)", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedPartnerTab == PartnerTypeTab.SUPPLIERS,
                onClick = { selectedPartnerTab = PartnerTypeTab.SUPPLIERS },
                text = { Text("Təchizatçılar (Kreditor)", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        when (selectedPartnerTab) {
            PartnerTypeTab.DEBTORS -> {
                DebtorsScreen(
                    viewModel = viewModel,
                    onNavigateToAddDebtor = onNavigateToAddDebtor,
                    modifier = Modifier.fillMaxSize()
                )
            }
            PartnerTypeTab.SUPPLIERS -> {
                SuppliersScreen(
                    viewModel = viewModel,
                    onNavigateToAddSupplier = onNavigateToAddSupplier,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
