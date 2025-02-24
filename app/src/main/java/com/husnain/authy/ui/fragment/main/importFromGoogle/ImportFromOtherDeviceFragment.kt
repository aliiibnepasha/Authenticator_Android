package com.husnain.authy.ui.fragment.main.importFromGoogle

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentImportFromOtherDeviceBinding
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack

class ImportFromOtherDeviceFragment : Fragment(){
    private var _binding: FragmentImportFromOtherDeviceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImportFromOtherDeviceBinding.inflate(inflater, container, false)
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

        binding.btnScanQrCode.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean(Constants.KEY_IS_COMING_FROM_SETTINGS_FOR_GOOGLE_AUTH_IMPORT,true)
            }
            navigate(R.id.action_importFromOtherDeviceFragment_to_addAccountFragment,bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}