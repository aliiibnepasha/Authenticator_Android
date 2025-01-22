package com.husnain.authy.ui.fragment.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                showCustomToast("Name cannot be empty")
                false
            }

            email.isEmpty() -> {
                showCustomToast("Email cannot be empty")
                false
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showCustomToast("Invalid email")
                false
            }

            password.isEmpty() -> {
                showCustomToast("Password cannot be empty")
                false
            }

            password.length < 6 -> {
                showCustomToast("Password must be at least 6 characters long")
                false
            }

            else -> true
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}