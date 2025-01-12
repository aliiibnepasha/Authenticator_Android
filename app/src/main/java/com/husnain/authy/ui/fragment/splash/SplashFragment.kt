package com.husnain.authy.ui.fragment.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSplashBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.navigate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        makeFragmentFullScreen()
        init()
        return binding.root
    }

    private fun init() {
        if (Constants.isComingFromLogout) {
            Constants.isComingFromLogout = false
            navigate(R.id.action_splashFragment_to_signupFragment)
        } else {
            handleUser()
        }
    }

    private fun handleUser() {
        when {
            preferenceManager.isBiometricLockEnabled() -> {
                checkForBiometricLogin()
            }

            !preferenceManager.getPin().isNullOrEmpty() -> {
                navigateToPinSetup()
            }

            else -> {
                delayAndNavigate()
            }
        }
    }

    private fun delayAndNavigate() {
        binding.root.postDelayed({
            if (isUserLogedIn()){
                goToMainActivity()
            }else{
                navigate(R.id.action_splashFragment_to_signupFragment)
            }
        }, 500)
    }

    private fun checkForBiometricLogin() {
        if (!requireActivity().supportFragmentManager.isStateSaved) {
            showBiometricPrompt(
                activity = requireActivity(),
                onSuccess = {
                    if (isUserLogedIn()) {
                        goToMainActivity()
                    } else {
                        navigate(R.id.action_splashFragment_to_signupFragment)
                    }
                },
                onFailure = {
                    showCustomToast("Something went wrong!")
                }
            )
        }
    }

    private fun navigateToPinSetup() {
        val bundle = Bundle().apply {
            putBoolean(Constants.SIGNUPTOPIN_KEY, true)
        }
        navigate(R.id.action_splashFragment_to_setPinFragment2, bundle)
    }

    private fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
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

    private fun isUserLogedIn():Boolean{
        return auth.currentUser != null
    }

    private fun goToMainActivity(){
        startActivity(Intent(requireActivity(), MainActivity::class.java))
        requireActivity().finish()
    }

    private fun makeFragmentFullScreen() {
        requireActivity().window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the flags to restore the default UI behavior
        requireActivity().window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE // Reset the system UI visibility
        }
        _binding = null
    }
}
