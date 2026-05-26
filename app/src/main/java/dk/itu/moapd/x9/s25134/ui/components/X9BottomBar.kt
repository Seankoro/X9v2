package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.NavRoutes
import dk.itu.moapd.x9.s25134.R

/**
 * The bottom navigation bar shown on every screen except login.
 * Five destinations: Home, Reports, Add (Centre Floating Action Button), Map, Profile.
 */
@Composable
fun X9BottomBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onReportsClick: () -> Unit,
    onAddClick: () -> Unit,
    onMapClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val isReportActive =
        currentRoute == NavRoutes.REPORTS ||
        currentRoute?.startsWith("${NavRoutes.REPORTS}?") == true ||
        currentRoute == NavRoutes.DETAIL_ROUTE ||
        currentRoute == NavRoutes.EDIT_ROUTE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BottomNavItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                selected = currentRoute == NavRoutes.HOME,
                onClick = onHomeClick
            )
            // Reports
            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_reports),
                selected = isReportActive,
                onClick = onReportsClick
            )
            // Add (Centre FAB)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_report),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Map
            BottomNavItem(
                icon = Icons.Default.Map,
                label = stringResource(R.string.nav_map),
                selected = currentRoute == NavRoutes.MAP,
                onClick = onMapClick
            )
            // Profile
            BottomNavItem(
                icon = Icons.Default.Person,
                label = stringResource(R.string.nav_profile),
                selected = currentRoute == NavRoutes.PROFILE,
                onClick = onProfileClick
            )
        }
    }
}

/** A single icon-and-label navigation item in the navigation bar. */
@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
