package com.husnain.authy.ui.fragment.main.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.showBottomSheetDialog
import com.husnain.authy.utls.progress.showDeleteAccountConfirmationBottomSheet
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmSettings: VmSettings by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var auth: FirebaseAuth
    private val KEY_HEADER_TITLE = "headerTitle"
    private val KEY_LINK_TO_LOAD = "linkToLoad"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        inIt()
        updateUiBasedOnUserState()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateUiBasedOnUserState()
    }

    private fun inIt() {
        setOnClickListener()
        setUpObserver()
    }

    private fun updateUiBasedOnUserState() {
        val userId = auth.currentUser?.uid
        val isUserLoggedIn = userId != null
        val isUserHavePremium = preferenceManager.isSubscriptionActive()
        val isUserLifeTimePremium = preferenceManager.isLifeTimeAccessActive()

        binding.apply {
            // Handle premium state
            if (isUserLifeTimePremium) {
                lyGetPremium.gone()
                imgSyncPremium.gone()
            } else {
                lyGetPremium.visible()
                imgSyncPremium.visible()
            }

            // Handle login state
            if (!isUserLoggedIn) {
                btnLogout.gone()
                lyDeleteAccount.gone()

            } else {
                lyDeleteAccount.visible()
                btnLogout.visible()

            }
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }


        //Navto buy premium
        binding.lyGetPremium.setOnClickListener {
            navigate(R.id.action_settingFragment_to_subscriptionFragment)
        }

        //nav to change lang 
        binding.lyLocalizeLanguages.setOnClickListener {
            navigate(R.id.action_settingFragment_to_localizeFragment)
        }

        //delete account
        binding.lyDeleteAccount.setOnClickListener {
            //show confirmation bottom sheet to user on delete tv click delete user data and account from db
            showDeleteAccountConfirmationBottomSheet {
                vmSettings.deleteAccount()
            }
        }

        //Backup and sync
        binding.lyBackupAndSync.setOnClickListener {
            val isUserLoggedIn = auth.currentUser?.uid != null
            val isUserHavePremium = preferenceManager.isSubscriptionActive()
            Log.d(Constants.TAG, "premium = $isUserHavePremium, loggedIn = $isUserLoggedIn")

            when {
                // If the user is not subscribed, take them to the subscription screen
                !isUserHavePremium -> {
                    navigate(R.id.action_settingFragment_to_subscriptionFragment)
                }

                // If the user is subscribed but not logged in, take them to the auth activity
                isUserHavePremium && !isUserLoggedIn -> {
                    Constants.isComingToAuthFromGuest = true
                    startActivity(AuthActivity::class.java)
                }

                // If the user is subscribed and logged in, take them to the backup screen
                isUserHavePremium && isUserLoggedIn -> {
                    navigate(R.id.action_settingFragment_to_backUpFragment)
                }
            }
        }


        //Recently deleted
        binding.lyRecentlyDeleted.setOnClickListener {
            navigate(R.id.action_settingFragment_to_recentlyDeletedFragment)
        }

        //logout
        binding.btnLogout.setOnClickListener {
            showBottomSheetDialog(resources.getString(R.string.string_logout),"Are you sure you want to logout","Yes",true, onPrimaryClick = {
                vmSettings.logout()
            })
        }

        //google authenticator import
        binding.lyImportGoogleAuthData.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean(Constants.KEY_IS_COMING_FROM_SETTINGS_FOR_GOOGLE_AUTH_IMPORT,true)
            }
            navigate(R.id.action_settingFragment_to_addAccountFragment,bundle)
        }

        //Info and share
        binding.lyTermsAndConsidtions.setOnClickListener {
            navToTermsAndConditions()
        }
        binding.lyPrivacyPolicy.setOnClickListener {
            navToPrivacyPolicy()
        }
        binding.lyRateUs.setOnClickListener {
            rateApp()
        }
        binding.lyShareOurApp.setOnClickListener {
            shareApp()
        }
    }


    private fun setUpObserver() {
        vmSettings.logoutState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    popBack()
                }

                is DataState.Error -> {
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
                }
            }
        })

        vmSettings.deleteAccountState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                    binding.loadingViewMain.start()
                }

                is DataState.Success -> {
                    binding.loadingViewMain.stop()
                    popBack()
                }

                is DataState.Error -> {
                    binding.loadingViewMain.stop()
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
                }
            }
        })

        vmSettings.insertState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    navigate(R.id.action_settingFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
                }
            }
        })
    }


    //info navigations
    private fun navToTermsAndConditions() {
        val url = "https://sites.google.com/view/authenticatorapp-termofuse/home"
        val bundle = Bundle().apply {
            putString(KEY_HEADER_TITLE, resources.getString(R.string.string_terms_amp_condition))
            putString(KEY_LINK_TO_LOAD, url)
        }
        navigate(R.id.action_settingFragment_to_webViewFragment, bundle)
    }

    private fun navToPrivacyPolicy() {
        val url = "https://sites.google.com/view/authenticatorapp-privacypolicy/home"
        val bundle = Bundle().apply {
            putString(KEY_HEADER_TITLE, resources.getString(R.string.string_privacy_policy))
            putString(KEY_LINK_TO_LOAD, url)
        }
        navigate(R.id.action_settingFragment_to_webViewFragment, bundle)
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
            putExtra(Intent.EXTRA_TEXT, "Hey there! \uD83D\uDC4B\n" +
                    "\n" +
                    "Stay secure with Authenticator, the simple and reliable app for generating 2FA codes to protect your accounts.\n" +
                    "\n" +
                    "✨ Key Features:\n" +
                    "✅ Easy to use.\n" +
                    "✅ Backup & restore made simple.\n" +
                    "✅ Your data, your privacy.\n ${Constants.PLAY_STORE_URL}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun rateApp() {
        try {
            val uri = Uri.parse("market://details?id=com.theswiftvision.authenticatorapp")
            val rateIntent = Intent(Intent.ACTION_VIEW, uri)
            rateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(rateIntent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store is unavailable, open in a web browser
            val webUri = Uri.parse(Constants.PLAY_STORE_URL)
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(webIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}