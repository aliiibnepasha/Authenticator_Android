package com.husnain.authy.ui.fragment.main.addAccountManually

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.room.EntityTotp
import com.husnain.authy.databinding.FragmentAddAccountManuallyBinding
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAccountManuallyFragment : Fragment() {
    private var _binding: FragmentAddAccountManuallyBinding? = null
    private val binding get() = _binding!!
    private val vmHome:VmHome by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddAccountManuallyBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
    }

    private fun setUpObservers() {
        vmHome.insertState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    showCustomToast("Data Saved Successfully")
                    navigate(R.id.action_addAccountManuallyFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnAddAccount.setOnClickListener {
            if (validateAccountFields()){
                vmHome.insertSecretData(EntityTotp(0,binding.edtAccountName.text.toString(),binding.edtPrivateKey.text.toString()))
            }
        }
    }

    private fun validateAccountFields(): Boolean {
        val accountName = binding.edtAccountName.text.toString().trim()
        val secretKey = binding.edtPrivateKey.text.toString().trim()

        if (accountName.isEmpty()) {
            showCustomToast("Account name cannot be empty")
            return false
        }

        if (secretKey.isEmpty()) {
            showCustomToast("Private key cannot be empty")
            return false
        }

        if (!isValidSecretKey(secretKey)) {
            showCustomToast("Invalid private key format")
            return false
        }

        return true
    }

    private fun isValidSecretKey(secret: String): Boolean {
        return isValidBase32(secret) || isValidBase64(secret)
    }

    private fun isValidBase32(secret: String): Boolean {
        val base32Regex = "^[A-Z2-7]*$".toRegex()
        return secret.matches(base32Regex)
    }

    private fun isValidBase64(secret: String): Boolean {
        val base64Regex = "^[A-Za-z0-9+/]*={0,2}$".toRegex()
        return secret.matches(base64Regex)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}