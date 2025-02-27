package com.husnain.authy.ui.fragment.main.otpDetail

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.NativeAdView
import com.husnain.authy.BuildConfig
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.FragmentTotpDetailBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.TotpUtil
import com.husnain.authy.utls.admob.NativeAdUtils
import com.husnain.authy.utls.copyToClip
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.showBottomSheetDialog
import com.husnain.authy.utls.showSnackBar
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TotpDetailFragment : Fragment() {
    private var _binding: FragmentTotpDetailBinding? = null
    private val binding get() = _binding!!
    private var secretKey = ""
    private var serviceName = ""
    private var docId = ""
    private var updateHandler: Handler? = null
    private val vmHome: VmHome by viewModels()
    @Inject lateinit var daoTotp: DaoTotp
    @Inject lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotpDetailBinding.inflate(inflater, container, false)
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
        getArgs()
        generateTotp()
        setOnClickListener()
        setUpObservers()
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

    private fun setUpObservers() {
        vmHome.deleteState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    Flags.comingBackFromDetailAfterDelete = true
                    popBack()
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })

        vmHome.insertRecentlyDeletedState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {

                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun getArgs() {
        val accountName = arguments?.getString("accountName")
        val secretKey = arguments?.getString("secretKey")
        val docId = arguments?.getString("docId")

        if (accountName != null && secretKey != null && docId != null) {
            binding.edtAccountName.setText(accountName)
            this.secretKey = secretKey
            this.serviceName = accountName
            this.docId = docId
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.btnEditName.setOnClickListener {
            binding.edtAccountName.isEnabled = true
            binding.edtAccountName.requestFocus()
            binding.edtAccountName.setSelection(binding.edtAccountName.text.length)
            showKeyboard(binding.edtAccountName)
            it.gone()
        }

        // Handle "Done" action on keyboard
        binding.edtAccountName.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performActionOnDone()

                hideKeyboard(binding.edtAccountName)
                binding.btnEditName.visible()
                binding.edtAccountName.clearFocus()
                binding.edtAccountName.isEnabled = false
                true
            } else {
                false
            }
        }

        binding.btnCopy.setOnClickListener {
            requireContext().copyToClip(binding.tvOtp.text.toString())
            showSnackBar(binding.root, "Code copied to clipboard.")
        }
        binding.btnDeleteAccount.setOnClickListener {
            showBottomSheetDialog(
                "Remove this account",
                "move to trash",
                "Remove",
                true,
                onPrimaryClick = {
                    vmHome.insertToRecentlyDeleted(
                        RecentlyDeleted(
                            serviceName,
                            secretKey,
                            docId
                        )
                    )
                    vmHome.deleteTotp(secretKey)
                })
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    // Function to hide keyboard
    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Function to perform action on "Done"
    private fun performActionOnDone() {
        val enteredText = binding.edtAccountName.text.toString()
        lifecycleScope.launch {
            try {
                daoTotp.updateServiceNameBySecretKey(secretKey,enteredText)
            }catch (e: Exception){
                showCustomToast(e.localizedMessage)
            }
        }
    }

    private fun generateTotp() {
        updateHandler = Handler(Looper.getMainLooper())
        val updateTotp = object : Runnable {
            override fun run() {
                try {
                    val totp = TotpUtil.generateTotp(secretKey)
                    binding.tvOtp.text = totp.chunked(3).joinToString(" ")

                    val remainingSeconds = TotpUtil.getRemainingSeconds()
                    binding.tvCounter.text = remainingSeconds.toString()
                    binding.progressIndicator.progress = remainingSeconds
                    updateHandler?.postDelayed(this, 1000L)
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.tvOtp.text = getString(R.string.string_error)
                }
            }
        }

        // Start TOTP updates
        updateHandler?.post(updateTotp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Flags.comingBackFromDetailAfterDelete = true
        _binding = null
        updateHandler?.removeCallbacksAndMessages(null)
    }
}