package com.husnain.authy.ui.fragment.main.subscription

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelSubscription
import com.husnain.authy.databinding.FragmentSubscriptionBinding
import com.husnain.authy.ui.fragment.main.subscription.adapter.AdapterSubscription

class SubscriptionFragment : Fragment() {
    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpAdapter()
    }

    private fun setUpAdapter() {
        val subscriptionDataList = listOf(
            ModelSubscription("weekly","Most Popular",7.99),
            ModelSubscription("Monthly","Most Popular",49.9),
            ModelSubscription("Annually","Most Popular",199.99),
        )
        val adapter = AdapterSubscription(subscriptionDataList){

        }
        binding.rvSubscription.adapter = adapter
    }

    private fun setOnClickListener() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}