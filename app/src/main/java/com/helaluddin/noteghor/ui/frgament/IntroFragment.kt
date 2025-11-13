@file:Suppress("DEPRECATION")

package com.helaluddin.noteghor.ui.frgament

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.helaluddin.noteghor.data.utils.AuthResult
import com.helaluddin.noteghor.R
import com.helaluddin.noteghor.databinding.FragmentIntroBinding
import com.helaluddin.noteghor.ui.viewModel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroFragment : Fragment() {

    private var _binding: FragmentIntroBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(Exception::class.java)
                    if (account != null) {
                        firebaseAuthWithGoogle(account)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Google sign-in failed",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Google sign-in error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "Google sign-in cancelled", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroBinding.inflate(inflater, container, false)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate automatically if already logged in
        if (authViewModel.getCurrentUser() != null) {
            if (findNavController().currentDestination?.id == R.id.introFragment) {
                findNavController().navigate(R.id.action_introFragment_to_dashboardFragment)
            }
        }


        binding.guestBtn.setOnClickListener { authViewModel.signInGuest() }

        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> binding.progressBar.visibility = View.VISIBLE
                is AuthResult.Success -> handleAuthSuccess(
                    result.user.displayName
                )

                is AuthResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Button listeners
        binding.googleSignInBtn.setOnClickListener { signInWithGoogle() }
        binding.guestBtn.setOnClickListener { authViewModel.signInGuest() }

        // Observe authentication result
        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> binding.progressBar.visibility = View.VISIBLE
                is AuthResult.Success -> handleAuthSuccess(
                    result.user.displayName
                )

                is AuthResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        authViewModel.signInWithGoogle(credential)
    }

    private fun handleAuthSuccess(name: String?) {
        binding.progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), "Welcome, ${name ?: "Guest"}", Toast.LENGTH_SHORT).show()
        if (findNavController().currentDestination?.id == R.id.introFragment) {
            findNavController().navigate(R.id.action_introFragment_to_dashboardFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
