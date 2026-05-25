package dk.itu.moapd.x9.s25134.ui.theme

import androidx.compose.ui.graphics.Color

// Light scheme
val Primary = Color(0xFF06d6a0)
val OnPrimary = Color(0xFF0a0e1a)
val PrimaryContainer = Color(0xFFc8f7ec)
val OnPrimaryContainer = Color(0xFF002117)

val Secondary = Color(0xFF118ab2)
val OnSecondary = Color(0xFFffffff)
val SecondaryContainer = Color(0xFFc8e8f4)
val OnSecondaryContainer = Color(0xFF001f2a)

val Background = Color(0xFFf0f4f8)
val OnBackground = Color(0xFF191c20)
val Surface = Color(0xFFffffff)
val OnSurface = Color(0xFF191c20)
val SurfaceVariant = Color(0xFFe8f5f0)
val OnSurfaceVariant = Color(0xFF44485a)
val Outline = Color(0xFF74788a)
val OutlineVariant = Color(0xFFc4c8da)

val ErrorColor = Color(0xFFD32F2F)
val OnErrorColor = Color(0xFFffffff)

// Dark scheme
val PrimaryDark = Color(0xFF06d6a0)
val OnPrimaryDark = Color(0xFF0a0e1a)
val PrimaryContainerDark = Color(0xFF004d38)
val OnPrimaryContainerDark = Color(0xFFc8f7ec)

val SecondaryDark = Color(0xFF67c9e8)
val OnSecondaryDark = Color(0xFF003547)
val SecondaryContainerDark = Color(0xFF004d67)
val OnSecondaryContainerDark = Color(0xFFc8e8f4)

val BackgroundDark = Color(0xFF0a0e1a)
val OnBackgroundDark = Color(0xFFf0f4f8)
val SurfaceDark = Color(0xFF111827)
val OnSurfaceDark = Color(0xFFf0f4f8)
val SurfaceVariantDark = Color(0xFF1a2236)
val OnSurfaceVariantDark = Color(0xFF8899b4)
val OutlineDark = Color(0xFF1e2a45)
val OutlineVariantDark = Color(0xFF2a3a5c)

// Severity
val SeverityMinor = Color(0xFF4CAF50)
val SeverityLow = Color(0xFF8BC34A)
val SeverityModerate = Color(0xFFFFC107)
val SeverityHigh = Color(0xFFFF9800)
val SeverityCritical = Color(0xFFF44336)

fun severityColor(level: Int): Color = when (level) {
    1 -> SeverityMinor
    2 -> SeverityLow
    3 -> SeverityModerate
    4 -> SeverityHigh
    5 -> SeverityCritical
    else -> SeverityMinor
}

// Swipe actions
val SwipeEdit = Color(0xFF2E7D32)
val SwipeDelete = Color(0xFFC62828)
val SwipeIdle = Color.Transparent
val SwipeIconTint = Color.White
