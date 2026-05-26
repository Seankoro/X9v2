package dk.itu.moapd.x9.s25134.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.*
import dk.itu.moapd.x9.s25134.viewmodel.ReportViewModel
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays the full details of a single traffic report.
 */
@Composable
fun ReportDetailScreen(
    reportKey: String,
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    // Find this specific report in the list by its Firebase key
    val entry = state.reports.find { it.key == reportKey }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    }

    // Check if report was deleted
    LaunchedEffect(entry) {
        if (entry == null) onBack()
    }
    // If entry is deleted don't render anything and exit
    if (entry == null) return

    val report = entry.report
    val isOwner = report.creatorId == currentUserId
    val severityColor = severityColor(report.severity)

    // Reverse geocoding
    val hasLocation = report.latitude != 0.0 || report.longitude != 0.0
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }

    LaunchedEffect(report.latitude, report.longitude) {
        if (hasLocation) {
            address = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val results = Geocoder(context, Locale.getDefault())
                        .getFromLocation(report.latitude, report.longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0) ?: ""
                // if geocoding fails show nothing rather than crashing
                } catch (_: Exception) {
                    ""
                }
            }
        }
    }

    // Report Detail Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Report type
        Text(
            text = report.type,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Severity
        Text(
            text = stringResource(R.string.severity_display, report.severity),
            style = MaterialTheme.typography.titleMedium,
            color = severityColor
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        // Address (only if location exists and geocoding succeeded)
        if (hasLocation && address.isNotEmpty()) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }
        // Timestamp
        report.createdAt?.let { timestamp ->
            Text(
                text = dateFormat.format(Date(timestamp)),
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }
        // Full Description
        Text(
            text = report.description,
            style = MaterialTheme.typography.bodyLarge
        )
        // Full Image
        if (report.imageUrl.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            AsyncImage(
                model = report.imageUrl,
                contentDescription = stringResource(R.string.cd_report_photo),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        // Owner actions
        if (isOwner) {
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                // Edit
                Button(
                    onClick = { onEdit(reportKey) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.edit_label))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Delete
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.delete_label))
                }
            }
        }
    }
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReport(reportKey, report.imageUrl)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }
}
