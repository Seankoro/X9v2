package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import coil3.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.*
import dk.itu.moapd.x9.s25134.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays a Google Map with markers for all reports that have a location.
 */
@Composable
fun MapScreen(
    viewModel: ReportViewModel,
    onReportClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionDeniedMsg = stringResource(R.string.permission_denied_message)

    val state by viewModel.uiState.collectAsState()

    // Camera state
    val cameraPositionState = rememberCameraPositionState()

    // Permission check
    var hasPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher for the system permission dialog
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar(permissionDeniedMsg)
            }
        }
    }

    // Permission + location centering
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(location.latitude, location.longitude), 14f
                        )
                    }
                }
        }
    }

    // Selected marker
    var selectedReportKey by rememberSaveable { mutableStateOf<String?>(null) }
    // Look up full report from key
    val selectedEntry = selectedReportKey?.let { key ->
        state.reports.find { it.key == key }
    }
    // Map Layout
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                ),
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = hasPermission,
                ),
                onMapClick = { selectedReportKey = null },
            ) {
                // Place markers
                state.reports.forEach { entry ->
                    val report = entry.report
                    if (report.latitude != 0.0 || report.longitude != 0.0) {
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = LatLng(report.latitude, report.longitude)
                            ),
                            title = report.type,
                            snippet = report.description.take(50),
                            onClick = {
                                selectedReportKey = entry.key
                                true
                            }
                        )
                    }
                }
            }
            // Info card
            if (selectedEntry != null) {
                val report = selectedEntry.report
                val dateFormat = remember {
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                }
                val severityColor = severityColor(report.severity)

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        // Left side: text details
                        Column(modifier = Modifier.weight(1f)) {
                            // Report type
                            Text(
                                text = report.type,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Severity
                            Text(
                                text = stringResource(R.string.severity_display, report.severity),
                                style = MaterialTheme.typography.bodySmall,
                                color = severityColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Description
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            // Timestamp
                            report.createdAt?.let { timestamp ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Link to full detail screen
                            TextButton(
                                onClick = { onReportClick(selectedEntry.key) }
                            ) {
                                Text(stringResource(R.string.cd_view_report))
                            }
                        }
                        // Right side: thumbnail (if report has image)
                        if (report.imageUrl.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            AsyncImage(
                                model = report.imageUrl,
                                contentDescription = stringResource(R.string.cd_report_photo),
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}