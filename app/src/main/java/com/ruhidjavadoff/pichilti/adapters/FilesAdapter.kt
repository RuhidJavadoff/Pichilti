package com.ruhidjavadoff.pichilti.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ruhidjavadoff.pichilti.FileModel
import com.ruhidjavadoff.pichilti.databinding.ItemFileBinding
import java.util.Locale

class FilesAdapter(
    private val files: List<FileModel>,
    private val onFileClicked: (FileModel) -> Unit
) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024 // 25 MB
        private val FORBIDDEN_EXTENSIONS = setOf("apk", "exe", "zip", "rar", "jar", "sh", "bat")
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileModel) {
            binding.fileNameTextView.text = file.name
            binding.fileInfoTextView.text = android.text.format.Formatter.formatShortFileSize(itemView.context, file.size)

            val extension = file.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
            val isAllowed = file.size <= MAX_FILE_SIZE_BYTES && !FORBIDDEN_EXTENSIONS.contains(extension)

            if (isAllowed) {
                itemView.alpha = 1.0f
                itemView.isEnabled = true
                itemView.setOnClickListener { onFileClicked(file) }
            } else {
                itemView.alpha = 0.5f
                itemView.isEnabled = false
                itemView.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size
}
