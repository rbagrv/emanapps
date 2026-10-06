package com.example.business.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class D1TenantsResponse(
    val success: Boolean,
    val tenant: D1TenantDto?,
    val tenants: List<D1TenantDto>?
)

data class D1TenantDto(
    val id: String,
    val name: String,
    val voen: String?,
    val token: String?
)

data class D1ProductsResponse(
    val success: Boolean,
    val count: Int,
    val total: Int,
    val products: List<D1ProductDto>?
)

data class D1ProductDto(
    val id: String,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val unit: String?,
    val price: Double?,
    val salePrice: Double?,
    val costPrice: Double?,
    val stock: Double?,
    val stockQuantity: Double?,
    val categoryId: String?
)

data class D1CustomersResponse(
    val success: Boolean,
    val count: Int,
    val total: Int,
    val customers: List<D1CustomerDto>?
)

data class D1CustomerDto(
    val id: String,
    val name: String,
    val voen: String?,
    val phone: String?,
    val email: String?,
    val balance: Double?,
    val debt: Double?
)

data class D1SalesResponse(
    val success: Boolean,
    val count: Int,
    val total: Int?,
    val sales: List<D1SaleDto>?
)

data class D1SaleDto(
    val id: String,
    val invoiceNumber: String?,
    val date: String?,
    val customerName: String?,
    val customerId: String?,
    val totalAmount: Double?,
    val paidAmount: Double?,
    val balanceDue: Double?,
    val status: String?,
    val paymentType: String?,
    val createdAt: String?
)

// Direct Cloudflare D1 SQL query models
data class D1SqlQueryRequest(
    val sql: String,
    val params: List<Any>? = null
)

data class D1SqlQueryResultItem<T>(
    val results: List<T>?,
    val success: Boolean
)

data class D1SqlQueryResponse<T>(
    val result: List<D1SqlQueryResultItem<T>>?,
    val success: Boolean,
    val errors: List<Map<String, Any>>?
)

data class D1RawProduct(
    val id: String,
    val code: String?,
    val name: String,
    val barcode: String?,
    val category: String?,
    val unit: String?,
    @SerializedName("cost_price") val costPrice: Double?,
    @SerializedName("sale_price") val salePrice: Double?,
    @SerializedName("stock_quantity") val stockQuantity: Double?,
    @SerializedName("min_stock") val minStock: Double?
)

data class D1RawCustomer(
    val id: String,
    val name: String,
    val voen: String?,
    val phone: String?,
    val balance: Double?
)

data class D1RawSupplier(
    val id: String,
    val name: String,
    val voen: String?,
    val phone: String?,
    val balance: Double?
)

data class D1RawInvoice(
    val id: String,
    @SerializedName("invoice_number") val invoiceNumber: String?,
    @SerializedName("customer_id") val customerId: String?,
    val date: String?,
    val status: String?,
    @SerializedName("total_amount") val totalAmount: Double?,
    @SerializedName("paid_amount") val paidAmount: Double?,
    @SerializedName("balance_due") val balanceDue: Double?
)

data class D1RawPosSale(
    val id: String,
    @SerializedName("tenant_id") val tenantId: String?,
    @SerializedName("cheque_date") val chequeDate: String?,
    @SerializedName("gross_total") val grossTotal: Double?,
    @SerializedName("cash_amount") val cashAmount: Double?,
    @SerializedName("cashless_amount") val cashlessAmount: Double?
)

data class D1RawAccount(
    val code: String,
    val name: String,
    @SerializedName("current_balance") val currentBalance: Double?,
    val normal: String?
)

interface D1RestApiService {
    @GET("api/extension/tenants")
    suspend fun getTenants(): Response<D1TenantsResponse>

    @GET("api/products")
    suspend fun getProducts(
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 1000
    ): Response<D1ProductsResponse>

    @GET("api/customers")
    suspend fun getCustomers(
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 1000
    ): Response<D1CustomersResponse>

    @GET("api/sales")
    suspend fun getSales(
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 1000
    ): Response<D1SalesResponse>
}

interface CloudflareD1DirectService {
    @POST("client/v4/accounts/{accountId}/d1/database/{databaseId}/query")
    suspend fun queryRaw(
        @Path("accountId") accountId: String,
        @Path("databaseId") databaseId: String,
        @Header("Authorization") authHeader: String,
        @Body body: D1SqlQueryRequest
    ): Response<com.google.gson.JsonObject>
}

object D1ApiClient {
    private const val ERP_BASE_URL = "https://www.e-manager.az/"
    private const val CF_BASE_URL = "https://api.cloudflare.com/"

    // Cloudflare D1 Direct Credentials (from ERP backend configuration)
    const val CF_ACCOUNT_ID = "998c5b83aa3cdafa7f4325d5ee1791f2"
    const val CF_D1_DATABASE_ID = "682601ec-f260-421b-bbe3-d9824fd82b95"
    const val CF_API_TOKEN = "cfat_fRTPrXdOjVACm5ibMRhczAs5tNjKlgxmgomAyuyO58760cf8"
    val CF_AUTH_HEADER = "Bearer $CF_API_TOKEN"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val erpRestService: D1RestApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ERP_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(D1RestApiService::class.java)
    }

    val cfDirectService: CloudflareD1DirectService by lazy {
        Retrofit.Builder()
            .baseUrl(CF_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudflareD1DirectService::class.java)
    }
}
