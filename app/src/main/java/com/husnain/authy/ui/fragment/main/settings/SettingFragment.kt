package com.husnain.authy.ui.fragment.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.ui.fragment.auth.VmAuth
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmAuth: VmAuth by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObserver()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.lyGetPremium.setOnClickListener {
            vmAuth.logout()
        }
        binding.lyLocalizeLanguages.setOnClickListener {
            navigate(R.id.action_settingFragment_to_localizeFragment)
        }
        binding.lyDeleteAccount.setOnClickListener {
            showDeleteAccountDialog {
                vmAuth.deleteAccount()
            }
        }
    }

    private fun setUpObserver(){
        vmAuth.logoutState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    startActivity(AuthActivity::class.java)
                    requireActivity().finish()
                }

                is DataState.Error -> {
                    showCustomToast(state.message)
                }
            }
        })

        vmAuth.deleteAccountState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    startActivity(AuthActivity::class.java)
                    requireActivity().finish()
                }

                is DataState.Error -> {
                    showCustomToast(state.message)
                }
            }
        })
    }

    //dialog
    fun showDeleteAccountDialog(onConfirm: () -> Unit) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Delete Account")
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.")
        builder.setPositiveButton("Yes") { _, _ -> onConfirm() }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}