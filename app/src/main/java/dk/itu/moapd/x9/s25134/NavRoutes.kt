package dk.itu.moapd.x9.s25134

/**
 * Centralised route constants for Compose Navigation.
 * Every screen's route string is defined here to avoid scattered magic strings.
 */
object NavRoutes {
    const val HOME = "home"
    const val REPORTS = "reports"
    const val ADD = "add"
    const val DETAIL_ROUTE = "detail/{reportId}"
    const val EDIT_ROUTE = "edit/{reportId}"
    const val MAP = "map"
    const val PROFILE = "profile"

    fun detail(reportId: String) = "detail/$reportId"
    fun edit(reportId: String) = "edit/$reportId"
}
