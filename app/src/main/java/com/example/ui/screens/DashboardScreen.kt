package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.PrescriptionViewModel
import com.example.ui.viewmodel.WorkspacesState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PrescriptionViewModel,
    onSelectWorkspace: (WorkspaceResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val workspacesState by viewModel.workspacesState.collectAsState()
    val localHistory by viewModel.localHistory.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var historyExpandedId by remember { mutableStateOf<Int?>(null) }

    val userDisplayName = (authState as? AuthState.Success)?.name ?: "User"
    val userEmail = (authState as? AuthState.Success)?.email ?: ""

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Customized Brand & Header Profile Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userDisplayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = "Hello, $userDisplayName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userEmail.isNotEmpty()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val isTokenSaved by viewModel.isTokenSaved.collectAsState()
                            Surface(
                                color = if (isTokenSaved) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Token saved: ${if (isTokenSaved) "YES" else "NO"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = if (isTokenSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refresh workspaces list
                        IconButton(onClick = { viewModel.loadWorkspaces() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Logout
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log Out",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Workspaces", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Direct Scan", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("History", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddWorkspaceDialog = true },
                    modifier = Modifier.testTag("add_workspace_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Add Workspace")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Workspaces List
                    when (val state = workspacesState) {
                        is WorkspacesState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is WorkspacesState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Workspace Error",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadWorkspaces() }) {
                                    Text("Retry Connection")
                                }
                            }
                        }
                        is WorkspacesState.Success -> {
                            if (state.list.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Workspaces Found",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Create a workspace folder to organize, append images, and build structural dataset sheets.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(onClick = { showAddWorkspaceDialog = true }) {
                                        Text("Create Workspace Folder")
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                                ) {
                                    items(state.list) { ws ->
                                        WorkspaceItemCard(
                                            workspace = ws,
                                            onClick = { onSelectWorkspace(ws) },
                                            onDownload = {
                                                viewModel.downloadWorkspaceCsv(
                                                    workspaceId = ws.id,
                                                    workspaceName = ws.name,
                                                    context = context,
                                                    onComplete = { _, _ -> }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Direct Scan / Extract Screen
                    ExtractScreen(viewModel = viewModel)
                }
                2 -> {
                    // Extraction History
                    if (localHistory.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Scan History Empty",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Successfully processed prescription results are cached locally for offline reference.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Offline Cached Records (${localHistory.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear All")
                                    }
                                }
                            }
                            
                            items(localHistory) { record ->
                                HistoricalRecordCard(
                                    record = record,
                                    isExpanded = historyExpandedId == record.id,
                                    onToggleExpand = {
                                        historyExpandedId = if (historyExpandedId == record.id) null else record.id
                                    },
                                    onDelete = { viewModel.deleteHistoryRecord(record.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Create Workspace Dialog
        if (showAddWorkspaceDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddWorkspaceDialog = false
                    newWorkspaceName = ""
                },
                title = {
                    Text(
                        text = "Create Workspace Folder",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Name your folder based on the clinic, doctor, patient list, or categories to organize CSV extractions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newWorkspaceName,
                            onValueChange = { newWorkspaceName = it },
                            label = { Text("Workspace Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("workspace_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newWorkspaceName.isNotBlank()) {
                                viewModel.createWorkspace(newWorkspaceName) {
                                    showAddWorkspaceDialog = false
                                    newWorkspaceName = ""
                                }
                            }
                        },
                        modifier = Modifier.testTag("workspace_confirm_button"),
                        enabled = newWorkspaceName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddWorkspaceDialog = false
                        newWorkspaceName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceItemCard(
    workspace: WorkspaceResponse,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workspace_card_${workspace.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workspace.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${workspace.row_count} records processed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = onDownload,
                modifier = Modifier.testTag("download_csv_item_button_${workspace.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Workspace CSV",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
