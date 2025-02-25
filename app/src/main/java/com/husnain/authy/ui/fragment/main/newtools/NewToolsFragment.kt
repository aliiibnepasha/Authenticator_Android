package com.husnain.authy.ui.fragment.main.newtools

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.app.App
import com.husnain.authy.databinding.FragmentNewToolsBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NewToolsFragment : Fragment() {
    private var _binding: FragmentNewToolsBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewToolsBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        inItUi()
        setOnClickListener()
    }

    private fun inItUi() {
        binding.switchAllowScreenShots.isChecked = preferenceManager.isAllowScreenShots()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.lyAppLock.setOnClickListener {
            navigate(R.id.action_newToolsFragment_to_appLockFragment)
        }

        binding.switchAllowScreenShots.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.saveIsAllowScreenShots(isChecked)

            if (isChecked) {
                (requireActivity().application as App).setScreenshotRestriction(true)
            } else {
                (requireActivity().application as App).setScreenshotRestriction(false)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}