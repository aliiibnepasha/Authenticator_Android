package com.husnain.authy.ui.fragment.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelUser
import com.husnain.authy.databinding.FragmentSigninBinding
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.getTextFromEdit
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupKeyboardDismissListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SigninFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!
    private val vmAuth: VmAuth by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardDismissListener(view)
    }

    private fun inIt() {
        setOnClickListener()
        setUpObserver()
        goBackPressed {
            startMainActivityFromGuestToLogin()
        }
    }

    private fun setOnClickListener() {
        binding.tvSignup.setOnClickListener {
            Constants.isComingToAuthFromGuestToSignIn = false
            popBack()
        }
        binding.tvForgotPassword.setOnClickListener {
            navigate(R.id.action_signinFragment_to_forgotPasswordFragment)
        }
        binding.btnLogin.setOnClickListener {
            if (validateFields()){
                requestLogin()
            }
        }
        binding.googleButton.setOnClickListener {
            vmAuth.continueWithGoogle(requireActivity())
        }
    }

    private fun setUpObserver() {
        vmAuth.loginState.observe(viewLifecycleOwner, Observer { state ->
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

    private fun requestLogin() {
        vmAuth.loginWithEmailPass(
            ModelUser(
            binding.edtEmail.getTextFromEdit(),
            binding.edtPass.getTextFromEdit(),
        )
        )
    }

    private fun validateFields(): Boolean {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPass.text.toString().trim()

        if (email.isEmpty()) {
            showCustomToast(getString(R.string.email_cannot_be_empty))
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showCustomToast(getString(R.string.invalid_email))
            return false
        }

        if (password.isEmpty()) {
            showCustomToast(getString(R.string.password_cannot_be_empty))
            return false
        }

        if (password.length < 6) {
            showCustomToast(getString(R.string.password_must_be_at_least_6_characters_long))
            return false
        }

        return true
    }

    private fun startMainActivityFromGuestToLogin(){
        Constants.isComingToAuthFromGuestToSignIn = false
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoader(){
        binding.loadingView.start(viewToHideIf = binding.tvLogin)
    }

    private fun stopLoader(){
        binding.loadingView.stop(binding.tvLogin)
    }


    private fun showLoaderForGoogle(){
        binding.loadingViewGoogle.start(viewToHideIf = binding.tvGoogle)
    }

    private fun stopLoaderForGoogle(){
        binding.loadingViewGoogle.stop(binding.tvGoogle)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}