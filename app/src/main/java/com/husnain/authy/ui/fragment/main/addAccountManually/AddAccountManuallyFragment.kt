package com.husnain.authy.ui.fragment.main.addAccountManually

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentAddAccountManuallyBinding
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack

class AddAccountManuallyFragment : Fragment() {
    private var _binding: FragmentAddAccountManuallyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddAccountManuallyBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnAddAccount.setOnClickListener {
            navigate(R.id.action_addAccountManuallyFragment_to_homeFragment)
        }
    }
    private fun validateAccountFields(): Boolean {
        val accountName = binding.edtAccountName.text.toString().trim()
        val privateKey = binding.edtPrivateKey.text.toString().trim()

        if (accountName.isEmpty()) {
            showCustomToast("Account name cannot be empty")
            return false
        }

        if (privateKey.isEmpty()) {
            showCustomToast("Private key cannot be empty")
            return false
        }

        if (!isValidSecretKey(privateKey)) {
            showCustomToast("Invalid private key format")
            return false
        }

        return true
    }

    private fun isValidSecretKey(secret: String): Boolean {
        return isValidBase32(secret) || isValidBase64(secret)
    }

    // Validate Base32 format
    private fun isValidBase32(secret: String): Boolean {
        val base32Regex = "^[A-Z2-7]*$".toRegex()  // Base32 encoding valid characters (A-Z, 2-7)
        return secret.matches(base32Regex)
    }

    // Validate Base64 format
    private fun isValidBase64(secret: String): Boolean {
        val base64Regex = "^[A-Za-z0-9+/]*={0,2}$".toRegex()  // Base64 characters and optional padding
        return secret.matches(base64Regex)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}