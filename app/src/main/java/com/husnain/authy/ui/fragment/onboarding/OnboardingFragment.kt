package com.husnain.authy.ui.fragment.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentOnboardingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OnboardingAdapter
    @Inject lateinit var preferenceManager: PreferenceManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        makeFragmentFullScreen()
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
            "Welcome",
            "Discover Features",
            "Get Started"
        )

        val descriptions = listOf(
            "Welcome to our app. Enjoy the journey!",
            "Discover amazing features tailored for you.",
            "Let’s get started and explore together!"
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
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.onboardingFragment, true)  // Remove OnboardingFragment from the back stack
                .build()

            preferenceManager.saveOnboardingFinished(true)
            if (preferenceManager.isGuestUser()){
                startActivity(MainActivity::class.java)
                requireActivity().finish()
            }else{
                findNavController().navigate(R.id.action_onboardingFragment_to_signupFragment, null, navOptions)
            }
        } else {
            binding.viewPager.setCurrentItem(currentItem + 1, true)
        }
    }

    private fun makeFragmentFullScreen() {
        requireActivity().window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the flags to restore the default UI behavior
        requireActivity().window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE // Reset the system UI visibility
        }
        _binding = null
    }
}