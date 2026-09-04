package com.amanansari.iykyk.ui.screen

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.amanansari.iykyk.ui.viewmodel.ProcessingViewModel


@Composable
fun ProcessingScreen(
    uri: Uri,
    viewModel: ProcessingViewModel = hiltViewModel()
){

    LaunchedEffect(uri) {
        viewModel.updateUri(uri)
        viewModel.startProcessing()
    }

    ProcessingScreenContent()
}

@Composable
fun ProcessingScreenContent(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Processing Video"
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Analyzing faces..."
        )
    }
}