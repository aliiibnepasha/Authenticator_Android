package com.husnain.authy.ui.fragment.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentOnboardingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OnboardingAdapter
    @Inject
    lateinit var preferenceManager: PreferenceManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        inIt()
        setUpViewPagerAdapter();
        return binding.root
    }

    private fun setUpViewPagerAdapter() {
        val images = listOf(
            R.drawable.img_onboarding1,
            R.drawable.img_onboarding2,
            R.drawable.img_onboarding3
        )

        val titles = listOf(
            getString(R.string.stay_one_step_ahead),
            getString(R.string.protect_what_matters),
            getString(R.string.access_made_easy)
        )

        val descriptions = listOf(
            getString(R.string.get_seamless_authentication_and_advanced_security_features_all_in_one_place),
            getString(R.string.protect_what_matters_with_enhanced_security_and_peace_of_mind),
            getString(R.string.secure_your_digital_world_with_ease)
        )

        adapter = OnboardingAdapter(images, titles, descriptions)
        binding.viewPager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.viewPager)
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.tvNext.setOnClickListener {
            movePageNext()
        }
    }

    private fun movePageNext() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem == adapter.itemCount - 1) {
            preferenceManager.saveOnboardingFinished(true)
            startActivity(MainActivity::class.java)
            requireActivity().finish()
        } else {
            binding.viewPager.setCurrentItem(currentItem + 1, true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}