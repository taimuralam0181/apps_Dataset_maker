package com.example.data.api

import com.example.BuildConfig
import com.example.data.models.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface PrescriptionApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("api/me")
    suspend fun getMe(): Map<String, Any?>

    @Multipart
    @POST("api/extract")
    suspend fun extractImage(
        @Part file: MultipartBody.Part
    ): Map<String, Any?>

    @GET("api/workspaces")
    suspend fun getWorkspaces(): Map<String, Any?>

    @POST("api/workspaces")
    suspend fun createWorkspace(@Body request: WorkspaceCreateRequest): WorkspaceResponse

    @Multipart
    @POST("api/workspaces/{workspace_id}/images")
    suspend fun uploadWorkspaceImage(
        @Path("workspace_id") workspaceId: Int,
        @Part file: MultipartBody.Part
    ): Map<String, Any?>

    @GET("api/workspaces/{workspace_id}/download")
    @Streaming
    suspend fun downloadWorkspaceCsv(
        @Path("workspace_id") workspaceId: Int
    ): ResponseBody

    companion object {
        private const val BASE_URL = "https://prescription-api-a4d1.onrender.com/"

        fun create(
            tokenProvider: () -> String?,
            onUnauthorized: () -> Unit = {}
        ): PrescriptionApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                redactHeader("Authorization")
            }

            val authInterceptor = okhttp3.Interceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                val requestBuilder = request.newBuilder()
                
                val isAuthRoute = path.contains("/api/auth/login") || path.contains("/api/auth/register")
                val token = tokenProvider()
                
                if (!isAuthRoute && !token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                
                if (path.contains("/api/extract")) {
                    android.util.Log.d("PrescriptionApi", "Calling /api/extract. Token exists: ${!token.isNullOrEmpty()}")
                }
                
                val finalRequest = requestBuilder.build()
                val response = chain.proceed(finalRequest)
                
                android.util.Log.d("PrescriptionApi", "Request URL: ${finalRequest.url}, Response Code: ${response.code}")
                
                if (response.code == 401 && !isAuthRoute) {
                    onUnauthorized()
                }
                response
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(PrescriptionApi::class.java)
        }
    }
}
