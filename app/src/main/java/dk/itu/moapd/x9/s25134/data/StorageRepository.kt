package dk.itu.moapd.x9.s25134.data

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.storage.*

/**
 * Handles Firebase Storage operations for uploading and deleting report images.
 */
class StorageRepository(
    private val storage: FirebaseStorage = Firebase.storage,
) {

    fun uploadImage(localUri: Uri, remotePath: String): Task<Uri> {
        val ref: StorageReference = storage.reference.child(remotePath)
        return ref.putFile(localUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                throw (task.exception ?: Exception("Upload failed"))
            }
            ref.downloadUrl
        }
    }

    fun deleteImage(url: String): Task<Void> {
        return Firebase.storage.getReferenceFromUrl(url).delete()
    }
}