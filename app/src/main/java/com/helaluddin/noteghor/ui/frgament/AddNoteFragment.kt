package com.helaluddin.noteghor.ui.frgament

import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.helaluddin.noteghor.R
import com.helaluddin.noteghor.data.model.Note
import com.helaluddin.noteghor.data.utils.ThemeHelper
import com.helaluddin.noteghor.databinding.FragmentAddNoteBinding
import com.helaluddin.noteghor.ui.viewModel.AuthViewModel
import com.helaluddin.noteghor.ui.viewModel.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class AddNoteFragment : Fragment() {

    private lateinit var binding: FragmentAddNoteBinding
    private val noteViewModel: NoteViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var currentNote: Note? = null
    private var isLocked = false
    private var notePassword: String? = null

    // track active states
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentNote = arguments?.getSerializable("note") as? Note
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddNoteBinding.inflate(inflater, container, false)

        // Edit mode
        currentNote?.let { note ->
            binding.etTitle.setText(Html.fromHtml(note.title, Html.FROM_HTML_MODE_LEGACY))
            binding.etContent.setText(Html.fromHtml(note.description, Html.FROM_HTML_MODE_LEGACY))
            binding.btnDelete.visibility = View.VISIBLE
            isLocked = note.isLocked
            notePassword = note.password
            updateLockUI()
        } ?: run { binding.btnDelete.visibility = View.GONE }

        loadSavedTheme()

        // Buttons
        binding.btnClose.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveNote.setOnClickListener { saveNote() }
        binding.btnDelete.setOnClickListener { deleteCurrentNote() }

        // Text style buttons
        binding.bold.setOnClickListener {
            isBoldActive = !isBoldActive
            updateStyleButtonUI()
            applyStyle(Typeface.BOLD)
        }

        binding.italic.setOnClickListener {
            isItalicActive = !isItalicActive
            updateStyleButtonUI()
            applyStyle(Typeface.ITALIC)
        }

        binding.underline.setOnClickListener {
            isUnderlineActive = !isUnderlineActive
            updateStyleButtonUI()
            applyUnderline()
        }

        // Lock button
        binding.ivLock.setOnClickListener { showPasswordDialog() }

        return binding.root
    }

    private fun saveNote() {
        val currentUser = authViewModel.getCurrentUser() ?: return

        lifecycleScope.launch {
            try {
                // Get user data
                val userData = noteViewModel.getUserData(currentUser.uid)
                val currentCoins = userData?.coins ?: 0

                // Check if user has enough coins
                if (currentCoins < 5) {
                    Toast.makeText(requireContext(), "Not enough coins! Please buy more.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_addNoteFragment_to_coinFragment)
                    return@launch
                }

                // Deduct 5 coins safely
                val newCoins = currentCoins - 5
                noteViewModel.updateUserCoins(currentUser.uid, newCoins)

                // Prepare note content
                val titleHtml = Html.toHtml(binding.etTitle.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                val descHtml = Html.toHtml(binding.etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

                val note = currentNote?.copy(
                    title = titleHtml,
                    description = descHtml,
                    isLocked = isLocked,
                    password = notePassword
                ) ?: Note(
                    userId = currentUser.uid,
                    title = titleHtml,
                    description = descHtml,
                    isLocked = isLocked,
                    password = notePassword
                )

                // Save note
                noteViewModel.addOrUpdateNote(note, currentUser.uid)

                // Show success
                Toast.makeText(requireContext(), "Note saved! 5 coins deducted.", Toast.LENGTH_SHORT).show()

                // Navigate back
                findNavController().navigateUp()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Something went wrong: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCurrentNote() {
        currentNote?.let { note ->
            val currentUser = authViewModel.getCurrentUser() ?: return
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Delete") { dialog, _ ->
                    noteViewModel.deleteNote(note, currentUser.uid)
                    dialog.dismiss()
                    findNavController().navigateUp()
                }
                .show()
        }
    }

    private fun showPasswordDialog() {
        val passwordInput = EditText(requireContext()).apply { hint = "Enter password" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isLocked) "Change Password" else "Set Password")
            .setView(passwordInput)
            .setPositiveButton("Save") { dialog, _ ->
                val pw = passwordInput.text.toString().trim()
                if (pw.isNotEmpty()) {
                    notePassword = pw
                    isLocked = true
                    Toast.makeText(requireContext(), "Note Locked", Toast.LENGTH_SHORT).show()
                } else {
                    notePassword = null
                    isLocked = false
                    Toast.makeText(requireContext(), "Lock Cancelled", Toast.LENGTH_SHORT).show()
                }
                updateLockUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateLockUI() {
        binding.ivLock.alpha = if (isLocked) 1.0f else 0.3f
    }

    // === Toggle Bold / Italic ===
    private fun applyStyle(style: Int) {
        val editText = binding.etContent
        val titleEditText = binding.etTitle

        toggleStyleForEditText(editText, style)
        toggleStyleForEditText(titleEditText, style)
    }

    private fun toggleStyleForEditText(editText: EditText, style: Int) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start == end) return

        val spans = editText.text.getSpans(start, end, StyleSpan::class.java)
        var removed = false

        spans.forEach { span ->
            if (span.style == style) {
                editText.text.removeSpan(span)
                removed = true
            }
        }

        if (!removed) {
            editText.text.setSpan(
                StyleSpan(style),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // === Toggle Underline ===
    private fun applyUnderline() {
        val editText = binding.etContent
        val titleEditText = binding.etTitle

        toggleUnderlineForEditText(editText)
        toggleUnderlineForEditText(titleEditText)
    }

    private fun toggleUnderlineForEditText(editText: EditText) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start == end) return

        val spans = editText.text.getSpans(start, end, UnderlineSpan::class.java)
        var removed = false

        spans.forEach { span ->
            editText.text.removeSpan(span)
            removed = true
        }

        if (!removed) {
            editText.text.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // === Button Highlight ===
    private fun updateStyleButtonUI() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.purple_200)
        val normalColor = ContextCompat.getColor(requireContext(), R.color.transparent)

        binding.bold.setBackgroundColor(if (isBoldActive) activeColor else normalColor)
        binding.italic.setBackgroundColor(if (isItalicActive) activeColor else normalColor)
        binding.underline.setBackgroundColor(if (isUnderlineActive) activeColor else normalColor)
    }

    // === Theme Management ===
    private fun loadSavedTheme() {
        val savedTheme = ThemeHelper.getSavedTheme(requireContext())
        applyDynamicTheme(savedTheme)
    }

    private fun applyDynamicTheme(themeName: String) {
        when (themeName) {
            "Sunset" -> {
                binding.root.setBackgroundColor(resources.getColor(R.color.sunset_bg))
                binding.btnDelete.setBackgroundColor(resources.getColor(R.color.sunset_primary))
                binding.btnSaveNote.setBackgroundColor(resources.getColor(R.color.sunset_primary))
                binding.etTitle.setTextColor(resources.getColor(R.color.black))
                binding.etContent.setTextColor(resources.getColor(R.color.black))
            }
            "Ocean" -> {
                binding.root.setBackgroundColor(resources.getColor(R.color.ocean_bg))
                binding.btnDelete.setBackgroundColor(resources.getColor(R.color.ocean_primary))
                binding.btnSaveNote.setBackgroundColor(resources.getColor(R.color.ocean_primary))
                binding.etTitle.setTextColor(resources.getColor(R.color.black))
                binding.etContent.setTextColor(resources.getColor(R.color.black))
            }
            "Forest" -> {
                binding.root.setBackgroundColor(resources.getColor(R.color.forest_bg))
                binding.btnDelete.setBackgroundColor(resources.getColor(R.color.forest_primary))
                binding.btnSaveNote.setBackgroundColor(resources.getColor(R.color.forest_primary))
                binding.etTitle.setTextColor(resources.getColor(R.color.white))
                binding.etContent.setTextColor(resources.getColor(R.color.white))
            }
            "Midnight" -> {
                binding.root.setBackgroundColor(resources.getColor(R.color.midnight_bg))
                binding.btnDelete.setBackgroundColor(resources.getColor(R.color.midnight_primary))
                binding.btnSaveNote.setBackgroundColor(resources.getColor(R.color.midnight_primary))
                binding.etTitle.setTextColor(resources.getColor(R.color.midnight_text))
                binding.etContent.setTextColor(resources.getColor(R.color.midnight_text))
            }
            "Coral" -> {
                binding.root.setBackgroundColor(resources.getColor(R.color.coral_bg))
                binding.btnDelete.setBackgroundColor(resources.getColor(R.color.coral_primary))
                binding.btnSaveNote.setBackgroundColor(resources.getColor(R.color.coral_primary))
                binding.etTitle.setTextColor(resources.getColor(R.color.coral_text))
                binding.etContent.setTextColor(resources.getColor(R.color.coral_text))
            }
        }
    }
}
