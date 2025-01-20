package com.husnain.authy.ui.fragment.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelUser
import com.husnain.authy.databinding.FragmentSigninBinding
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.getTextFromEdit
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupKeyboardDismissListener
import com.husnain.authy.utls.startActivity
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
                    startActivity(MainActivity::class.java)
                    requireActivity().finish()
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
                    startActivity(MainActivity::class.java)
                    requireActivity().finish()
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