package com.usbprintpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.usbprintpro.domain.model.USBPrinter
import com.usbprintpro.ui.components.PrinterCard
import com.usbprintpro.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPrint: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val printers by viewModel.printers.collectAsState()
    val status by viewModel.status.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB Print Pro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (printers.isEmpty()) {
                EmptyState(
                    isScanning = isScanning,
                    status = status,
                    onSearch = { viewModel.discoverPrinters() },
                    onTestMode = onNavigateToPrint
                )
            } else {
                PrinterList(
                    printers = printers,
                    status = status,
                    onSearch = { viewModel.discoverPrinters() },
                    isScanning = isScanning,
                    onTestMode = onNavigateToPrint
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyState(
    isScanning: Boolean,
    status: String,
    onSearch: () -> Unit,
    onTestMode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Print,
            contentDescription = "Impresora",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Conecta tu impresora mediante USB OTG",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Usa un adaptador USB OTG para conectar tu impresora y podrás imprimir documentos, imágenes y más.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Buscar impresoras",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onTestMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Modo prueba (sin USB OTG)",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(status = status)
    }
}

@Composable
private fun ColumnScope.PrinterList(
    printers: List<USBPrinter>,
    status: String,
    onSearch: () -> Unit,
    isScanning: Boolean,
    onTestMode: () -> Unit
) {
    Text(
        text = "Impresoras detectadas",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(printers, key = { it.deviceId }) { printer ->
            PrinterCard(
                name = printer.name,
                manufacturer = printer.manufacturer
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    StatusCard(status = status)

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onSearch,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isScanning
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Buscar nuevamente",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onTestMode,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Modo prueba (sin USB OTG)",
            style = MaterialTheme.typography.labelLarge
        )
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
