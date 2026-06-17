package com.example.atvidadedm.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.atvidadedm.data.local.TripPhotoDao
import com.example.atvidadedm.data.local.TripPhotoEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

data class PhotoCaptureTarget(
    val uri: Uri,
    val file: File
)

class TripPhotoRepository(
    private val tripPhotoDao: TripPhotoDao,
    private val context: Context
) {
    fun observePhotosByTripId(tripId: Long): Flow<List<TripPhotoEntity>> {
        return tripPhotoDao.getPhotosByTripId(tripId)
    }

    suspend fun addGalleryPhoto(tripId: Long, sourceUri: Uri): Long {
        val localUri = copyGalleryPhotoToLocalStorage(tripId, sourceUri)
        return insertPhoto(tripId, localUri)
    }

    suspend fun addCapturedPhoto(tripId: Long, photoUri: Uri): Long {
        return insertPhoto(tripId, photoUri)
    }

    fun createCameraCaptureTarget(tripId: Long): PhotoCaptureTarget {
        val photoDir = File(context.filesDir, "trip_photos/trip_$tripId").apply {
            mkdirs()
        }

        val photoFile = File(photoDir, "photo_${System.currentTimeMillis()}.jpg")
        if (!photoFile.exists()) {
            photoFile.createNewFile()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, photoFile)
        return PhotoCaptureTarget(uri = uri, file = photoFile)
    }

    fun deleteCaptureTarget(target: PhotoCaptureTarget) {
        if (target.file.exists()) {
            target.file.delete()
        }
    }

    private suspend fun insertPhoto(tripId: Long, uri: Uri): Long {
        return tripPhotoDao.insert(
            TripPhotoEntity(
                tripId = tripId,
                photoUri = uri.toString()
            )
        )
    }

    private fun copyGalleryPhotoToLocalStorage(tripId: Long, sourceUri: Uri): Uri {
        val destinationTarget = createCameraCaptureTarget(tripId)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationTarget.file).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Nao foi possivel ler a foto selecionada.")

        return destinationTarget.uri
    }
}
