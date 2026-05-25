package dk.itu.moapd.x9.s25134.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.*
import dk.itu.moapd.x9.s25134.viewmodel.ReportEntry
import dk.itu.moapd.x9.s25134.viewmodel.ReportViewModel
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * The home screen showing a greeting, report summary and quick navigation.
 */
@Composable
fun DashboardScreen(
    viewModel: ReportViewModel,
    onNewReport: () -> Unit = {},
    onViewReports: () -> Unit = {},
    onViewMap: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val user = FirebaseAuth.getInstance().currentUser
    // Greet user based on hour of day
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    // Beginning of Dashboard Screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Greeting
        Text(
            text = "$greeting, ${user?.displayName ?: "User"}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Report Count
        Text(
            text = stringResource(R.string.report_count, state.reports.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Quick action buttons, New Report and View Map
        Row {
            Button(onClick = onNewReport) {
                Text(text = stringResource(R.string.quick_action_new_report))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onViewMap) {
                Text(text = stringResource(R.string.quick_action_view_map))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Recent Reports Section
        Text(
            text = stringResource(R.string.section_recent_reports),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        // No reports :(
        if (state.reports.isEmpty()) {
            Text(
                text = stringResource(R.string.msg_no_reports),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // show 3 most recent reports
            state.reports.take(3).forEach { entry ->
                DashboardReportCard(
                    entry = entry,
                    onClick = { onReportClick(entry.key) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // show View All button if more than 3 reports
            if (state.reports.size > 3) {
                Button(onClick = onViewReports) {
                    Text(text = stringResource(R.string.btn_view_all))
                }
            }
        }
    }
}

@Composable
private fun DashboardReportCard(
    entry: ReportEntry,
    onClick: () -> Unit = {},
) {
    val report = entry.report
    val context = LocalContext.current

    val severityColor = severityColor(report.severity)
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    }
    val hasLocation = report.latitude != 0.0 || report.longitude != 0.0
    var address by remember { mutableStateOf("") }
    LaunchedEffect(report.latitude, report.longitude) {
        if (hasLocation) {
            // run block on background thread
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
    // Report card for Recent Reports Section
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Report Type
            Text(
                text = report.type,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Severity
            Text(
                text = stringResource(R.string.severity_display, report.severity),
                style = MaterialTheme.typography.bodySmall,
                color = severityColor
            )
            // Address (only shown if geocoding succeeded)
            if (address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Description preview
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            // Timestamp (only shown if createdAt is not null)
            report.createdAt?.let { timestamp ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}