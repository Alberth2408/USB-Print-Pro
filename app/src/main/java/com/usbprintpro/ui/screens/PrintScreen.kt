package com.usbprintpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.usbprintpro.domain.model.Orientation
import com.usbprintpro.domain.model.PaperSize
import com.usbprintpro.ui.viewmodel.PrintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintScreen(
    onBack: () -> Unit,
    viewModel: PrintViewModel = hiltViewModel()
) {
    val text by viewModel.text.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val hexPreview by viewModel.hexPreview.collectAsState()
    val status by viewModel.status.collectAsState()
    val exportPath by viewModel.exportPath.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportPath) {
        exportPath?.let {
            snackbarHostState.showSnackbar("Exportado a: $it")
            viewModel.clearExportPath()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Imprimir") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Texto a imprimir:",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Escribe aquí el texto...") },
                maxLines = 5
            )

            Text(
                text = "Configuración:",
                style = MaterialTheme.typography.titleMedium
            )

            SettingsSection(
                copies = settings.copies,
                orientation = settings.orientation,
                paperSize = settings.paperSize,
                onCopiesChange = { viewModel.updateCopies(it) },
                onOrientationChange = { viewModel.updateOrientation(it) },
                onPaperSizeChange = { viewModel.updatePaperSize(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testPrint() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🧪 Probar (sin USB)")
                }

                Button(
                    onClick = { viewModel.exportToFile() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💾 Exportar .prn")
                }
            }

            if (hexPreview.isNotEmpty()) {
                HexPreviewCard(hexDump = hexPreview)
            }

            StatusCard(status = status)
        }
    }
}

@Composable
private fun SettingsSection(
    copies: Int,
    orientation: Orientation,
    paperSize: PaperSize,
    onCopiesChange: (Int) -> Unit,
    onOrientationChange: (Orientation) -> Unit,
    onPaperSizeChange: (PaperSize) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CopiesRow(copies = copies, onCopiesChange = onCopiesChange)
            DropdownRow(
                label = "Orientación",
                selected = orientation.label,
                items = Orientation.entries.map { it.label to it },
                onSelect = onOrientationChange
            )
            DropdownRow(
                label = "Papel",
                selected = paperSize.label,
                items = PaperSize.entries.map { it.label to it },
                onSelect = onPaperSizeChange
            )
        }
    }
}

@Composable
private fun CopiesRow(
    copies: Int,
    onCopiesChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Copias",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onCopiesChange(copies - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = "Restar")
        }
        Text(
            text = copies.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = { onCopiesChange(copies + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Sumar")
        }
    }
}

@Composable
private fun <T> DropdownRow(
    label: String,
    selected: String,
    items: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HexPreviewCard(hexDump: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vista previa PCL (hex):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hexDump,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun StatusCard(status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
