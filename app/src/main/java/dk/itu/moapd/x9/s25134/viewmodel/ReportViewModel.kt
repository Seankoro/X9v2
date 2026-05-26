package dk.itu.moapd.x9.s25134.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import dk.itu.moapd.x9.s25134.data.ReportRepository
import dk.itu.moapd.x9.s25134.data.StorageRepository
import dk.itu.moapd.x9.s25134.model.TrafficReport
import kotlinx.coroutines.flow.*

/**
 * A report entry with its Firebase key.
 */
data class ReportEntry(
    val key: String,
    val report: TrafficReport,
)

/**
 * The UI state for screens that display traffic reports.
 */
data class ReportUiState(
    val reports: List<ReportEntry> = emptyList(),
)

/**
 * Observes Firebase Realtime Database for traffic report changes and handles image uploads.
 */
class ReportViewModel(
    private val reportRepository: ReportRepository = ReportRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
) : ViewModel() {

    companion object {
        private val TAG = ReportViewModel::class.qualifiedName
        private const val IMAGES = "report-images"
    }

    // Private mutable state
    private val _uiState = MutableStateFlow(ReportUiState())
    // Public read-only state observed by UI screens
    val uiState: StateFlow<ReportUiState> = _uiState

    private var listener: ValueEventListener? = null

    init {
        observeReports()
    }

    /** Observe the changes in the Firebase Realtime Database. */
    private fun observeReports() {
        val query = reportRepository.reportsQuery()

        val valueListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val key = child.key ?: return@mapNotNull null
                    val report = child.getValue(TrafficReport::class.java)
                        ?: return@mapNotNull null
                    ReportEntry(key = key, report = report)
                }.sortedByDescending { it.report.createdAt ?: 0L }

                _uiState.value = ReportUiState(reports = items)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeReports:onCancelled", error.toException())
            }
        }

        listener = valueListener
        query.addValueEventListener(valueListener)
    }

    /** Removes the Firebase listener to prevent memory leaks when the ViewModel is destroyed. */
    override fun onCleared() {
        super.onCleared()
        listener?.let {
            reportRepository.reportsQuery().removeEventListener(it)
        }
    }

    /** Adds a new report. Uploads the image first if one is provided. */
    fun addReport(report: TrafficReport, imageUri: Uri? = null) {
        if (imageUri == null) {
            reportRepository.addReport(report)
            return
        }

        val remotePath = "$IMAGES/${reportRepository.currentUserId()}/${System.currentTimeMillis()}"
        storageRepository.uploadImage(imageUri, remotePath)
            .addOnSuccessListener { downloadUrl ->
                val updated = report.copy(imageUrl = downloadUrl.toString())
                reportRepository.addReport(updated)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Image upload failed, saving without image", e)
                reportRepository.addReport(report)
            }
    }

    /** Updates an existing report. Uploads a new image first if one is provided. */
    fun updateReport(key: String, report: TrafficReport, imageUri: Uri? = null) {
        if (imageUri == null) {
            reportRepository.updateReport(key, report)
            return
        }

        val remotePath = "$IMAGES/${reportRepository.currentUserId()}/${System.currentTimeMillis()}"
        storageRepository.uploadImage(imageUri, remotePath)
            .addOnSuccessListener { downloadUrl ->
                val updated = report.copy(imageUrl = downloadUrl.toString())
                reportRepository.updateReport(key, updated)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Image upload failed, updating without new image", e)
                reportRepository.updateReport(key, report)
            }
    }

    /** Deletes a report and its image from storage if it has one. */
    fun deleteReport(key: String, imageUrl: String = "") {
        if (imageUrl.isEmpty()) {
            reportRepository.deleteReport(key)
            return
        }

        storageRepository.deleteImage(imageUrl)
            .addOnSuccessListener { reportRepository.deleteReport(key) }
            .addOnFailureListener {
                Log.w(TAG, "Storage delete failed, deleting report anyway")
                reportRepository.deleteReport(key)
            }
    }
}