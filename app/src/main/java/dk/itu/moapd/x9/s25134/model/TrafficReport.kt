package dk.itu.moapd.x9.s25134.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * A traffic report stored in Firebase Realtime Database.
 *
 * All fields have default values so Firebase can deserialize snapshots using
 * `DataSnapshot.getValue(TrafficReport::class.java)`.
 *
 * @property type The type of traffic event (e.g., "Heavy Traffic", "Accident").
 * @property description A user-provided description of the situation.
 * @property severity A severity level from 1 (minor) to 5 (critical).
 * @property latitude The latitude where the report was created.
 * @property longitude The longitude where the report was created.
 * @property creatorId The UID of the user who created the report.
 * @property userName The display name of the user who created the report.
 * @property imageUrl The download URL of an attached photo, empty if none.
 * @property createdAt The creation timestamp in milliseconds.
 * @property updatedAt The last update timestamp in milliseconds.
 */
@IgnoreExtraProperties
data class TrafficReport(
    val type: String = "",
    val description: String = "",
    val severity: Int = 1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val creatorId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
