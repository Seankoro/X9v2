package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.location.Geocoder
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.*
import dk.itu.moapd.x9.s25134.viewmodel.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays a list of all traffic reports with filter chips and swipe actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    viewModel: ReportViewModel,
    onReportClick: (String) -> Unit = {},
    onReportEdit: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    // Snackbar setup
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.delete_label)

    // For owner check
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Filter setup
    val filterAll = stringResource(R.string.filter_all)
    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val types = listOf(filterAll) + trafficTypes
    var selectedFilter by rememberSaveable { mutableStateOf(filterAll) }

    // Apply filter
    val filteredReports = if (selectedFilter == filterAll) {
        state.reports
    } else {
        state.reports.filter { it.report.type == selectedFilter }
    }

    // Report List Layout
    Column(modifier = Modifier.fillMaxSize()) {
        // Title
        Text(
            text = stringResource(R.string.subtitle_text),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // loop through list, create one FilterChip per type
            items(types) { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = type },
                    label = { Text(type) }
                )
            }
        }

        // Report count
        Text(
            text = if (selectedFilter == filterAll) {
                stringResource(R.string.report_count, state.reports.size)
            } else {
                stringResource(
                    R.string.filtered_count,
                    filteredReports.size,
                    state.reports.size
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Auto-scroll to top when filter changes
        val listState = rememberLazyListState()
        LaunchedEffect(selectedFilter) { listState.animateScrollToItem(0) }


        SnackbarHost(hostState = snackbarHostState)

        // Report list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        ) {
            items(items = filteredReports, key = { it.key }) { entry ->
                // Check if owner
                val isOwner = entry.report.creatorId == currentUserId
                // Swipe state
                val dismissState = rememberSwipeToDismissBoxState()
                // Swipe setup
                LaunchedEffect(dismissState.currentValue) {
                    when (dismissState.currentValue) {
                        // Swipe left to right = EDIT
                        SwipeToDismissBoxValue.StartToEnd -> {
                            if (isOwner) onReportEdit(entry.key)
                            dismissState.reset()
                        }
                        // Swipe right to left = DELETE
                        SwipeToDismissBoxValue.EndToStart -> {
                            if (isOwner) {
                                viewModel.deleteReport(entry.key, entry.report.imageUrl)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = deletedMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                dismissState.reset()
                            }
                        }
                        SwipeToDismissBoxValue.Settled -> {}
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // Colour and icon based on swipe direction
                        val direction = dismissState.dismissDirection
                        val color = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> SwipeEdit // green
                            SwipeToDismissBoxValue.EndToStart -> SwipeDelete // red
                            else -> SwipeIdle // transparent
                        }
                        val icon = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                            else -> Icons.Default.Delete
                        }
                        // Coloured box behind card + icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                                Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Icon(icon, contentDescription = null, tint = SwipeIconTint)
                        }
                    },
                    // Only swipeable if owner
                    enableDismissFromStartToEnd = isOwner,
                    enableDismissFromEndToStart = isOwner,
                    content = {
                        ReportCard(
                            entry = entry,
                            onClick = { onReportClick(entry.key) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReportCard(
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
    // Report Card Layout
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Show thumbnail only if report has an image URL
            if (report.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = stringResource(R.string.cd_report_photo),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            // Use remaining horizontal space after thumbnail
            Column(modifier = Modifier.weight(1f)) {
                // Report type
                Text(
                    text = report.type,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Severity
                Text(
                    text = stringResource(R.string.severity_display, report.severity),
                    style = MaterialTheme.typography.bodySmall,
                    color = severityColor
                )
                // Location
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
            }
        }
    }
}
