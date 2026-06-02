package com.tomasps.slrummanager.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.AuthMethod
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    addServerOnly: Boolean = false,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(initialPage = if (addServerOnly) 1 else 0) { 3 }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val keyFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val pem = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            viewModel.update { copy(privateKeyPem = pem) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (addServerOnly) "Add Server" else "Welcome") })
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage(onNext = { scope.launch { pagerState.animateScrollToPage(1) } })
                1 -> AddServerPage(
                    state = state,
                    onUpdate = viewModel::update,
                    onPickKey = { keyFilePicker.launch(arrayOf("*/*")) },
                    onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
                2 -> TestConnectionPage(
                    state = state,
                    onTest = { viewModel.testConnection(context) },
                    onFinish = { viewModel.saveServer(context, onFinished) }
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SLURM Manager", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Text(
            "Monitor and control your HPC jobs across multiple clusters.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
    }
}

@Composable
private fun AddServerPage(
    state: OnboardingUiState,
    onUpdate: (OnboardingUiState.() -> OnboardingUiState) -> Unit,
    onPickKey: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Server", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.name, onValueChange = { onUpdate { copy(name = it) } },
            label = { Text("Display Name (optional)") }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.hostname, onValueChange = { onUpdate { copy(hostname = it) } },
            label = { Text("Hostname") }, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.port, onValueChange = { onUpdate { copy(port = it) } },
                label = { Text("Port") }, modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.username, onValueChange = { onUpdate { copy(username = it) } },
                label = { Text("Username") }, modifier = Modifier.weight(1f)
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AuthMethod.entries.forEachIndexed { i, method ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(i, AuthMethod.entries.size),
                    onClick = { onUpdate { copy(authMethod = method) } },
                    selected = state.authMethod == method,
                    label = { Text(if (method == AuthMethod.PASSWORD) "Password" else "SSH Key") }
                )
            }
        }
        if (state.authMethod == AuthMethod.PASSWORD) {
            OutlinedTextField(
                value = state.password, onValueChange = { onUpdate { copy(password = it) } },
                label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
        } else {
            OutlinedButton(onClick = onPickKey, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.privateKeyPem.isBlank()) "Import Private Key" else "Key Loaded ✓")
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hostname.isNotBlank() && state.username.isNotBlank()
        ) { Text("Test Connection") }
    }
}

@Composable
private fun TestConnectionPage(
    state: OnboardingUiState,
    onTest: () -> Unit,
    onFinish: () -> Unit
) {
    LaunchedEffect(Unit) { onTest() }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            state.isTesting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Testing connection…", style = MaterialTheme.typography.bodyLarge)
            }
            state.testSuccess == true -> {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Connected in ${state.testLatencyMs}ms", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Save Server") }
            }
            state.testSuccess == false -> {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(state.testError, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
            }
        }
    }
}
