package com.husnain.authy.ui.fragment.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentHomeBinding
import com.husnain.authy.utls.navigate

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.btnAddNewAccountWhenSomeAccountAdded.setOnClickListener {

        }

        binding.btnAddAccountFirstTime.setOnClickListener {
            navigate(R.id.action_homeFragment_to_addNewFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}