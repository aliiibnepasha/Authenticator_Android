package com.husnain.authy.ui.fragment.main.applock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentAppLockBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.CustomToast.showCustomToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppLockFragment : Fragment() {
    private var _binding: FragmentAppLockBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager
    private var isNavigatingToSetPin = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppLockBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        inItUi()
        setOnClickListener()
    }

    private fun inItUi() {
        binding.switchSetPinLock.isChecked = !preferenceManager.getPin().isNullOrEmpty()
        binding.switchBiometricLocak.isChecked = preferenceManager.isBiometricLockEnabled()
    }

    private fun setOnClickListener() {
        binding.switchSetPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                handleSwitchOn()
            } else {
                handleSwitchOff()
            }
        }

        binding.switchBiometricLocak.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                handleBiometricChecked()
            } else {
                preferenceManager.saveBiometricLock(false)
            }
        }
    }

    private fun handleBiometricChecked() {
        //This condition prevents the biometric switch to turn off if user
        //came back from pin after not setting pin but the biometric was on before
        if (isNavigatingToSetPin && !preferenceManager.isBiometricLockEnabled()) {
            binding.switchBiometricLocak.isChecked = false
        } else {
            if (isBiometricAvailable()) {
                preferenceManager.saveBiometricLock(true)

                //if user on biometric turn of pin lock and clear it's data
                if (binding.switchSetPinLock.isChecked) {
                    binding.switchSetPinLock.isChecked = false
                    preferenceManager.savePin("")
                }
            } else {
                binding.switchBiometricLocak.isChecked = false
            }
        }
    }


    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showCustomToast("Biometric is not supported on your device")
                false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showCustomToast("Biometric is not supported on your device")
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showCustomToast("No biometric enrolled")
                false
            }

            else -> false
        }
    }

    private fun handleSwitchOn() {
        if (preferenceManager.getPin().isNullOrEmpty()) {
            if (!isNavigatingToSetPin) {
                isNavigatingToSetPin = true
                findNavController().navigate(R.id.action_appLockFragment_to_setPinFragment)
            }
        } else {
            //do nothing here
        }
    }

    private fun handleSwitchOff() {
        preferenceManager.savePin("")
    }

    override fun onResume() {
        super.onResume()
        binding.switchSetPinLock.isChecked = !preferenceManager.getPin().isNullOrEmpty()
        binding.switchBiometricLocak.isChecked = preferenceManager.isBiometricLockEnabled()
        isNavigatingToSetPin = false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}