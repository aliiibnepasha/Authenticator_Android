package com.husnain.authy.ui.fragment.splash

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
import com.husnain.authy.utls.DelayOption
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.admob.AdUtils
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Runnable
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
        if (Constants.isComingToAuthFromGuest) {
            Constants.isComingToAuthFromGuest = false
            navigate(R.id.action_splashFragment_to_signupFragment)
        }else{
            if (!preferenceManager.isSubscriptionActive()){
                AdUtils.loadInterstitialAd(requireActivity()) { isAdLoaded ->
                    if (isAdLoaded) {
                        init()
                    } else {
                        init()
                    }
                }
            }else{
                binding.root.postDelayed(Runnable {
                    init()
                }, 1500)
            }
        }
        return binding.root
    }

    private fun init() {
        Flags.isComingFromSplash = true
        if (!preferenceManager.isOnboardingFinished()) {
            navigate(R.id.action_splashFragment_to_subscriptionFragmentAuth)
        } else {
            preferenceManager.saveIsToShowSubsScreenAsDialog(true)
            handleUser()
        }
    }

    private fun handleUser() {
        if (shouldShowLockScreen()) {
            preferenceManager.saveLastAppOpenTime()
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
        } else {
            delayAndNavigate()
        }
    }

    private fun delayAndNavigate() {
//        binding.root.postDelayed({
//            if (preferenceManager.isGuestUser()) {
//                goToMainActivity()
//            } else {
//                if (isUserLogedIn()) {
//                    goToMainActivity()
//                } else {
//                    navigate(R.id.action_splashFragment_to_signupFragment)
//                }
//            }
        goToMainActivity()

//        }, 1500)
    }

    private fun checkForBiometricLogin() {
        if (!requireActivity().supportFragmentManager.isStateSaved) {
            showBiometricPrompt(
                activity = requireActivity(),
                onSuccess = {
                    if (preferenceManager.isGuestUser()) {
                        goToMainActivity()
                    } else {
                        if (isUserLogedIn()) {
                            goToMainActivity()
                        } else {
                            navigate(R.id.action_splashFragment_to_signupFragment)
                        }
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

    private fun isUserLogedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun goToMainActivity() {
        startActivity(MainActivity::class.java)
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

    private fun shouldShowLockScreen(): Boolean {
        val selectedDelayOption = preferenceManager.getDelayOption()
        val lastAppOpenTime = preferenceManager.getLastAppOpenTime()
        val currentTime = System.currentTimeMillis()
        if (selectedDelayOption == DelayOption.IMMEDIATELY) {
            return true
        }

        val timeDifference = currentTime - lastAppOpenTime
        val delayInMillis = when (selectedDelayOption) {
            DelayOption.IMMEDIATELY -> return true
            DelayOption.AFTER_15S -> 15 * 1000L
            DelayOption.AFTER_30S -> 30 * 1000L
            DelayOption.AFTER_50S -> 50 * 1000L
            DelayOption.AFTER_1M -> 60 * 1000L
            DelayOption.NEVER -> Long.MAX_VALUE // NEVER means no delay, so always show lock screen
        }

        return timeDifference > delayInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
