package com.helaluddin.noteghor.ui.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.helaluddin.noteghor.data.model.Note
import com.helaluddin.noteghor.databinding.ItemNoteBinding

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onLockedNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback()) {

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.tvTitle.text = Html.fromHtml(note.title, Html.FROM_HTML_MODE_LEGACY)
            binding.tvDescription.text = Html.fromHtml(note.description, Html.FROM_HTML_MODE_LEGACY)

            // Lock & Pin visibility
            binding.ivLock.visibility = if (note.isLocked) View.VISIBLE else View.GONE
            binding.ivPin.visibility = if (note.isPined) View.VISIBLE else View.GONE

            // Dim text if locked
            val alpha = if (note.isLocked) 0.5f else 1f
            binding.tvTitle.alpha = alpha
            binding.tvDescription.alpha = alpha

            // Item click
            binding.root.setOnClickListener {
                if (note.isLocked) onLockedNoteClick(note)
                else onNoteClick(note)
            }

            // Pin toggle
            binding.ivPin.setOnClickListener {
                onPinClick(note)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
