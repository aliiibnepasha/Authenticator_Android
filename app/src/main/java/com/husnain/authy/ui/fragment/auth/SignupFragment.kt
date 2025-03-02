package com.husnain.authy.ui.fragment.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelUser
import com.husnain.authy.databinding.FragmentSignupBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.getTextFromEdit
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.setupKeyboardDismissListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var auth: FirebaseAuth
    private val vmAuth: VmAuth by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardDismissListener(view)
    }

    private fun inIt() {
        setUpObserver()
        setOnClickListener()
        handleSystemNavBackPressed()
    }

    private fun setOnClickListener() {
        if (Constants.isComingToAuthFromGuestToSignIn) {
            navigate(R.id.action_signupFragment_to_signinFragment)
        }

        binding.tvTermsOfService.setOnClickListener {
            openLink("https://sites.google.com/view/authenticatorapp-termofuse/home")
        }
        binding.tvPrivacyPolicy.setOnClickListener {
            openLink("https://sites.google.com/view/authenticatorapp-privacypolicy/home")
        }

        binding.tvSignin.setOnClickListener {
            navigate(R.id.action_signupFragment_to_signinFragment)
        }
        binding.btnCreateAccount.setOnClickListener {
            if (validateFields()) {
                requestCreateAccount()
            }
        }
        binding.googleButton.setOnClickListener {
            vmAuth.continueWithGoogle(requireActivity())
        }
    }

    private fun setUpObserver() {
        vmAuth.signUpState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                    showLoader()
                }

                is DataState.Success -> {
                    stopLoader()
                    startMainActivityFromGuestToLogin()
                }

                is DataState.Error -> {
                    stopLoader()
                    showCustomToast(state.message)
                }
            }
        })

        vmAuth.googleLoginState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                    showLoaderForGoogle()
                }

                is DataState.Success -> {
                    stopLoaderForGoogle()
                    startMainActivityFromGuestToLogin()
                }

                is DataState.Error -> {
                    stopLoaderForGoogle()
                    showCustomToast(state.message)
                }
            }
        })
    }

    private fun startMainActivityFromGuestToLogin() {
        Constants.isComingToAuthFromGuestToSignIn = false
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun requestCreateAccount() {
        vmAuth.signUpWithEmailPass(
            ModelUser(
                binding.edtEmail.getTextFromEdit(),
                binding.edtPass.getTextFromEdit(),
                binding.edtName.getTextFromEdit()
            )
        )
    }

    private fun validateFields(): Boolean {
        val name = binding.edtName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPass.text.toString().trim()

        return when {
            name.isEmpty() -> {
                showCustomToast(getString(R.string.name_cannot_be_empty))
                false
            }

            email.isEmpty() -> {
                showCustomToast(getString(R.string.email_cannot_be_empty))
                false
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showCustomToast(getString(R.string.invalid_email))
                false
            }

            password.isEmpty() -> {
                showCustomToast(getString(R.string.password_cannot_be_empty))
                false
            }

            password.length < 6 -> {
                showCustomToast(getString(R.string.password_must_be_at_least_6_characters_long))
                false
            }

            else -> {
                true
            }
        }
    }

    private fun showLoader() {
        binding.loadingView.start(viewToHideIf = binding.tvCreateAccount)
    }

    private fun stopLoader() {
        binding.loadingView.stop(binding.tvCreateAccount)
    }

    private fun showLoaderForGoogle() {
        binding.loadingViewGoogle.start(viewToHideIf = binding.tvGoogle)
    }

    private fun stopLoaderForGoogle() {
        binding.loadingViewGoogle.stop(binding.tvGoogle)
    }


    private fun handleSystemNavBackPressed() {
        goBackPressed {
            startMainActivityFromGuestToLogin()
        }
    }

    private fun openLink(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            // If Chrome Custom Tabs is not available, open in default browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}