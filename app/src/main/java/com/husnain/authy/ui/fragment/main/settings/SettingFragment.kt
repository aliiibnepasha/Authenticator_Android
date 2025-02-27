package com.husnain.authy.ui.fragment.main.settings

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8
import android.util.Base64
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.datatransport.runtime.scheduling.jobscheduling.SchedulerConfig.Flag
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.DialogPasswordBinding
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.showBottomSheetDialog
import com.husnain.authy.utls.progress.showDeleteAccountConfirmationBottomSheet
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmSettings: VmSettings by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var daoTotp: DaoTotp

    @Inject
    lateinit var auth: FirebaseAuth
    private val KEY_HEADER_TITLE = "headerTitle"
    private val KEY_LINK_TO_LOAD = "linkToLoad"
    private val salt = "some_fixed_salt".toByteArray()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        preferenceManager.incrementOpenCount()
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
                tvSignInToBackUp.visible()
                btnLogout.gone()
                lyDeleteAccount.gone()

            } else {
                lyDeleteAccount.visible()
                btnLogout.visible()
                tvSignInToBackUp.gone()
            }
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.lyHowToUse.setOnClickListener {
            showCustomToast("Coming Soon!")
        }

        binding.ly2faGuides.setOnClickListener {
            showCustomToast("Coming Soon!")
        }

        binding.lyExport.setOnClickListener {
            findNavController().navigate(R.id.action_settingFragment_to_exportCodesFragment)
        }

        binding.lyImport.setOnClickListener {
            openFilePickerForImport()
        }

        //Navto buy premium
        binding.lyGetPremium.setOnClickListener {
            Flags.isNotToShowAd = true
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
                    Flags.isNotToShowAd = true
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
            showBottomSheetDialog(
                resources.getString(R.string.string_logout),
                getString(R.string.are_you_sure_you_want_to_logout),
                getString(R.string.yes),
                true,
                onPrimaryClick = {
                    vmSettings.logout()
                })
        }

        //google authenticator import
        binding.lyImportGoogleAuthData.setOnClickListener {
            navigate(R.id.action_settingFragment_to_importFromGoogleFragment)
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
            putExtra(
                Intent.EXTRA_TEXT, "Hey there! \uD83D\uDC4B\n" +
                        "\n" +
                        "Stay secure with Authenticator, the simple and reliable app for generating 2FA codes to protect your accounts.\n" +
                        "\n" +
                        "✨ Key Features:\n" +
                        "✅ Easy to use.\n" +
                        "✅ Backup & restore made simple.\n" +
                        "✅ Your data, your privacy.\n ${Constants.PLAY_STORE_URL}"
            )
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


    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                importTotpDetails(it)
            }
        }

    private fun importTotpDetails(uri: Uri) {
        lifecycleScope.launch {
            val json = readJsonFromUri(uri)
            if (isEncrypted(json)) {
                showPasswordDialog(uri)
            } else {
                val totpDetails = parseJsonToTotpDetails(json)
                saveTotpDetailsToDb(totpDetails)
            }
        }
    }

    private fun isEncrypted(data: String): Boolean {
        // Assuming an encrypted file contains base64-encoded data (you can modify this check based on your format)
        return data.startsWith("ENCRYPTED")
    }

    private fun showPasswordDialog(uri: Uri) {
        // Initialize dialog
        val dialog = Dialog(requireContext())
        val binding = DialogPasswordBinding.inflate(LayoutInflater.from(requireContext()))


        dialog.apply {
            setContentView(binding.root)
            setCancelable(true)

            val horizontalMargin = context.resources.getDimensionPixelSize(R.dimen.padding_top_20dp)
            val screenWidth = context.resources.displayMetrics.widthPixels
            val dialogWidth = screenWidth - 2 * horizontalMargin

            window?.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)
        }


        // Handling the OK button
        binding.btnOk.setOnClickListener {
            val password = binding.edtPassword.text.toString()
            if (password.isEmpty()) {
                showCustomToast(getString(R.string.password_is_required))
            } else {
                lifecycleScope.launch {
                    try {
                        val decryptedJson = decrypt(uri, password)
                        val totpDetails = parseJsonToTotpDetails(decryptedJson)
                        saveTotpDetailsToDb(totpDetails)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showCustomToast(getString(R.string.invalid_password))
                        }
                    }
                }
                dialog.dismiss()
            }
        }

        // Handling the Cancel button
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private suspend fun decrypt(uri: Uri, password: String): String {
        val encryptedData = readJsonFromUri(uri)
        val base64Data = encryptedData.removePrefix("ENCRYPTED")
        val encryptedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val iv = encryptedBytes.copyOfRange(0, 16)
        val encryptedContent = encryptedBytes.copyOfRange(16, encryptedBytes.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        }

        val decryptedData = cipher.doFinal(encryptedContent)
        return String(decryptedData, UTF_8)
    }

    private suspend fun readJsonFromUri(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()
                .use { it?.readText() } ?: ""
        }
    }

    private fun parseJsonToTotpDetails(json: String): List<EntityTotp> {
        val totpDetailsList = mutableListOf<EntityTotp>()
        val jsonObject = JSONObject(json)
        val jsonArray = jsonObject.getJSONArray("totpDetails")

        for (i in 0 until jsonArray.length()) {
            val detailJson = jsonArray.getJSONObject(i)
            val serviceName = detailJson.getString("serviceName")
            val secretKey = detailJson.getString("secretKey")
            totpDetailsList.add(EntityTotp(0, serviceName, secretKey))
        }

        return totpDetailsList
    }

    private suspend fun saveTotpDetailsToDb(totpDetails: List<EntityTotp>) {
        withContext(Dispatchers.IO) {
            try {
                for (totp in totpDetails) {
                    daoTotp.insertOrReplaceTotpData(totp)
                }
            } catch (e: Exception) {
//                showCustomToast(e.localizedMessage)
            } finally {
                withContext(Dispatchers.Main) {
                    Flags.comingBackFromDetailAfterDelete = true
                    showCustomToast(getString(R.string.imported_successfully))  // Show the toast on the main thread
                }
            }
        }
    }

    private fun openFilePickerForImport() {
        getContent.launch("application/json")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}