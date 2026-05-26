@file:Suppress("DEPRECATION")

package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import android.content.*
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.theme.*
import dk.itu.moapd.x9.s25134.viewmodel.ReportViewModel
import dk.itu.moapd.x9.s25134.service.LocationService
import java.io.File
import java.util.Locale
import kotlinx.coroutines.*

/**
 * Form screen for creating or editing traffic reports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    viewModel: ReportViewModel,
    onReportAdded: () -> Unit = {},
    editKey: String? = null,
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val severityLabels = stringArrayResource(R.array.severity_labels)

    val reportUpdatedMsg = stringResource(R.string.msg_report_updated)
    val reportSubmittedMsg = stringResource(R.string.msg_report_submitted)

    // Load existing report if editing
    val state by viewModel.uiState.collectAsState()
    val existingEntry = editKey?.let { key ->
        state.reports.find { it.key == key }
    }
    val existingReport = existingEntry?.report

    // Form state
    var selectedType by rememberSaveable {
        mutableStateOf(existingReport?.type ?: trafficTypes.first())
    }
    var typeExpanded by remember { mutableStateOf(false) }
    var description by rememberSaveable {
        mutableStateOf(existingReport?.description ?: "")
    }
    var severity by rememberSaveable {
        mutableFloatStateOf(existingReport?.severity?.toFloat() ?: 1f)
    } // Float because Slider works with Float values
    var descriptionError by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var latitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var longitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }

    // Has the user entered anything?
    val hasInput = description.isNotBlank() || imageUri != null

    // Back button handler
    BackHandler(enabled = hasInput && editKey == null) {
        showDiscardDialog = true
    }

    // Gallery (uses system file picker filtered to images)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) {
            imageUri = cameraUri
        }
    }

    // Camera Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Create temp file in cache for photo
            val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            // Get secure content:// URI via FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Location Service
    DisposableEffect(Unit) {
        // Keep original location if editing
        if (editKey != null) return@DisposableEffect onDispose {}

        // BroadcastReceiver listens for location updates from LocationService
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Extract location object from broadcast intent
                val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(
                        LocationService.EXTRA_LOCATION, Location::class.java
                    )
                else
                    // Older APIs
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(LocationService.EXTRA_LOCATION)

                // Update lat/lng if valid location
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                }
            }
        }

        // Register receiver and start foreground GPS service
        LocalBroadcastManager.getInstance(context).registerReceiver(
            receiver, IntentFilter(LocationService.ACTION_LOCATION_BROADCAST)
        )
        val serviceIntent = Intent(context, LocationService::class.java)
        context.startForegroundService(serviceIntent)

        // Unregister receiver and stop GPS service if leaving screen
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            context.stopService(serviceIntent)
        }
    }
    // Reverse geocoding
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            address = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val results = Geocoder(context, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0) ?: ""
                // if geocoding fails show nothing rather than crashing
                } catch (_: Exception) {
                    ""
                }
            }
        } else {
            address = ""
        }
    }

    // Report Form Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = stringResource(
                if (editKey != null) R.string.title_edit_report
                else R.string.title_new_report
            ),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Type dropdown
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                trafficTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedType = type
                            typeExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                descriptionError = false
            },
            label = { Text(stringResource(R.string.label_description)) },
            placeholder = { Text(stringResource(R.string.hint_description)) },
            isError = descriptionError,
            supportingText = if (descriptionError) {
                { Text(stringResource(R.string.error_empty_description)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Severity levels
        Text(
            text = stringResource(R.string.label_severity),
            style = MaterialTheme.typography.titleSmall
        )
        val severityColor = severityColor(severity.toInt())
        // Severity slider
        Slider(
            value = severity,
            onValueChange = { severity = it },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = severityColor,
                activeTrackColor = severityColor,
            )
        )
        // Severity label
        Text(
            text = severityLabels[severity.toInt()],
            style = MaterialTheme.typography.bodyMedium,
            color = severityColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Photo buttons
        Text(
            text = stringResource(R.string.label_photo),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            // Camera button
            OutlinedButton(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.button_camera))
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Gallery button
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.button_gallery))
            }
        }

        // Image preview
        val displayUri = imageUri
        val existingImageUrl = existingReport?.imageUrl ?: ""

        if (displayUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = displayUri,
                contentDescription = stringResource(R.string.cd_report_photo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        } else if (existingImageUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = existingImageUrl,
                contentDescription = stringResource(R.string.cd_report_photo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Address
        if (address.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Submit button
        Button(
            onClick = {
                // Description cannot be empty
                if (description.isBlank()) {
                    descriptionError = true
                    return@Button
                }

                val now = System.currentTimeMillis()

                if (editKey != null && existingReport != null) {
                    val updated = existingReport.copy(
                        type = selectedType,
                        description = description.trim(),
                        severity = severity.toInt(),
                        updatedAt = now,
                    )
                    viewModel.updateReport(editKey, updated, imageUri)
                    Toast.makeText(context, reportUpdatedMsg, Toast.LENGTH_SHORT).show()
                } else {
                    val report = TrafficReport(
                        type = selectedType,
                        description = description.trim(),
                        severity = severity.toInt(),
                        creatorId = user?.uid ?: "",
                        userName = user?.displayName ?: "",
                        latitude = latitude,
                        longitude = longitude,
                        createdAt = now,
                        updatedAt = now,
                    )
                    viewModel.addReport(report, imageUri)
                    Toast.makeText(context, reportSubmittedMsg, Toast.LENGTH_SHORT).show()
                }
                onReportAdded()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.button_submit))
        }
    }

    // Discard dialog - when user presses back with unsaved input
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_report_title)) },
            text = { Text(stringResource(R.string.discard_report_message)) },
            confirmButton = {
                TextButton(onClick = { onReportAdded() }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            }
        )
    }
}
