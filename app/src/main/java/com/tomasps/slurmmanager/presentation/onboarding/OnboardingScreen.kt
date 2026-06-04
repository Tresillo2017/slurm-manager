package com.tomasps.slurmmanager.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slurmmanager.domain.model.AuthMethod
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            TopAppBar(
                title = { Text(if (addServerOnly) "Add Server" else "Welcome") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage(onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(1,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    }
                })
                1 -> AddServerPage(
                    state = state,
                    onUpdate = viewModel::update,
                    onPickKey = { keyFilePicker.launch(arrayOf("*/*")) },
                    onNext = {
                        scope.launch {
                            pagerState.animateScrollToPage(2,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            viewModel.testConnection(context)
                        }
                    }
                )
                2 -> TestConnectionPage(
                    state = state,
                    onTest = { viewModel.testConnection(context) },
                    onFinish = { viewModel.saveServer(context, isFirstServer = !addServerOnly, onFinished) }
                )
            }
        }
    }
}

// ─── Welcome ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            "SLURM Manager",
            style = MaterialTheme.typography.headlineLargeEmphasized,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Monitor and control your HPC jobs across multiple clusters.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(64.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
    }
}

// ─── Add Server ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddServerPage(
    state: OnboardingUiState,
    onUpdate: (OnboardingUiState.() -> OnboardingUiState) -> Unit,
    onPickKey: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text("Connect a cluster", style = MaterialTheme.typography.headlineMediumEmphasized)
        Text(
            "Enter the SSH connection details for your HPC cluster.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // Connection fields
        OutlinedTextField(
            value = state.name, onValueChange = { onUpdate { copy(name = it) } },
            label = { Text("Display Name") },
            placeholder = { Text("e.g. My HPC Cluster") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.hostname, onValueChange = { onUpdate { copy(hostname = it) } },
            label = { Text("Hostname") },
            placeholder = { Text("e.g. login.cluster.edu") },
            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.port, onValueChange = { onUpdate { copy(port = it) } },
                label = { Text("Port") },
                modifier = Modifier.width(110.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.username, onValueChange = { onUpdate { copy(username = it) } },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Auth method toggle
        Text("Authentication", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth()
        ) {
            AuthMethod.entries.forEachIndexed { i, method ->
                ToggleButton(
                    checked = state.authMethod == method,
                    onCheckedChange = { onUpdate { copy(authMethod = method) } },
                    modifier = Modifier.weight(1f),
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        AuthMethod.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Icon(
                        if (method == AuthMethod.PASSWORD) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (method == AuthMethod.PASSWORD) "Password" else "SSH Key")
                }
            }
        }

        // Auth credential input
        AnimatedContent(
            targetState = state.authMethod,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(
                    spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.95f
                )) togetherWith fadeOut()
            },
            label = "auth_method"
        ) { method ->
            if (method == AuthMethod.PASSWORD) {
                OutlinedTextField(
                    value = state.password, onValueChange = { onUpdate { copy(password = it) } },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            } else {
                if (state.privateKeyPem.isBlank()) {
                    OutlinedButton(onClick = onPickKey, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Private Key File")
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Private key loaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            TextButton(onClick = onPickKey) { Text("Change") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hostname.isNotBlank() && state.username.isNotBlank()
        ) {
            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Test Connection")
        }
    }
}

// ─── Test Connection ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TestConnectionPage(
    state: OnboardingUiState,
    onTest: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            state.isTesting -> {
                LoadingIndicator(modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(32.dp))
                Text("Testing connection…",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Connecting via SSH",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
            state.testSuccess == true -> {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text("Connection successful",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Latency: ${state.testLatencyMs}ms",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(48.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Server")
                }
            }
            state.testSuccess == false -> {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Error, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text("Connection failed",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp))
                        Text(state.testError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(32.dp))
                FilledTonalButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}
