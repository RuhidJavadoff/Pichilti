package com.ruhidjavadoff.pichilti.fragments

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.ruhidjavadoff.pichilti.FileModel

class FilesFragment : Fragment() {

    companion object {
        const val REQUEST_KEY = "file_request"
        const val KEY_SELECTED_FILE = "selected_file"
    }

    // Sistem fayl seçicisini açmaq üçün ActivityResultLauncher
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // İstifadəçi fayl seçdi, indi onun məlumatlarını alırıq
            val fileModel = getFileModelFromUri(uri)
            if (fileModel != null) {
                // Nəticəni ChatFragment-ə geri göndəririk
                setFragmentResult(REQUEST_KEY, bundleOf(KEY_SELECTED_FILE to fileModel))
            }
        } else {
            // İstifadəçi fayl seçmədən pəncərəni bağladı
            Log.d("FilesFragment", "Fayl seçilmədi.")
        }
        // Nəticəni göndərdikdən və ya ləğv etdikdən sonra bu fragmenti bağlayırıq
        parentFragmentManager.popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fragment yaradılan kimi fayl seçici pəncərəsini açırıq
        // * işarəsi istənilən fayl növünü seçməyə imkan verir
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    // Bu funksiya seçilmiş faylın URI-sindən onun adını və ölçüsünü alır
    private fun getFileModelFromUri(uri: Uri): FileModel? {
        val cursor: Cursor? = context?.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex != -1) it.getString(nameIndex) else "unknown_file"
                val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L

                return FileModel(uri, name, size)
            }
        }
        return null
    }
}