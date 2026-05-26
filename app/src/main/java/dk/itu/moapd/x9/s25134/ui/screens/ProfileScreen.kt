package dk.itu.moapd.x9.s25134.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.core.content.edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.X9Application

/**
 * Displays the user's profile information, a dark mode toggle and a sign-out button.
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    onDarkModeChanged: (Boolean) -> Unit = {},
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(X9Application.PREFS_NAME, Context.MODE_PRIVATE)
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

    // Profile Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display name
        Text(
            text = user?.displayName ?: "User",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Email
        Text(
            text = user?.email ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Dark mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_dark_mode),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = darkMode,
                onCheckedChange = { enabled ->
                    darkMode = enabled
                    prefs.edit { putBoolean("dark_mode", enabled) }
                    onDarkModeChanged(enabled) // Notify MainActivity to switch theme
                }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Sign out button
        Button(onClick = onSignOut) {
            Text(text = stringResource(R.string.sign_out_label))
        }
    }
}