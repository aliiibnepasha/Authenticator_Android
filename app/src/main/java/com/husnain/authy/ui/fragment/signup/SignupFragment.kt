package com.husnain.authy.ui.fragment.signup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSignupBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        startActivity(MainActivity::class.java)
        inIt()
        return binding.root
    }

    private fun inIt() {
        checkForOnBoarding()
        setOnClickListener()
    }

    private fun checkForOnBoarding() {
        if (!preferenceManager.isOnboardingFinished()){
            navigate(R.id.action_signupFragment_to_onboardingFragment)
        }
    }

    private fun setOnClickListener() {
        binding.tvSignin.setOnClickListener {
            navigate(R.id.action_signupFragment_to_signinFragment)
        }
        binding.btnCreateAccount.setOnClickListener {
            if (!validateFields()){
                startActivity(MainActivity::class.java)
            }
        }
        binding.googleButton.setOnClickListener {

        }
        binding.facebookButton.setOnClickListener {

        }
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}