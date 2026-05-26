package dk.itu.moapd.x9.s25134

import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.s25134.ui.components.X9BottomBar
import dk.itu.moapd.x9.s25134.ui.screens.*
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import dk.itu.moapd.x9.s25134.viewmodel.ReportViewModel

/**
 * The single activity that hosts all Compose screens via a [NavHost]. Redirects to [LoginActivity]
 * if the user is not authenticated.
 */
class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs = getSharedPreferences(X9Application.PREFS_NAME, MODE_PRIVATE)
            var isDarkMode by remember {
                mutableStateOf(prefs.getBoolean("dark_mode", false))
            }

            X9ComposeTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val reportViewModel: ReportViewModel = viewModel()

                // Main scaffold with bottom navigation
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        X9BottomBar(
                            currentRoute = currentRoute,
                            onHomeClick = {
                                navController.navigate(NavRoutes.HOME) {
                                    popUpTo(NavRoutes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onReportsClick = {
                                navController.navigate(NavRoutes.REPORTS) {
                                    popUpTo(NavRoutes.HOME)
                                    launchSingleTop = true
                                }
                            },
                            onAddClick = {
                                navController.navigate(NavRoutes.ADD) {
                                    popUpTo(NavRoutes.HOME)
                                    launchSingleTop = true
                                }
                            },
                            onMapClick = {
                                navController.navigate(NavRoutes.MAP) {
                                    popUpTo(NavRoutes.HOME)
                                    launchSingleTop = true
                                }
                            },
                            onProfileClick = {
                                navController.navigate(NavRoutes.PROFILE) {
                                    popUpTo(NavRoutes.HOME)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    // Screen navigation
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.HOME) {
                            DashboardScreen(
                                viewModel = reportViewModel,
                                onNewReport = {
                                    navController.navigate(NavRoutes.ADD) {
                                        popUpTo(NavRoutes.HOME)
                                        launchSingleTop = true
                                    }
                                },
                                onViewReports = {
                                    navController.navigate(NavRoutes.REPORTS) {
                                        popUpTo(NavRoutes.HOME)
                                        launchSingleTop = true
                                    }
                                },
                                onViewMap = {
                                    navController.navigate(NavRoutes.MAP) {
                                        popUpTo(NavRoutes.HOME)
                                        launchSingleTop = true
                                    }
                                },
                                onReportClick = { key ->
                                    navController.navigate(NavRoutes.detail(key))
                                }
                            )
                        }
                        composable(NavRoutes.REPORTS) {
                            ReportListScreen(
                                viewModel = reportViewModel,
                                onReportClick = { key ->
                                    navController.navigate(NavRoutes.detail(key))
                                },
                                onReportEdit = { key ->
                                    navController.navigate(NavRoutes.edit(key))
                                }
                            )
                        }
                        composable(NavRoutes.ADD) {
                            ReportFormScreen(
                                viewModel = reportViewModel,
                                onReportAdded = { navController.popBackStack() }
                            )
                        }
                        composable(NavRoutes.MAP) {
                            MapScreen(
                                viewModel = reportViewModel,
                                onReportClick = { key ->
                                    navController.navigate(NavRoutes.detail(key))
                                }
                            )
                        }
                        composable(NavRoutes.PROFILE) {
                            ProfileScreen(
                                onSignOut = ::signOut,
                                onDarkModeChanged = { enabled ->
                                    isDarkMode = enabled
                                }
                            )
                        }
                        composable(NavRoutes.DETAIL_ROUTE) { backStackEntry ->
                            val reportKey = backStackEntry.arguments
                                ?.getString("reportId") ?: return@composable
                            ReportDetailScreen(
                                reportKey = reportKey,
                                viewModel = reportViewModel,
                                onBack = { navController.popBackStack() },
                                onEdit = { key ->
                                    navController.navigate(NavRoutes.edit(key))
                                }
                            )
                        }
                        composable(NavRoutes.EDIT_ROUTE) { backStackEntry ->
                            val reportKey = backStackEntry.arguments
                                ?.getString("reportId") ?: return@composable
                            ReportFormScreen(
                                viewModel = reportViewModel,
                                editKey = reportKey,
                                onReportAdded = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Called after onCreate or after the activity resumes from being stopped. Redirects to
     * [LoginActivity] if no user is currently signed in.
     */
    override fun onStart() {
        super.onStart()
        auth.currentUser ?: startLoginActivity()
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                startLoginActivity()
            }
    }

    private fun startLoginActivity() {
        Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.let(::startActivity)
    }
}
