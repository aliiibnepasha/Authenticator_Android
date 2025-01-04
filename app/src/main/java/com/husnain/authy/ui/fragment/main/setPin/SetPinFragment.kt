package com.husnain.authy.ui.fragment.main.setPin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSetPinBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SetPinFragment : Fragment() {
    private var _binding: FragmentSetPinBinding? = null
    private val binding get() = _binding!!
    private var enteredPin = ""
    private lateinit var pinDots: List<ImageView>
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private var isFromSignupToPin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetPinBinding.inflate(inflater, container, false)
        isFromSignupToPin = arguments?.getBoolean(Constants.SIGNUPTOPIN_KEY, false) ?: false
        inIt()
        return binding.root
    }

    private fun inIt() {
        inItUi()
        setOnClickListener()
        handleSystemBackPressed()
    }

    private fun inItUi() {
        if (isFromSignupToPin) {
            binding.tvToolBarTitle.text = "Enter pin"
            binding.tvSetPinTitle.text = "Enter your pin"
        } else {
            binding.tvToolBarTitle.text = "Set pin"
            binding.tvSetPinTitle.text = "Set your pin"
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            goBack()
        }

        pinDots = listOf(
            binding.pinDot1,
            binding.pinDot2,
            binding.pinDot3,
            binding.pinDot4
        )


        binding.btn1.setOnClickListener { onNumberClicked("1") }
        binding.btn2.setOnClickListener { onNumberClicked("2") }
        binding.btn3.setOnClickListener { onNumberClicked("3") }
        binding.btn4.setOnClickListener { onNumberClicked("4") }
        binding.btn5.setOnClickListener { onNumberClicked("5") }
        binding.btn6.setOnClickListener { onNumberClicked("6") }
        binding.btn7.setOnClickListener { onNumberClicked("7") }
        binding.btn8.setOnClickListener { onNumberClicked("8") }
        binding.btn9.setOnClickListener { onNumberClicked("9") }
        binding.btn0.setOnClickListener { onNumberClicked("0") }
        binding.btnCross.setOnClickListener { onCrossClicked() }
    }

    private fun onNumberClicked(number: String) {
        if (enteredPin.length < 4) {
            enteredPin += number
            updatePinDots()
            if (enteredPin.length == 4) {
                onPinEntered(enteredPin)
            }
        }
    }

    private fun onCrossClicked() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        for (i in pinDots.indices) {
            if (i < enteredPin.length) {
                pinDots[i].setImageResource(R.drawable.ic_pin_dot_active)
            } else {
                pinDots[i].setImageResource(R.drawable.ic_pin_dot_inactive)
            }
        }
    }

    private fun onPinEntered(pin: String) {
        if (isFromSignupToPin) {
            if (pin == preferenceManager.getPin()) {
                startActivity(MainActivity::class.java)
                requireActivity().finish()
            } else {
                handleWrongPin()
            }
        } else {
            preferenceManager.saveBiometricLock(false)
            preferenceManager.savePin(pin)
            popBack()
        }
    }

    private fun handleWrongPin(){
        showCustomToast("Wrong pin!")
        //Clear active state of pinDots and clear entered pin
        for (i in pinDots.indices) {
            pinDots[i].setImageResource(R.drawable.ic_pin_dot_inactive)
            enteredPin = ""
        }
    }

    private fun handleSystemBackPressed() {
        goBackPressed {
            goBack()
        }
    }

    private fun goBack() {
        if (isFromSignupToPin) {
            requireActivity().finishAffinity()
        } else {
            popBack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}