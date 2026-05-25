package dev.mangolabs.quilleditor.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      QuillSampleTheme {
        SampleApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleApp() {
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
      )
    }
  ) { innerPadding ->
    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      SampleEditorScreen(modifier = Modifier.fillMaxSize())
    }
  }
}
