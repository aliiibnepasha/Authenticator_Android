package com.husnain.authy.ui.fragment.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSigninBinding
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack

class SigninFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.tvSignup.setOnClickListener {
            popBack()
        }
        binding.tvForgotPassword.setOnClickListener {
            navigate(R.id.action_signinFragment_to_forgotPasswordFragment)
        }
        binding.btnLogin.setOnClickListener {
            if (validateFields()){
                showCustomToast("Loged in successfully")
            }
        }
        binding.googleButton.setOnClickListener {

        }
        binding.facebookButton.setOnClickListener {

        }
    }

    private fun validateFields(): Boolean {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPass.text.toString().trim()

        if (email.isEmpty()) {
            showCustomToast("Email cannot be empty")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showCustomToast("Invalid email")
            return false
        }

        if (password.isEmpty()) {
            showCustomToast("Password cannot be empty")
            return false
        }

        if (password.length < 6) {
            showCustomToast("Password must be at least 6 characters long")
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}