package com.kidsrec.chatbot.ui.curator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CuratorViewModel
) {
    var rawText by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Library Curator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.little_dino),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Add Books with AI Magic!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Paste book lists or text from any website below. Little Dino will automatically extract the details and add them to your app!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                label = { Text("Paste book text here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                placeholder = { Text("Paste text from the website here...") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val currentState = state) {
                is CuratorState.Loading -> {
                    CircularProgressIndicator()
                    Text("Little Dino is reading and formatting...", modifier = Modifier.padding(top = 8.dp))
                }
                is CuratorState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Successfully added ${currentState.count} books to the library!", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.resetState(); rawText = "" },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Add More")
                    }
                }
                is CuratorState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { viewModel.curateBooks(rawText) }) {
                        Text("Try Again")
                    }
                }
                CuratorState.Idle -> {
                    Button(
                        onClick = { viewModel.curateBooks(rawText) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = rawText.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Curate with AI Magic", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
