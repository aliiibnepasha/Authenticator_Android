package com.husnain.authy.ui.fragment.main.importFromGoogle

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentImportFromGoogleBinding
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack

class ImportFromGoogleFragment : Fragment() {
    private var _binding: FragmentImportFromGoogleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImportFromGoogleBinding.inflate(inflater, container, false)
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

        binding.lyImportFromOtherDevice.setOnClickListener {
            navigate(R.id.action_importFromGoogleFragment_to_importFromOtherDeviceFragment)

        }
        binding.lyImportFromSameDevice.setOnClickListener {
            navigate(R.id.action_importFromGoogleFragment_to_addFromSameDeviceFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}