package dk.itu.moapd.x9.s25134.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dk.itu.moapd.x9.s25134.X9Application
import dk.itu.moapd.x9.s25134.model.TrafficReport

/**
 * Wraps Firebase Realtime Database operations for traffic reports.
 */
class ReportRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: DatabaseReference = Firebase.database(X9Application.DATABASE_URL).reference,
) {

    companion object {
        private const val REPORTS = "reports"
        private const val CREATED_AT = "createdAt"
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun reportsQuery(): Query =
        database.child(REPORTS).orderByChild(CREATED_AT)

    fun addReport(report: TrafficReport) {
        val key = database.child(REPORTS).push().key ?: return
        database.child(REPORTS).child(key).setValue(report)
    }

    fun updateReport(key: String, report: TrafficReport) {
        database.child(REPORTS)
            .child(key)
            .setValue(report)
    }

    fun deleteReport(key: String) {
        database.child(REPORTS)
            .child(key)
            .removeValue()
    }
}
