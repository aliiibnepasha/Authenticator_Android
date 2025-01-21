package com.husnain.authy.ui.fragment.main.applock

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.widget.PopupMenu
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentAppLockBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DelayOption
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.isBiometricSupported
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppLockFragment : Fragment() {
    private var _binding: FragmentAppLockBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager
    private val delayOptions = DelayOption.entries.toTypedArray()
    private var isNavigatingToSetPin = false
    private lateinit var autoCompleteTextView: AutoCompleteTextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppLockBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
    private fun inIt() {
        inItUi()
        setOnClickListener()
    }

    private fun inItUi() {
        binding.autoCompleteTextView.text = preferenceManager.getDelayOption().getDisplayText(requireContext())
        binding.switchSetPinLock.isChecked = !preferenceManager.getPin().isNullOrEmpty()
        binding.switchBiometricLocak.isChecked = preferenceManager.isBiometricLockEnabled()
        binding.cardViewBiometricLock.apply { if (isBiometricSupported()) visible() else gone() }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.dropdownLayout.setOnClickListener {
            showCustomDropdownMenu(it)
        }

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

    private fun showCustomDropdownMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        val menu = popupMenu.menu
        delayOptions.forEachIndexed { index, option ->
            menu.add(0, index, 0, option.getDisplayText(requireContext()))
        }

        popupMenu.gravity = Gravity.END;
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            preferenceManager.saveDelayOption(delayOptions[item.itemId])
            preferenceManager.saveLastAppOpenTime()
            binding.autoCompleteTextView.text = delayOptions[item.itemId].getDisplayText(requireContext())
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}