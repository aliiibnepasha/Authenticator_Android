package com.husnain.authy.ui.fragment.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.data.ModelUser
import com.husnain.authy.databinding.FragmentSignupBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.GoogleSigninUtils
import com.husnain.authy.utls.LoadingView
import com.husnain.authy.utls.getTextFromEdit
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var auth: FirebaseAuth
    private val vmAuth: VmAuth by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(MainActivity::class.java)
        }
        inIt()
        return binding.root
    }

    private fun inIt() {
        checkForPin()
        checkForBiometricLogin()
        checkForOnBoarding()
        setUpObserver()
        setOnClickListener()

    }

    private fun setOnClickListener() {
        binding.tvSignin.setOnClickListener {
            navigate(R.id.action_signupFragment_to_signinFragment)
        }
        binding.btnCreateAccount.setOnClickListener {
            if (validateFields()){
                requestCreateAccount()
            }
        }
        binding.googleButton.setOnClickListener {
            GoogleSigninUtils.doGoogleSingin(requireActivity(),lifecycleScope)
        }
    }

    private fun checkForPin() {
        if (!preferenceManager.getPin().isNullOrEmpty()){
            val bundle = Bundle().apply {
                putBoolean(Constants.SIGNUPTOPIN_KEY,true)
            }
            navigate(R.id.action_signupFragment_to_setPinFragment2,bundle)
        }
    }

    private fun checkForOnBoarding() {
        if (!preferenceManager.isOnboardingFinished()){
            navigate(R.id.action_signupFragment_to_onboardingFragment)
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
                    startActivity(MainActivity::class.java)
                    requireActivity().finish()
                }

                is DataState.Error -> {
                    stopLoader()
                    showCustomToast(state.message)
                }
            }
        })

        GoogleSigninUtils.signUpStatusWithGoogle.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                    showLoader()
                }

                is DataState.Success -> {
                    stopLoader()
                    startActivity(MainActivity::class.java)
                    requireActivity().finish()
                }

                is DataState.Error -> {
                    stopLoader()
                    showCustomToast(state.message)
                }
            }
        })
    }

    private fun requestCreateAccount() {
        vmAuth.signUpWithEmailPass(ModelUser(
            binding.edtEmail.getTextFromEdit(),
            binding.edtPass.getTextFromEdit(),
            binding.edtName.getTextFromEdit()
        ))
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


    private fun checkForBiometricLogin() {
        if (preferenceManager.isBiometricLockEnabled()) {
            if (!requireActivity().supportFragmentManager.isStateSaved) {
                showBiometricPrompt(
                    activity = requireActivity(),
                    onSuccess = {
                        startActivity(Intent(requireActivity(), MainActivity::class.java))
                        requireActivity().finish()
                    },
                    onFailure = {
                        showCustomToast("Something went wrong!")
                    }
                )
            }
        }
    }

    private fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d("BiometricAuth", "Error: $errString")
                onFailure()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d("BiometricAuth", "Authentication succeeded!")
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("BiometricAuth", "Authentication failed.")
                onFailure()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credentials")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    private fun showLoader(){
        binding.loadingView.start(viewToHideIf = binding.tvCreateAccount)
    }

    private fun stopLoader(){
        binding.loadingView.stop(binding.tvCreateAccount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}