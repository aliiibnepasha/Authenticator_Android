package com.husnain.authy.ui.fragment.main.localization

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.rpc.context.AttributeContext.Auth
import com.husnain.authy.BuildConfig
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelLanguage
import com.husnain.authy.databinding.FragmentLocalizeBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.admob.NativeAdUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocalizeFragment : Fragment() {
    private var _binding: FragmentLocalizeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var modelLang: ModelLanguage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocalizeBinding.inflate(inflater, container, false)
        if (!preferenceManager.isSubscriptionActive()) {
            loadAndPopulateAd()
        } else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.gone()
            binding.frameLayout2.gone()
        }
        inIt()
        return binding.root
    }

    private fun inIt() {
        modelLang = preferenceManager.getLang()?.let { ModelLanguage("", it) }!!
        setOnClickListener()
        setUpAdapter()
    }


    private fun loadAndPopulateAd() {
        binding.shimmerLayout.startShimmer()
        NativeAdUtils.loadNativeAd(requireContext(), getNativeAdId(),false) { nativeAd ->
            if (nativeAd != null) {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.gone()
                binding.frameLayout2.visible()
                // Native ad successfully loaded, now populate the ad in the layout
                val adView: NativeAdView = binding.adContainer.nativeAdView
                NativeAdUtils.populateNativeAdView(nativeAd, adView)
            } else {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.gone()
                binding.frameLayout2.gone()
            }
        }
    }

    private fun getNativeAdId(): String {
        return if (BuildConfig.DEBUG) {
            getString(R.string.admob_native_ad_id_test)
        } else {
            getString(R.string.admob_native_ad_id_release_language_screen)
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.tvDone.setOnClickListener {
            val isComingFromOnboarding = arguments?.getBoolean("comingFromOnboarding") ?: false
            if (isComingFromOnboarding) {
                if (::modelLang.isInitialized) {
                    changeLanguage(modelLang)
                    navigate(R.id.action_localizeFragment2_to_onboardingFragment)
                }
            } else {
                if (::modelLang.isInitialized) {
                    changeLanguage(modelLang)
                    popBack()
                }
            }
        }
    }

    private fun setUpAdapter() {
        val languagesList = listOf(
            ModelLanguage("English", "en"),
            ModelLanguage("Arabic (العربية)", "ar"),
            ModelLanguage("Spanish (Español)", "es"),
            ModelLanguage("French (Français)", "fr"),
            ModelLanguage("Indonesian (Bahasa Indonesia)", "id"),
            ModelLanguage("Chinese (中文)", "zh"),
            ModelLanguage("Hebrew (עברית)", "he"),
            ModelLanguage("Russian (русский)", "ru"),
            ModelLanguage("Hindi (हिंदी)", "hi"),
            ModelLanguage("Filipino", "tl"),
            ModelLanguage("Turkish (Türkçe)", "tr"),
            ModelLanguage("Urdu (اردو)", "ur")
        )

        val adapter = AdapterLanguages(languagesList) {
            this.modelLang = it
        }
        binding.rvLocalizationLanugages.adapter = adapter
        adapter.updateSelectedLang(preferenceManager.getLang())
        binding.rvLocalizationLanugages.hasFixedSize()
    }

    private fun changeLanguage(modelLanguage: ModelLanguage) {
        preferenceManager.saveLang(modelLanguage.langShortType)
        val isComingFromOnboarding = arguments?.getBoolean("comingFromOnboarding") ?: false
        if (isComingFromOnboarding) {
            (requireActivity() as AuthActivity).changeLanguage(modelLanguage.langShortType)
        } else {
            (requireActivity() as MainActivity).changeLanguage(modelLanguage.langShortType)
        }

        // Update the layout direction dynamically
        val newLocale = Locale(modelLanguage.langShortType)
        Locale.setDefault(newLocale)

        val layoutDirection =
            if (TextUtils.getLayoutDirectionFromLocale(newLocale) == View.LAYOUT_DIRECTION_RTL) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }

        requireActivity().window.decorView.layoutDirection = layoutDirection
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}