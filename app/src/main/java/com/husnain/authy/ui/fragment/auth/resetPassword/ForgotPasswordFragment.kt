package com.husnain.authy.ui.fragment.auth.resetPassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentForgotPasswordBinding
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupKeyboardDismissListener

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardDismissListener(view)
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.backButton.setOnClickListener {
            popBack()
        }
        binding.continueButton.setOnClickListener {
            if (isValidInput()) {
                showLoader()
                sendPasswordResetEmail(binding.emailEditText.text.toString().trim())
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                stopLoader()
                if (task.isSuccessful) {
                    binding.emailEditText.setText("")
                    showCustomToast(getString(R.string.password_reset_email_sent_successfully))
                } else {
                    showCustomToast(getString(R.string.failed_to_send_password_reset_email_please_try_again))
                }
            }
    }


    private fun isValidInput(): Boolean {
        val email = binding.emailEditText.text.toString().trim()

        return when {
            email.isEmpty() -> {
                showCustomToast(getString(R.string.email_field_cannot_be_empty))
                false
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showCustomToast(getString(R.string.please_enter_a_valid_email_address))
                false
            }

            else -> true
        }
    }


    private fun showLoader() {
        binding.loadingView.start(viewToHideIf = binding.tvContinue)
    }

    private fun stopLoader() {
        binding.loadingView.stop(binding.tvContinue)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
