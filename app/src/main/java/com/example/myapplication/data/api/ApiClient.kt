package com.example.myapplication.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {
    var authToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")

        // Only attach tenant-id if we are not hitting the login endpoint
        // (sometimes the backend rejects tenant-id before auth)
        if (!originalRequest.url.encodedPath.contains("api/auth/login")) {
            requestBuilder.header("x-tenant-id", "IZrfGLfOkcwzP3RSCHzt")
        }

        authToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}

interface ErpApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): retrofit2.Response<LoginResponse>

    @GET("api/products")
    suspend fun getProducts(
        @Query("barcode") barcode: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): retrofit2.Response<ProductResponse>

    @POST("api/pos/sale")
    suspend fun submitSale(@Body saleRequest: SaleRequest): retrofit2.Response<SaleResponse>
}

object ApiClient {
    private const val BASE_URL = "https://www.e-manager.az/"

    val authInterceptor = AuthInterceptor()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val erpApiService: ErpApiService by lazy {
        retrofit.create(ErpApiService::class.java)
    }

    fun setAuthToken(token: String?) {
        authInterceptor.authToken = token
    }
}
