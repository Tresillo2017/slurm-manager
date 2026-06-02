package com.tomasps.slrummanager.presentation.submit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitJobSheet(
    serverId: String?,
    servers: List<Server>,
    onDismiss: () -> Unit,
    viewModel: SubmitJobViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val script = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            viewModel.update { copy(scriptContent = script) }
        }
    }

    LaunchedEffect(serverId) {
        if (serverId != null) viewModel.update { copy(selectedServerId = serverId) }
        else if (servers.isNotEmpty()) viewModel.update { copy(selectedServerId = servers.first().id.toString()) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Submit Job", style = MaterialTheme.typography.titleLarge)

            if (servers.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = servers.find { it.id.toString() == state.selectedServerId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Server") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        servers.forEach { server ->
                            DropdownMenuItem(
                                text = { Text(server.name) },
                                onClick = { viewModel.update { copy(selectedServerId = server.id.toString()) }; expanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.scriptContent.isBlank()) "Pick Script File" else "Script Loaded ✓")
            }

            Text("Or paste script:", style = MaterialTheme.typography.labelMedium)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                BasicTextField(
                    value = state.scriptContent,
                    onValueChange = { viewModel.update { copy(scriptContent = it) } },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.partition, onValueChange = { viewModel.update { copy(partition = it) } }, label = { Text("Partition") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = state.nodes, onValueChange = { viewModel.update { copy(nodes = it) } }, label = { Text("Nodes") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.cpus, onValueChange = { viewModel.update { copy(cpus = it) } }, label = { Text("CPUs") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = state.timeLimit, onValueChange = { viewModel.update { copy(timeLimit = it) } }, label = { Text("Time Limit") }, modifier = Modifier.weight(1f))
            }

            state.submitResult?.let { result ->
                Text(result, color = if (result.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = { viewModel.submit(context, onDismiss) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.scriptContent.isNotBlank() && !state.isSubmitting
            ) {
                if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Submit")
            }
        }
    }
}
