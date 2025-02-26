package com.husnain.authy.ui.fragment.onboarding

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAdView
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentOnboardingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.admob.AdUtils
import com.husnain.authy.utls.admob.NativeAdUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.invisible
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
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
        if (!preferenceManager.isSubscriptionActive()){
            loadInlineAdaptiveBannerAd()
        }else{
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.gone()
            binding.adView.gone()
        }
        inIt()
        setUpViewPagerAdapter();
        return binding.root
    }

    private fun loadInlineAdaptiveBannerAd() {
        val adSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
            requireContext(),
            AdSize.FULL_WIDTH
        )

        val bannerView = AdView(requireContext()).apply {
            adUnitId = getString(R.string.admob_banner_id_test)
            setAdSize(adSize)
        }

        val adRequest = AdRequest.Builder().build()

        // Start shimmer effect while ad is loading
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.adView.visibility = View.GONE

        bannerView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                // Ad successfully loaded
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.adView.visibility = View.VISIBLE

                // Update constraint to attach RelativeLayout above AdView with margin
                val constraintLayout = binding.constraintLayout
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.clear(binding.relativeLayout.id, ConstraintSet.BOTTOM)
                constraintSet.connect(
                    binding.relativeLayout.id,
                    ConstraintSet.BOTTOM,
                    binding.adView.id,
                    ConstraintSet.TOP,
                    20 // Adding 20dp margin
                )

                constraintSet.applyTo(constraintLayout)
            }


            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                // Ad failed to load, stop shimmer and restore constraints
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.adView.visibility = View.GONE

                val constraintLayout = binding.constraintLayout
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.clear(binding.relativeLayout.id, ConstraintSet.BOTTOM)
                constraintSet.connect(
                    binding.relativeLayout.id,
                    ConstraintSet.BOTTOM,
                    binding.shimmerLayout.id,
                    ConstraintSet.TOP
                )

                constraintSet.applyTo(constraintLayout)
            }
        }

        bannerView.loadAd(adRequest)
        binding.adView.addView(bannerView)
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

        adapter = OnboardingAdapter(images)
        binding.viewPager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.titleText.text = titles[position]
                binding.descriptionText.text = descriptions[position]
            }
        })
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.tvNext.setOnClickListener {
            movePageNext()
        }
        binding.tvSkip.setOnClickListener {
            preferenceManager.saveOnboardingFinished(true)
            startActivity(MainActivity::class.java)
            requireActivity().finish()
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