package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferenceManager
import com.example.data.api.PrescriptionApi
import com.example.data.db.AppDatabase
import com.example.data.db.ExtractionRecord
import com.example.data.models.LoginRequest
import com.example.data.models.RegisterRequest
import com.example.data.models.WorkspaceCreateRequest
import com.example.data.models.WorkspaceResponse
import com.example.data.repository.ExtractionRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val name: String?, val email: String?) : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface WorkspacesState {
    object Loading : WorkspacesState
    data class Success(val list: List<WorkspaceResponse>) : WorkspacesState
    data class Error(val message: String) : WorkspacesState
}

sealed interface ExtractState {
    object Idle : ExtractState
    object Loading : ExtractState
    data class Success(val data: Map<String, Any?>) : ExtractState
    data class Error(val message: String) : ExtractState
}

class PrescriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val prefManager = PreferenceManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = ExtractionRepository(database.extractionDao())

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

    // Api instance which updates reactively based on token provider
    private val api: PrescriptionApi = PrescriptionApi.create(
        tokenProvider = { prefManager.getToken() },
        onUnauthorized = { logout() }
    )

    // Auth States
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isTokenSaved = MutableStateFlow(prefManager.getToken() != null)
    val isTokenSaved: StateFlow<Boolean> = _isTokenSaved.asStateFlow()

    // Workspace States
    private val _workspacesState = MutableStateFlow<WorkspacesState>(WorkspacesState.Loading)
    val workspacesState: StateFlow<WorkspacesState> = _workspacesState.asStateFlow()

    // Extraction States
    private val _extractionState = MutableStateFlow<ExtractState>(ExtractState.Idle)
    val extractionState: StateFlow<ExtractState> = _extractionState.asStateFlow()

    // Local Records from Room
    val localHistory: StateFlow<List<ExtractionRecord>> = repository.allExtractions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        val savedToken = prefManager.getToken()
        if (!savedToken.isNullOrEmpty()) {
            _isTokenSaved.value = true
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    val response = api.getMe()
                    val userMap = (response["user"] as? Map<String, Any?>) ?: response
                    val name = userMap["full_name"] as? String ?: prefManager.getUserName() ?: "User"
                    val userEmail = userMap["email"] as? String ?: prefManager.getUserEmail() ?: ""
                    
                    prefManager.saveAuth(savedToken, name, userEmail)
                    _isTokenSaved.value = true
                    _authState.value = AuthState.Success(name, userEmail)
                    loadWorkspaces()
                } catch (e: Exception) {
                    android.util.Log.e("PrescriptionViewModel", "Session verification failed", e)
                    if (e is retrofit2.HttpException && e.code() == 401) {
                        logout()
                    } else if (prefManager.getToken().isNullOrEmpty()) {
                        logout()
                    } else {
                        val name = prefManager.getUserName() ?: "User"
                        val userEmail = prefManager.getUserEmail() ?: ""
                        _authState.value = AuthState.Success(name, userEmail)
                        loadWorkspaces()
                    }
                }
            }
        } else {
            _isTokenSaved.value = false
            _authState.value = AuthState.Idle
        }
    }

    // Login
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(LoginRequest(email, password))
                val name = response.user["full_name"] as? String ?: "User"
                val userEmail = response.user["email"] as? String ?: email
                prefManager.saveAuth(response.token, name, userEmail)
                _isTokenSaved.value = true
                _authState.value = AuthState.Success(name, userEmail)
                loadWorkspaces()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseAuthError(e, isLogin = true))
            }
        }
    }

    // Register
    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.register(RegisterRequest(fullName, email, password))
                val name = response.user["full_name"] as? String ?: fullName
                val userEmail = response.user["email"] as? String ?: email
                prefManager.saveAuth(response.token, name, userEmail)
                _isTokenSaved.value = true
                _authState.value = AuthState.Success(name, userEmail)
                loadWorkspaces()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseAuthError(e, isLogin = false))
            }
        }
    }

    // Logout
    fun logout() {
        prefManager.clearAuth()
        _isTokenSaved.value = false
        _authState.value = AuthState.Idle
        _workspacesState.value = WorkspacesState.Loading
        _extractionState.value = ExtractState.Idle
    }

    // Load Workspaces
    fun loadWorkspaces() {
        if (prefManager.getToken().isNullOrEmpty()) {
            logout()
            _workspacesState.value = WorkspacesState.Error("Token missing. Please log in.")
            return
        }
        viewModelScope.launch {
            _workspacesState.value = WorkspacesState.Loading
            try {
                val response = api.getWorkspaces()
                val parsed = parseWorkspaces(response)
                _workspacesState.value = WorkspacesState.Success(parsed)
            } catch (e: Exception) {
                _workspacesState.value = WorkspacesState.Error(parseNetworkError(e))
            }
        }
    }

    // Create Workspace
    fun createWorkspace(name: String, onSuccess: () -> Unit) {
        if (prefManager.getToken().isNullOrEmpty()) {
            logout()
            return
        }
        viewModelScope.launch {
            try {
                api.createWorkspace(WorkspaceCreateRequest(name))
                loadWorkspaces()
                onSuccess()
            } catch (e: Exception) {
                _workspacesState.value = WorkspacesState.Error(parseNetworkError(e))
            }
        }
    }

    // Direct Extraction
    fun extractPrescription(fileBytes: ByteArray, fileName: String) {
        if (prefManager.getToken().isNullOrEmpty()) {
            logout()
            _extractionState.value = ExtractState.Error("Token missing. Please log in.")
            return
        }
        viewModelScope.launch {
            _extractionState.value = ExtractState.Loading
            try {
                val compressedBytes = compressAndResizeImage(fileBytes)
                val requestFile = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, compressedBytes.size)
                val uploadFileName = toJpegFileName(fileName, "prescription_scan.jpg")
                val body = MultipartBody.Part.createFormData("file", uploadFileName, requestFile)
                val response = api.extractImage(body)
                _extractionState.value = ExtractState.Success(response)

                // Save to local Room history safely
                try {
                    val jsonString = mapAdapter.toJson(response)
                    repository.insert(
                        ExtractionRecord(
                            workspaceId = null,
                            workspaceName = "Direct Scan",
                            fileName = uploadFileName,
                            extractedJson = jsonString
                        )
                    )
                } catch (dbEx: Exception) {
                    android.util.Log.e("PrescriptionViewModel", "Failed to save record to local Room database, ignoring", dbEx)
                }
            } catch (e: Exception) {
                _extractionState.value = ExtractState.Error(parseNetworkError(e))
            }
        }
    }

    // Workspace-bound Extraction / Upload Image
    fun uploadWorkspaceImage(workspaceId: Int, workspaceName: String, fileBytes: ByteArray, fileName: String, onComplete: (Boolean, String?) -> Unit) {
        if (prefManager.getToken().isNullOrEmpty()) {
            logout()
            onComplete(false, "Token missing. Please log in.")
            return
        }
        viewModelScope.launch {
            try {
                val compressedBytes = compressAndResizeImage(fileBytes)
                val requestFile = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, compressedBytes.size)
                val uploadFileName = toJpegFileName(fileName, "workspace_scan.jpg")
                val body = MultipartBody.Part.createFormData("file", uploadFileName, requestFile)
                val response = api.uploadWorkspaceImage(workspaceId, body)
                
                // Save to local Room history safely
                try {
                    val jsonString = mapAdapter.toJson(response)
                    repository.insert(
                        ExtractionRecord(
                            workspaceId = workspaceId,
                            workspaceName = workspaceName,
                            fileName = uploadFileName,
                            extractedJson = jsonString
                        )
                    )
                } catch (dbEx: Exception) {
                    android.util.Log.e("PrescriptionViewModel", "Failed to save record to local Room database, ignoring", dbEx)
                }
                
                // Refresh workspaces list safely
                try {
                    loadWorkspaces()
                } catch (wsEx: Exception) {
                    android.util.Log.e("PrescriptionViewModel", "Failed to refresh workspaces list, ignoring", wsEx)
                }
                
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, parseNetworkError(e))
            }
        }
    }

    private fun parseNetworkError(e: Throwable): String {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            val errorBodyString = try {
                e.response()?.errorBody()?.string()
            } catch (ex: Exception) {
                null
            }
            val body = if (!errorBodyString.isNullOrEmpty()) {
                errorBodyString
            } else {
                e.message() ?: ""
            }
            val detail = extractDetailFromErrorBody(body)
            return when (code) {
                400 -> detail ?: "Please check your input."
                401 -> {
                    logout()
                    "Session expired. Please login again."
                }
                409 -> "Account already exists. Please login instead."
                500 -> "Server configuration issue. Please try again later."
                502 -> "AI extraction service is busy. Please try again."
                else -> detail ?: "Request failed. Status $code."
            }
        }
        return e.localizedMessage ?: "An unknown network error occurred"
    }

    private fun parseAuthError(e: Throwable, isLogin: Boolean): String {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            val errorBodyString = try {
                e.response()?.errorBody()?.string()
            } catch (ex: Exception) {
                null
            }
            val detail = extractDetailFromErrorBody(errorBodyString)
            return when (code) {
                400 -> detail ?: "Please check your input."
                401 -> if (isLogin) "Invalid email or password." else "Session expired. Please login again."
                409 -> "Account already exists. Please login instead."
                500 -> "Server configuration issue. Please try again later."
                502 -> "AI extraction service is busy. Please try again."
                else -> detail ?: "Request failed. Status $code."
            }
        }
        return e.localizedMessage ?: if (isLogin) "Login failed" else "Registration failed"
    }

    private fun extractDetailFromErrorBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return match?.groupValues?.getOrNull(1)
    }

    private fun toJpegFileName(fileName: String, fallback: String): String {
        val cleaned = fileName.trim().ifBlank { fallback }
        return if (cleaned.endsWith(".jpg", true) || cleaned.endsWith(".jpeg", true)) {
            cleaned
        } else {
            "$cleaned.jpg"
        }
    }

    private fun compressAndResizeImage(imageBytes: ByteArray): ByteArray {
        try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                return imageBytes
            }
            
            val targetWidth = 1600
            val scale = if (originalWidth > targetWidth) {
                targetWidth.toFloat() / originalWidth.toFloat()
            } else {
                1.0f
            }
            
            if (scale >= 1.0f) {
                // If width <= 1600, compress to JPEG at 85% quality to satisfy constraints and save bandwidth
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                val compressedBytes = outputStream.toByteArray()
                bitmap.recycle()
                return compressedBytes
            }
            
            // Calculate appropriate sample size (power of 2) to load a downscaled bitmap safely
            var sampleSize = 1
            while ((originalWidth / sampleSize) / 2 >= targetWidth) {
                sampleSize *= 2
            }
            
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions) ?: return imageBytes
            
            val finalBitmap = if (bitmap.width > targetWidth) {
                val finalScale = targetWidth.toFloat() / bitmap.width.toFloat()
                val matrix = android.graphics.Matrix().apply {
                    postScale(finalScale, finalScale)
                }
                android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).apply {
                    if (this != bitmap) {
                        bitmap.recycle()
                    }
                }
            } else {
                bitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val compressedBytes = outputStream.toByteArray()
            finalBitmap.recycle()
            return compressedBytes
        } catch (e: Exception) {
            android.util.Log.e("PrescriptionViewModel", "Failed to compress and resize image", e)
            return imageBytes
        }
    }

    // Download CSV and share
    fun downloadWorkspaceCsv(workspaceId: Int, workspaceName: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        if (prefManager.getToken().isNullOrEmpty()) {
            logout()
            onComplete(false, "Token missing. Please log in.")
            return
        }
        viewModelScope.launch {
            try {
                val responseBody = api.downloadWorkspaceCsv(workspaceId)
                val bytes = responseBody.bytes()
                
                // Save file in private files dir to allow secure FileProvider sharing
                val directory = File(context.cacheDir, "csv_downloads")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val cleanName = workspaceName.replace("\\s+".toRegex(), "_")
                val file = File(directory, "${cleanName}_workspace_${workspaceId}.csv")
                FileOutputStream(file).use { out ->
                    out.write(bytes)
                }

                // Share file using Share Intent
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "CSV Export - $workspaceName")
                    putExtra(Intent.EXTRA_TEXT, "Here is the CSV export for prescription workspace: $workspaceName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(shareIntent, "Share Workspace CSV").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                onComplete(true, "CSV downloaded successfully! Shared file.")
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Failed to download CSV")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    // Parse the API JSON representation to look for worksheets / workspaces
    private fun parseWorkspaces(map: Map<String, Any?>?): List<WorkspaceResponse> {
        if (map == null) return emptyList()
        // Try different standard keys returned by FastAPI
        val list = map["workspaces"] as? List<*>
            ?: map["items"] as? List<*>
            ?: map["data"] as? List<*>
            ?: map.values.find { it is List<*> } as? List<*>
            ?: emptyList<Any>()

        return list.mapNotNull { item ->
            try {
                if (item is Map<*, *>) {
                    val id = (item["id"] as? Number)?.toInt() ?: 0
                    val name = item["name"] as? String ?: ""
                    val csvFilename = item["csv_filename"] as? String
                    val rowCount = (item["row_count"] as? Number)?.toInt() ?: 0
                    val createdAt = item["created_at"] as? String ?: ""
                    WorkspaceResponse(id, name, csvFilename, rowCount, createdAt)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
