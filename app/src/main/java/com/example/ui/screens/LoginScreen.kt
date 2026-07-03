package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.PrescriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: PrescriptionViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    if (authState is AuthState.Loading && email.isEmpty() && password.isEmpty() && !isSignUp) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(gradientBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Verifying session...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Identity Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "RxExtract App Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Text(
                    text = "RxExtract",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Prescription OCR & Workspace Manager",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Form Fields with AnimateContentSize for nice height shifts
                    Column(
                        modifier = Modifier.animateContentSize(animationSpec = spring()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSignUp) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fullname_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )
                    }

                    // Error messages
                    if (authState is AuthState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    contentDescription = "Error icon"
                                )
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Token saved debug text on screen
                    val isTokenSaved by viewModel.isTokenSaved.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isTokenSaved) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Token saved: ${if (isTokenSaved) "YES" else "NO"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isTokenSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isSignUp) {
                                viewModel.register(fullName, email, password)
                            } else {
                                viewModel.login(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = authState !is AuthState.Loading && email.isNotBlank() && password.isNotBlank() && (!isSignUp || fullName.isNotBlank())
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Register" else "Login",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Toggle mode link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        TextButton(
                            onClick = {
                                isSignUp = !isSignUp
                                // Clear inputs on switch to make it polished
                                password = ""
                                if (authState is AuthState.Error) {
                                    viewModel.logout() // resets state to Idle
                                }
                            },
                            modifier = Modifier.testTag("toggle_auth_mode_button")
                        ) {
                            Text(
                                text = if (isSignUp) "Log In" else "Sign Up",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
