package com.example.myapplication.data

import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.api.SaleItem
import com.example.myapplication.data.api.SaleRequest
import com.example.myapplication.data.api.SaleResponse
import com.example.myapplication.data.db.PendingSaleEntity
import com.example.myapplication.data.db.PosDatabase
import com.example.myapplication.data.db.ProductEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PosRepository(database: PosDatabase) {
    private val apiService = ApiClient.erpApiService
    private val productDao = database.productDao()
    private val pendingSaleDao = database.pendingSaleDao()
    private val gson = Gson()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cartItems = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cartItems: StateFlow<Map<String, CartItem>> = _cartItems.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            productDao.getAllProducts().collect { entities ->
                _products.value = entities.map { it.toProduct() }
            }
        }
    }

    suspend fun fetchProductsFromErp(search: String? = null): Boolean {
        try {
            val response = apiService.getProducts(search = search)
            if (response.isSuccessful && response.body()?.success == true) {
                val remoteProducts = response.body()?.products.orEmpty()
                val entities = remoteProducts.map { dto ->
                    ProductEntity(
                        id = dto.id,
                        productNumber = dto.productNumber,
                        name = dto.name,
                        sku = dto.sku,
                        barcode = dto.barcode,
                        price = dto.price,
                        salePrice = dto.salePrice,
                        costPrice = dto.costPrice,
                        unit = dto.unit,
                        stock = dto.stock,
                        stockQuantity = dto.stockQuantity,
                        taxRate = dto.taxRate,
                        isVatApplicable = dto.isVatApplicable
                    )
                }
                productDao.insertAll(entities)
                syncOfflineSales()
                return true
            }
        } catch (e: Exception) {
            // Offline or error, Room remains our source of truth
        }
        return false
    }

    suspend fun submitSale(saleRequest: SaleRequest): SaleResponse? {
        var responseBody: SaleResponse? = null
        try {
            val response = apiService.submitSale(saleRequest)
            if (response.isSuccessful && response.body()?.success == true) {
                responseBody = response.body()
            } else {
                saveSaleOffline(saleRequest)
            }
        } catch (e: Exception) {
            saveSaleOffline(saleRequest)
        }

        // Record in transaction history
        val invoiceId = responseBody?.data?.invoiceNumber ?: "INV-${System.currentTimeMillis() % 1000000}"
        val record = TransactionRecord(
            id = invoiceId,
            items = saleRequest.items.map { item ->
                CartItem(
                    Product(
                        id = item.productId,
                        name = item.name,
                        price = item.price,
                        category = "Ümumi"
                    ),
                    quantity = item.quantity.toInt()
                )
            },
            totalAmount = saleRequest.totalAmount,
            timestamp = System.currentTimeMillis()
        )
        _transactions.value = listOf(record) + _transactions.value

        return responseBody
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.searchByBarcode(barcode).firstOrNull()?.toProduct()
    }

    private suspend fun saveSaleOffline(saleReq: SaleRequest) {
        val json = gson.toJson(saleReq)
        pendingSaleDao.insert(
            PendingSaleEntity(
                saleRequestJson = json,
                isSynced = false
            )
        )
    }

    suspend fun syncOfflineSales() {
        val unsynced = pendingSaleDao.getUnsyncedSales()
        for (pending in unsynced) {
            try {
                val req = gson.fromJson(pending.saleRequestJson, SaleRequest::class.java)
                val response = apiService.submitSale(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    pendingSaleDao.markAsSynced(pending.id)
                }
            } catch (e: Exception) {
                // Ignore, try again later
            }
        }
    }


}
