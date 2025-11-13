@file:Suppress("DEPRECATION")

package com.helaluddin.noteghor.ui.frgament

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.helaluddin.noteghor.R
import com.helaluddin.noteghor.data.model.Note
import com.helaluddin.noteghor.data.utils.ThemeHelper
import com.helaluddin.noteghor.databinding.FragmentDashboardBinding
import com.helaluddin.noteghor.ui.adapter.NoteAdapter
import com.helaluddin.noteghor.ui.viewModel.AuthViewModel
import com.helaluddin.noteghor.ui.viewModel.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        loadSavedTheme()
        setupGoogleSignIn()
        setupRecyclerView()
        setupFab()
        setupNavigationDrawer()

        val user = authViewModel.getCurrentUser()

        if (user == null) {
            updateDrawerHeader(null, null)
            binding.coin.text = "💰 0"
            Toast.makeText(requireContext(), "Welcome Guest!", Toast.LENGTH_SHORT).show()
        } else {
            observeUserCoins(user.uid)
            updateDrawerHeader(user.displayName, user.email)

            lifecycleScope.launch {
                val userData = noteViewModel.getUserData(user.uid)
                if (userData == null) {
                    // Remove these lines
                    /*
                    noteViewModel.giveFirstTimeFreeCoins(user.uid)
                    Toast.makeText(requireContext(), "20 free coins added!", Toast.LENGTH_SHORT).show()
                    */
                }
            }
        }
        return binding.root
    }

    // -------------------------------
    // Google Sign-In
    // -------------------------------
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    // -------------------------------
    // RecyclerView
    // -------------------------------
    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note -> openNoteFragment(note) },
            onLockedNoteClick = { note -> showPasswordDialog(note) },
            onPinClick = { note -> togglePin(note) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val currentUser = authViewModel.getCurrentUser()
        if (currentUser != null) {
            val userId = currentUser.uid
            lifecycleScope.launch {
                noteViewModel.getNotesFlow(userId).collect { notes ->
                    val sortedNotes = notes.sortedByDescending { it.isPined }
                    adapter.submitList(sortedNotes)
                    binding.textEmptyState.visibility =
                        if (notes.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun togglePin(note: Note) {
        val currentUser = authViewModel.getCurrentUser() ?: return
        val updatedNote = note.copy(isPined = !note.isPined)
        noteViewModel.addOrUpdateNote(updatedNote, currentUser.uid)
        Toast.makeText(
            requireContext(),
            if (updatedNote.isPined) "Note pinned" else "Note unpinned",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addNoteFragment)
        }
    }

    // -------------------------------
    // Navigation Drawer
    // -------------------------------
    @SuppressLint("UseKtx")
    private fun setupNavigationDrawer() {
        binding.navDrawer.setOnClickListener {
            val drawerLayout = binding.drawerLayout
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START)
            else
                drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_points_store -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_coinFragment)
                    true
                }

                R.id.nav_privacy_policy -> {
                    openUrl("https://sites.google.com/view/noteghor-privacy-policy-info/home")
                    true
                }

                R.id.nav_terms_of_service -> {
                    openUrl("https://sites.google.com/view/noteghor-terms-and-conditions/home")
                    true
                }

                R.id.nav_support_contact -> {
                    showThemeSelection()
                    true
                }

                R.id.nav_delete_account -> {
                    showDeleteAccountDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showThemeSelection() {
        val themes = arrayOf("Sunset", "Ocean", "Forest", "Midnight", "Coral")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Theme")
            .setItems(themes) { dialog, which ->
                val selectedTheme = themes[which]
                ThemeHelper.applyTheme(selectedTheme, requireContext())
                applyDynamicTheme(selectedTheme)
                dialog.dismiss()
            }.show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout Account")
            .setMessage("Are you sure you want to logout your account?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                authViewModel.logout()
                googleSignInClient.signOut().addOnCompleteListener {
                    findNavController().navigate(R.id.action_dashboardFragment_to_introFragment)
                }
                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // -------------------------------
    // Coins
    // -------------------------------
    @SuppressLint("SetTextI18n")
    private fun observeUserCoins(uid: String) {
        lifecycleScope.launch {
            noteViewModel.getUserDataFlow(uid).collect { userData ->
                val coins = userData?.coins ?: 0
                binding.coin.text = "💰 $coins"
            }
        }
    }

    // -------------------------------
    // Notes
    // -------------------------------
    private fun openNoteFragment(note: Note) {
        val bundle = Bundle().apply { putSerializable("note", note) }
        findNavController().navigate(R.id.action_dashboardFragment_to_addNoteFragment, bundle)
    }

    private fun showPasswordDialog(note: Note) {
        val passwordInput = EditText(requireContext()).apply { hint = "Enter password" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Password")
            .setView(passwordInput)
            .setPositiveButton("Unlock") { dialog, _ ->
                if (passwordInput.text.toString() == note.password) openNoteFragment(note)
                else Toast.makeText(requireContext(), "Wrong Password", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // -------------------------------
    // Theme
    // -------------------------------
    private fun loadSavedTheme() {
        val savedTheme = ThemeHelper.getSavedTheme(requireContext())
        applyDynamicTheme(savedTheme)
    }

    private fun applyDynamicTheme(themeName: String) {
        val bgColor: Int
        val fabColor: Int

        when (themeName) {
            "Sunset" -> { bgColor = R.color.sunset_bg; fabColor = R.color.sunset_primary }
            "Ocean" -> { bgColor = R.color.ocean_bg; fabColor = R.color.ocean_primary }
            "Forest" -> { bgColor = R.color.forest_bg; fabColor = R.color.forest_primary }
            "Midnight" -> { bgColor = R.color.midnight_bg; fabColor = R.color.midnight_primary }
            "Coral" -> { bgColor = R.color.coral_bg; fabColor = R.color.coral_primary }
            else -> { bgColor = R.color.white; fabColor = R.color.purple_500 }
        }

        binding.root.setBackgroundColor(resources.getColor(bgColor))
        binding.recyclerView.setBackgroundColor(resources.getColor(bgColor))
        binding.fabAddNote.setBackgroundColor(resources.getColor(fabColor))
    }

    @SuppressLint("SetTextI18n")
    private fun updateDrawerHeader(name: String?, email: String?) {
        val headerView = binding.navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tv_user_name)
        val tvEmail = headerView.findViewById<TextView>(R.id.tv_user_email)

        if (name.isNullOrEmpty() && email.isNullOrEmpty()) {
            tvName.text = "Guest"
            tvEmail.text = "guest@example.com"
        } else {
            tvName.text = name ?: "User"
            tvEmail.text = email ?: ""
        }
    }


    override fun onResume() {
        super.onResume()
        authViewModel.getCurrentUser()?.uid?.let { observeUserCoins(it) }
    }
}
