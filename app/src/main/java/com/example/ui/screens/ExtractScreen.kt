package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.ExtractState
import com.example.ui.viewmodel.PrescriptionViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(
    viewModel: PrescriptionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extractionState by viewModel.extractionState.collectAsState()

    // Real OCR Image State
    var userImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var userImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var userImageName by remember { mutableStateOf("") }

    // Demo Selection State
    var selectedDemoId by remember { mutableStateOf<Int?>(null) }
    val demoImageBytes = remember(selectedDemoId) {
        selectedDemoId?.let { id ->
            com.example.utils.SamplePrescriptionGenerator.generateSamplePrescriptionBytes(id)
        }
    }
    val demoImageName = remember(selectedDemoId) {
        selectedDemoId?.let { id -> "sample_prescription_$id.jpg" } ?: ""
    }

    // Tracker for whether the last ran request was a demo template
    var isLastRunDemo by remember { mutableStateOf(false) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                userImageUri = uri
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    userImageBytes = bytes
                    
                    // Extract file name
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                userImageName = it.getString(nameIndex)
                            }
                        }
                    }
                    if (userImageName.isEmpty()) {
                        userImageName = "prescription_scan.jpg"
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to read selected image: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Description Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Instant OCR Extractor",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Upload real prescription images or try with pre-configured templates to parse structured data instantly (doctor, medicines, dosages, and instructions).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }

        item {
            // SECTION A: REAL OCR EXTRACTION
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Section A: Real OCR Extraction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Call live API with your uploaded image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Image Picker Box/Surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("image_picker_target"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (userImageBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    onClick = {
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    if (userImageBytes != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = userImageBytes,
                                contentDescription = "Selected Prescription Scan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            
                            // Top End clear button
                            IconButton(
                                onClick = {
                                    userImageUri = null
                                    userImageBytes = null
                                    userImageName = ""
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Image", tint = Color.White)
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Tap to Change", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose Prescription Image",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "JPEG, PNG, WEBP up to 10MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Real Extraction trigger button
                Button(
                    onClick = {
                        val bytes = userImageBytes
                        if (bytes != null) {
                            isLastRunDemo = false
                            viewModel.extractPrescription(bytes, userImageName)
                        } else {
                            android.widget.Toast.makeText(context, "Please choose an image first.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("extract_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = extractionState !is ExtractState.Loading
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extract From Uploaded Image",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        }

        item {
            // SECTION B: DEMO PRESCRIPTIONS
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Section B: Demo Prescriptions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Simulate prescription image files to test API parsing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Demo cards selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(1, 2).forEach { id ->
                        val isSelected = selectedDemoId == id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            onClick = {
                                selectedDemoId = id
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Prescription $id",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (id == 1) "Cilnidip, Olmesart..." else "Dabigat, Bisop, Valgac...",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // If a demo card is selected, show a miniature preview of the template
                selectedDemoId?.let { id ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Mini thumbnail of prescription pad
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                val demoBytes = demoImageBytes
                                if (demoBytes != null) {
                                    AsyncImage(
                                        model = demoBytes,
                                        contentDescription = "Demo prescription $id",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Template $id Image Generated",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ready to upload \"sample_prescription_$id.jpg\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Run Demo button
                Button(
                    onClick = {
                        val bytes = demoImageBytes
                        if (bytes != null) {
                            isLastRunDemo = true
                            viewModel.extractPrescription(bytes, demoImageName)
                        } else {
                            android.widget.Toast.makeText(context, "Please select a demo prescription card first.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    enabled = selectedDemoId != null && extractionState !is ExtractState.Loading
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run Demo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        }

        item {
            // Output Display Box
        AnimatedVisibility(
            visible = extractionState !is ExtractState.Idle,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Demo Result Banner
                    if (isLastRunDemo && extractionState is ExtractState.Success) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Demo Result (Pre-configured Template)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLastRunDemo) "Demo Extraction Results" else "Real Extraction Results",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLastRunDemo) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                            
                            if (extractionState is ExtractState.Success) {
                                Badge(
                                    containerColor = if (isLastRunDemo) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isLastRunDemo) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = if (isLastRunDemo) "Demo Success" else "Success",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        when (val state = extractionState) {
                            is ExtractState.Loading -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = if (isLastRunDemo) "Uploading and parsing demo template..." else "Uploading and analyzing real document...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is ExtractState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Ocr Extraction Failed",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                            is ExtractState.Success -> {
                                ExtractedDataViewer(data = state.data)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
        }

        item {
            Spacer(modifier = Modifier.height(180.dp))
        }
    }
}

@Composable
fun ExtractedDataViewer(
    data: Map<String, Any?>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No fields extracted from this document", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val context = LocalContext.current
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CSV Export Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val csvContent = generateCsvContent(data)
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        val filename = "prescription_extraction_$timestamp.csv"
                        val success = saveCsvToDownloads(context, filename, csvContent)
                        if (success) {
                            android.widget.Toast.makeText(context, "CSV downloaded to Downloads folder!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to download CSV", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download CSV", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        val csvContent = generateCsvContent(data)
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Extracted Prescription Data")
                            putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share CSV"))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share CSV", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            // 1. Medicine Count (if present)
            val medicineCount = data["medicine_count"] ?: data["count"]
            if (medicineCount != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Medicine Count",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$medicineCount medicines detected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Department Card (if present)
            val department = data["department"] ?: data["dept"] ?: data["hospital_department"] ?: data["specialty"]
            if (department != null && department.toString().isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Department / Specialty",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = department.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Medicines List (if present)
            val medicines = data["medicines"] as? List<*> ?: data["items"] as? List<*>
            if (medicines != null && medicines.isNotEmpty()) {
                Text(
                    text = "Medicines List",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                medicines.forEachIndexed { index, med ->
                    val medMap = med as? Map<*, *>
                    if (medMap != null) {
                        val name = medMap["name"] ?: medMap["medicine_name"] ?: medMap["medicine"] ?: "Unknown Medicine"
                        val dosage = medMap["dosage"] ?: medMap["strength"] ?: "N/A"
                        val frequency = medMap["frequency"] ?: medMap["instructions"] ?: medMap["timing"] ?: "N/A"
                        val duration = medMap["duration"] ?: "N/A"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = name.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text("#${index + 1}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DOSAGE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = dosage.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "FREQUENCY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = frequency.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DURATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = duration.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Plain string item
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = med.toString(),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. Extracted Raw Text (if present)
            val extractedText = data["extracted_text"] as? String ?: data["text"] as? String
            if (extractedText != null && extractedText.isNotBlank()) {
                var isTextExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Extracted Text",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { isTextExpanded = !isTextExpanded }) {
                                Text(if (isTextExpanded) "Collapse" else "Expand")
                            }
                        }

                        AnimatedVisibility(visible = isTextExpanded) {
                            Column {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = extractedText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. Other key-value pairs (excluding structured keys we just handled, filtered to hide models/gemini)
            val otherData = data.filterKeys { key ->
                val kLower = key.lowercase()
                kLower !in setOf("medicine_count", "count", "medicines", "items", "extracted_text", "text", "model", "model_name", "backend_model", "engine", "department", "dept", "hospital_department", "specialty") &&
                !kLower.contains("model")
            }.filterValues { value ->
                val vStr = value?.toString()?.lowercase() ?: ""
                !vStr.contains("gemini") && !vStr.contains("flash")
            }

            if (otherData.isNotEmpty()) {
                var isAdditionalExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Additional Information",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { isAdditionalExpanded = !isAdditionalExpanded }) {
                                Icon(
                                    imageVector = if (isAdditionalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isAdditionalExpanded) "Collapse" else "Expand"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isAdditionalExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))
                                otherData.forEach { (key, value) ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = key.replace("_", " ").uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatValue(value),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Format nested maps/lists beautifully
fun formatValue(value: Any?): String {
    return when (value) {
        null -> "—"
        is Map<*, *> -> {
            value.entries.joinToString("\n") { (k, v) ->
                "• ${k?.toString()?.replace("_", " ")?.capitalize()}: ${formatValue(v)}"
            }
        }
        is List<*> -> {
            value.joinToString("\n") { item ->
                if (item is Map<*, *>) {
                    formatValue(item)
                } else {
                    "• ${item.toString()}"
                }
            }
        }
        else -> value.toString()
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun generateCsvContent(data: Map<String, Any?>): String {
    val builder = java.lang.StringBuilder()
    
    // Header
    builder.append("DATA FIELD,EXTRACTED VALUE\n")
    
    val ignoredKeys = setOf("medicine_count", "count", "medicines", "items", "extracted_text", "text", "model")
    val otherData = data.filterKeys { it !in ignoredKeys }
    
    // Add count
    val medicineCount = data["medicine_count"] ?: data["count"] ?: "N/A"
    builder.append("Medicine Count,${escapeCsvField(medicineCount.toString())}\n")
    
    otherData.forEach { (key, value) ->
        val cleanKey = key.replace("_", " ").uppercase()
        val cleanVal = formatValue(value).replace("\n", " ; ")
        builder.append("${escapeCsvField(cleanKey)},${escapeCsvField(cleanVal)}\n")
    }
    
    builder.append("\n") // empty spacer row
    
    // Medicines list
    builder.append("MEDICINE NAME,DOSAGE / STRENGTH,FREQUENCY / TIMING,DURATION\n")
    
    val medicines = data["medicines"] as? List<*> ?: data["items"] as? List<*>
    if (medicines != null && medicines.isNotEmpty()) {
        medicines.forEach { med ->
            val medMap = med as? Map<*, *>
            if (medMap != null) {
                val name = medMap["name"] ?: medMap["medicine_name"] ?: medMap["medicine"] ?: "Unknown Medicine"
                val dosage = medMap["dosage"] ?: medMap["strength"] ?: "N/A"
                val frequency = medMap["frequency"] ?: medMap["instructions"] ?: medMap["timing"] ?: "N/A"
                val duration = medMap["duration"] ?: "N/A"
                
                builder.append("${escapeCsvField(name.toString())},")
                builder.append("${escapeCsvField(dosage.toString())},")
                builder.append("${escapeCsvField(frequency.toString())},")
                builder.append("${escapeCsvField(duration.toString())}\n")
            } else {
                builder.append("${escapeCsvField(med.toString())},N/A,N/A,N/A\n")
            }
        }
    } else {
        builder.append("No structured medicines found,N/A,N/A,N/A\n")
    }
    
    return builder.toString()
}

private fun escapeCsvField(value: String): String {
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    return value
}

private fun saveCsvToDownloads(context: android.content.Context, filename: String, csvContent: String): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
                true
            } else {
                false
            }
        } else {
            val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(targetDir, filename)
            file.writeText(csvContent)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
