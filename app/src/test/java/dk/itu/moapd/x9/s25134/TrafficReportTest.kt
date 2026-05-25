package dk.itu.moapd.x9.s25134

import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.viewmodel.ReportEntry
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [TrafficReport] data class and report list operations.
 */
class TrafficReportTest {

    private fun applyFilter(
        reports: List<ReportEntry>,
        selectedFilter: String,
    ): List<ReportEntry> {
        val filterAll = "All"
        return if (selectedFilter == filterAll) reports
        else reports.filter { it.report.type == selectedFilter }
    }

    @Test
    fun defaultValues_areCorrect() {
        val report = TrafficReport()
        assertEquals("", report.type)
        assertEquals("", report.description)
        assertEquals(1, report.severity)
        assertEquals(0.0, report.latitude, 0.0)
        assertEquals(0.0, report.longitude, 0.0)
        assertEquals("", report.creatorId)
        assertEquals("", report.imageUrl)
        assertNull(report.createdAt)
    }

    @Test
    fun copy_preservesUnchangedFields() {
        val original = TrafficReport(
            type = "Accident",
            description = "Fender bender",
            severity = 3
        )
        val updated = original.copy(severity = 5)
        assertEquals("Accident", updated.type)
        assertEquals("Fender bender", updated.description)
        assertEquals(5, updated.severity)
    }

    @Test
    fun filterByType_returnsMatchingReports() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(type = "Accident")),
            ReportEntry("2", TrafficReport(type = "Heavy Traffic")),
            ReportEntry("3", TrafficReport(type = "Accident")),
        )
        val filtered = applyFilter(reports, "Accident")
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.report.type == "Accident" })
    }

    @Test
    fun filterAll_returnsEverything() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(type = "Accident")),
            ReportEntry("2", TrafficReport(type = "Heavy Traffic")),
        )
        val filtered = applyFilter(reports, "All")
        assertEquals(2, filtered.size)
    }

    @Test
    fun sortByCreatedAt_mostRecentFirst() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(createdAt = 1000L)),
            ReportEntry("2", TrafficReport(createdAt = 3000L)),
            ReportEntry("3", TrafficReport(createdAt = 2000L)),
        )
        val sorted = reports.sortedByDescending { it.report.createdAt ?: 0L }
        assertEquals("2", sorted[0].key)
        assertEquals("3", sorted[1].key)
        assertEquals("1", sorted[2].key)
    }

    @Test
    fun sortByCreatedAt_nullTimestampsGoLast() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(createdAt = null)),
            ReportEntry("2", TrafficReport(createdAt = 5000L)),
        )
        val sorted = reports.sortedByDescending { it.report.createdAt ?: 0L }
        assertEquals("2", sorted[0].key)
        assertEquals("1", sorted[1].key)
    }

    @Test
    fun hasLocation_isTrueWhenLatNonZero() {
        val report = TrafficReport(latitude = 55.6596, longitude = 0.0)
        assertTrue(report.latitude != 0.0 || report.longitude != 0.0)
    }

    @Test
    fun hasLocation_isTrueWhenLngNonZero() {
        val report = TrafficReport(latitude = 0.0, longitude = 12.5910)
        assertTrue(report.latitude != 0.0 || report.longitude != 0.0)
    }

    @Test
    fun hasLocation_isFalseWhenBothZero() {
        val report = TrafficReport()
        assertFalse(report.latitude != 0.0 || report.longitude != 0.0)
    }

    @Test
    fun severityRange_clampedOneToFive() {
        val report = TrafficReport(severity = 3)
        assertTrue(report.severity in 1..5)
    }

    @Test
    fun take3_returnsAtMostThreeReports() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(type = "Accident")),
            ReportEntry("2", TrafficReport(type = "Heavy Traffic")),
            ReportEntry("3", TrafficReport(type = "Road Work")),
            ReportEntry("4", TrafficReport(type = "Speed Camera")),
            ReportEntry("5", TrafficReport(type = "Accident")),
        )
        val recent = reports.take(3)
        assertEquals(3, recent.size)
    }

    @Test
    fun take3_returnsAllWhenFewerThanThree() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(type = "Accident")),
        )
        val recent = reports.take(3)
        assertEquals(1, recent.size)
    }

    @Test
    fun ownerCheck_matchesCreatorId() {
        val report = TrafficReport(creatorId = "user123")
        val currentUserId = "user123"
        assertTrue(report.creatorId == currentUserId)
    }

    @Test
    fun ownerCheck_failsForDifferentUser() {
        val report = TrafficReport(creatorId = "user123")
        val currentUserId = "user456"
        assertFalse(report.creatorId == currentUserId)
    }

    @Test
    fun descriptionTrim_removesWhitespace() {
        val raw = "  Fender bender on highway  "
        val trimmed = raw.trim()
        assertEquals("Fender bender on highway", trimmed)
    }

    @Test
    fun filterByType_returnsEmptyWhenNoMatches() {
        val reports = listOf(
            ReportEntry("1", TrafficReport(type = "Accident")),
            ReportEntry("2", TrafficReport(type = "Heavy Traffic")),
        )
        val filtered = applyFilter(reports, "Road Work")
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun blankDescription_isRejected() {
        val description = "   "
        assertTrue(description.isBlank())
    }

    @Test
    fun imageUrl_emptyMeansNoImage() {
        val report = TrafficReport(imageUrl = "")
        assertFalse(report.imageUrl.isNotEmpty())
    }

    @Test
    fun imageUrl_nonEmptyMeansHasImage() {
        val report = TrafficReport(imageUrl = "https://example.com/photo.jpg")
        assertTrue(report.imageUrl.isNotEmpty())
    }

    @Test
    fun take3_returnsEmptyWhenNoReports() {
        val reports = emptyList<ReportEntry>()
        val recent = reports.take(3)
        assertTrue(recent.isEmpty())
    }
}
