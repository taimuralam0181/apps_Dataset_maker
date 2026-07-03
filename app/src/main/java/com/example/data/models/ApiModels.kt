package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val full_name: String,
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class User(
    val id: Int? = null,
    val full_name: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class WorkspaceCreateRequest(
    val name: String
)

@JsonClass(generateAdapter = true)
data class WorkspaceResponse(
    val id: Int,
    val name: String,
    val csv_filename: String?,
    val row_count: Int,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    val message: String? = null,
    val status: String? = null
)
