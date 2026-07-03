package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ExtractionRecord
import com.example.data.models.WorkspaceResponse
import com.example.ui.viewmodel.PrescriptionViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetailScreen(
    workspace: WorkspaceResponse,
    viewModel: PrescriptionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localHistory by viewModel.localHistory.collectAsState()

    // Filter local Room history records by this workspace ID
    val workspaceHistory = remember(localHistory) {
        localHistory.filter { it.workspaceId == workspace.id }
    }

    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var expandedRecordId by remember { mutableStateOf<Int?>(null) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isUploading = true
                uploadError = null
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    
                    var fileName = "workspace_scan.jpg"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = it.getString(nameIndex)
                            }
                        }
                    }

                    if (bytes != null) {
                        viewModel.uploadWorkspaceImage(
                            workspaceId = workspace.id,
                            workspaceName = workspace.name,
                            fileBytes = bytes,
                            fileName = fileName,
                            onComplete = { success, error ->
                                isUploading = false
                                if (!success) {
                                    uploadError = error ?: "Upload failed"
                                }
                            }
                        )
                    } else {
                        isUploading = false
                        uploadError = "Could not read image bytes"
                    }
                } catch (e: Exception) {
                    isUploading = false
                    uploadError = e.localizedMessage ?: "Failed to read image"
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = workspace.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val isTokenSaved by viewModel.isTokenSaved.collectAsState()
                            Surface(
                                color = if (isTokenSaved) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Token saved: ${if (isTokenSaved) "YES" else "NO"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                    color = if (isTokenSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Workspace ID: #${workspace.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Export CSV Button
                    IconButton(
                        onClick = {
                            viewModel.downloadWorkspaceCsv(
                                workspaceId = workspace.id,
                                workspaceName = workspace.name,
                                context = context,
                                onComplete = { _, _ -> }
                            )
                        },
                        modifier = Modifier.testTag("download_csv_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Workspace CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.testTag("append_image_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add Image")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Stats Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Row Count (Extracted)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${workspace.row_count} entries",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.downloadWorkspaceCsv(
                                    workspaceId = workspace.id,
                                    workspaceName = workspace.name,
                                    context = context,
                                    onComplete = { _, _ -> }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("download_csv_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Quick Demo Prescription Append Section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "🧪 Append Demo Prescription to Workspace",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val bytes = com.example.utils.SamplePrescriptionGenerator.generateSamplePrescriptionBytes(1)
                                viewModel.uploadWorkspaceImage(
                                    workspaceId = workspace.id,
                                    workspaceName = workspace.name,
                                    fileBytes = bytes,
                                    fileName = "demo_prescription_1.jpg",
                                    onComplete = { _, _ -> }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo 1", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = {
                                val bytes = com.example.utils.SamplePrescriptionGenerator.generateSamplePrescriptionBytes(2)
                                viewModel.uploadWorkspaceImage(
                                    workspaceId = workspace.id,
                                    workspaceName = workspace.name,
                                    fileBytes = bytes,
                                    fileName = "demo_prescription_2.jpg",
                                    onComplete = { _, _ -> }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo 2", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Upload Status banner
            if (uploadError != null) {
                item {
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
                            Icon(Icons.Default.Error, tint = MaterialTheme.colorScheme.onErrorContainer, contentDescription = null)
                            Text(
                                text = uploadError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (isUploading) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Uploading prescription image and appending to workspace CSV...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Historical List Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned In Workspace (${workspaceHistory.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (workspaceHistory.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Scanned Prescriptions Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the float icon below to scan and append an image.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(workspaceHistory) { record ->
                    HistoricalRecordCard(
                        record = record,
                        isExpanded = expandedRecordId == record.id,
                        onToggleExpand = {
                            expandedRecordId = if (expandedRecordId == record.id) null else record.id
                        },
                        onDelete = { viewModel.deleteHistoryRecord(record.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalRecordCard(
    record: ExtractionRecord,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(record.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    // Try parsing JSON string from Room DB
    val parsedMap = remember(record.extractedJson) {
        try {
            val moshi = Moshi.Builder().build()
            val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any?>>(type)
            adapter.fromJson(record.extractedJson) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap<String, Any?>()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        onClick = onToggleExpand
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Task,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = record.fileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete from history",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Parsed Prescription Schema:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    ExtractedDataViewer(data = parsedMap)
                }
            }
        }
    }
}
