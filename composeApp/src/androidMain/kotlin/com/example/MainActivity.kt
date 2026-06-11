package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StudygramViewModel
import com.example.ui.screens.StudygramAppContent
import com.example.ui.theme.MyApplicationTheme

import com.example.data.DatabaseProvider
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    currentActivity = this
    DatabaseProvider.init(applicationContext)
    com.example.data.PermissionHandler.register(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: StudygramViewModel = viewModel()
        StudygramAppContent(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (currentActivity == this) {
      currentActivity = null
    }
  }

  companion object {
    var currentActivity: ComponentActivity? = null
  }
}

