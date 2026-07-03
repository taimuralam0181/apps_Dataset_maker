package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.WorkspaceDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.PrescriptionViewModel
import com.example.data.models.WorkspaceResponse

class MainActivity : ComponentActivity() {
  private val viewModel: PrescriptionViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val authState by viewModel.authState.collectAsState()
        var currentWorkspaceDetail by remember { mutableStateOf<WorkspaceResponse?>(null) }

        Crossfade(
          targetState = authState,
          modifier = Modifier.fillMaxSize(),
          label = "AuthCrossfade"
        ) { state ->
          when (state) {
            is AuthState.Success -> {
              val workspace = currentWorkspaceDetail
              if (workspace != null) {
                WorkspaceDetailScreen(
                  workspace = workspace,
                  viewModel = viewModel,
                  onBack = { currentWorkspaceDetail = null }
                )
              } else {
                DashboardScreen(
                  viewModel = viewModel,
                  onSelectWorkspace = { currentWorkspaceDetail = it }
                )
              }
            }
            else -> {
              LoginScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
