package com.husnain.authy.ui.fragment.main.subscription

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelSubscription
import com.husnain.authy.databinding.FragmentSubscriptionBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.ui.fragment.main.subscription.adapter.AdapterSubscription
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.admob.AdUtils
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.ProgressDialogUtil.dismissProgressDialog
import com.husnain.authy.utls.progress.ProgressDialogUtil.showProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class SubscriptionFragment : Fragment() {
    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var billingClient: BillingClient
    private lateinit var adapter: AdapterSubscription
    private var selectedProductId = Constants.weaklySubId
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    private val vmSubscription: VmSubscription by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        Log.d(Constants.TAG, "landed on subscription fragment")
        if (!Flags.isComingFromSplash && !preferenceManager.isSubscriptionActive()) {
            vmSubscription.loadAd(requireActivity())
        }
        setupBillingClient()
        setOnClickListener()
        return binding.root
    }

    private fun onCrossClick() {
        if (!preferenceManager.isSubscriptionActive()) {
            /**
             * - Shows the preloaded ad if launched from the splash screen.
             * - Loads the ad in `onCreateView()` if not coming from the splash screen.
             */
            if (Flags.isComingFromSplash) {
                Flags.isComingFromSplash = false
                Log.d(Constants.TAG, "interstial ad on showed")
                AdUtils.showInterstitialAdWithCallback(requireActivity(), failureCallback = {
                    if (!preferenceManager.isOnboardingFinished()) {
                        findNavController().navigate(R.id.action_subscriptionFragment2_to_onboardingFragment)
                    } else {
                        popBack()
                    }
                }, showCallback = {
                    popBack()
                }
                )
            } else {
                /**
                 * Observes the `isAdLoaded` status and performs actions based on its value.
                 * - `null`: Shows loading indicator.
                 * - `true`: Stops loading and shows the interstitial ad.
                 * - `false`: Stops loading and navigates back.
                 */
                vmSubscription.isAdLoaded.observe(viewLifecycleOwner) { isAdLoaded ->
                    when (isAdLoaded) {
                        null -> {
                            binding.mainLoadingView.start()
                        }

                        true -> {
                            binding.mainLoadingView.stop()
                            AdUtils.showInterstitialAdWithCallback(requireActivity(), failureCallback = {
                                if (!preferenceManager.isOnboardingFinished()) {
                                    navigate(R.id.action_subscriptionFragment2_to_onboardingFragment)
                                } else {
                                    popBack()
                                }
                            }, showCallback = {
                                popBack()
                            }
                            )
                        }

                        false -> {
                            binding.mainLoadingView.stop()
                            popBack()
                        }
                    }
                }
            }
        } else {
            popBack()
        }
    }

    private fun setOnClickListener() {
        binding.imgCross.setOnClickListener {
            onCrossClick()
        }
        binding.btnCheckout.setOnClickListener {
            if (selectedProductId == Constants.lifeTimePorductId) {
                initiatePurchase(selectedProductId)
            } else {
                initiateSubscribe(selectedProductId)
            }
        }
    }

    private fun initAdapter(data: List<ModelSubscription>) {
        adapter = AdapterSubscription(data)
        binding.rvSubscription.adapter = adapter

        adapter.itemClickListener { selectedSubscription ->
            selectedProductId = selectedSubscription.productId
        }
    }

    //Billing
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(subscribeUpdateListener)
            .enablePendingPurchases()
            .build()
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.e("Billing", "Billing service disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "Billing service connected")
                    queryProductDetails()
                } else {
                    showCustomToast("Failed to connect to billing service: ${billingResult.debugMessage}")
                }
            }
        })
    }

    private fun queryProductDetails() {
        // Define subscription products
        showProgressDialog()
        val subsProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.weaklySubId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.monthlySubId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        // Define in-app products
        val inAppProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.lifeTimePorductId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val updatedSubscriptionDataList = mutableListOf<ModelSubscription>()

            // Query subscriptions
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProductList)
                .build()
            val subsResult = billingClient.queryProductDetails(subsParams)

            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subsResult.productDetailsList?.forEach { productDetails ->
                    productDetailsMap[productDetails.productId] = productDetails

                    val price = productDetails.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""

                    val (duration, label) = getSubscriptionDurationAndLabel(productDetails.productId)
                    updatedSubscriptionDataList.add(
                        ModelSubscription(
                            duration,
                            label,
                            price,
                            productDetails.productId
                        )
                    )
                }
            }

            // Query in-app products
            val inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inAppProductList)
                .build()
            val inAppResult = billingClient.queryProductDetails(inAppParams)

            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                inAppResult.productDetailsList?.forEach { productDetails ->
                    productDetailsMap[productDetails.productId] = productDetails

                    val price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""

                    val (duration, label) = getSubscriptionDurationAndLabel(productDetails.productId)
                    updatedSubscriptionDataList.add(
                        ModelSubscription(
                            duration,
                            label,
                            price,
                            productDetails.productId
                        )
                    )
                }
            }

            val sortedSubscriptionDataList =
                updatedSubscriptionDataList.sortedWith(compareBy { subscription ->
                    when (subscription.productId) {
                        Constants.weaklySubId -> 1
                        Constants.monthlySubId -> 2
                        Constants.lifeTimePorductId -> 3
                        else -> Int.MAX_VALUE
                    }
                })

            // Update the UI with the sorted results
            withContext(Dispatchers.Main) {
                try {
                    initAdapter(sortedSubscriptionDataList)
                } catch (e: Exception) {
                    e.message?.let { showCustomToast(it) }
                }
                dismissProgressDialog()
            }
        }
    }

    private fun initiateSubscribe(productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails != null) {
            val offerToken = productDetails.subscriptionOfferDetails?.first()?.offerToken
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken ?: "")
                            .build()
                    )
                )
                .build()
            val billingResult = billingClient.launchBillingFlow(requireActivity(), flowParams)
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
            }
        } else {
            showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
        }
    }

    private val subscribeUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                when (purchase.products.firstOrNull()) {
                    Constants.lifeTimePorductId -> handleLifetimePurchase(purchase)
                    else -> handleSubscribe(purchase)
                }

            }
        }
    }

    private fun handleLifetimePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            preferenceManager.saveSubscriptionActive(true)
            preferenceManager.saveLifeTimeAccessActive(true)
            if (!preferenceManager.isOnboardingFinished()) {
                navigate(R.id.action_subscriptionFragment2_to_onboardingFragment)
            } else {
                (activity as? MainActivity)?.preloadAd()
                popBack()
            }
        } else {
            showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
        }
    }

    private fun handleSubscribe(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            preferenceManager.saveSubscriptionActive(true)
            if (!preferenceManager.isOnboardingFinished()) {
                navigate(R.id.action_subscriptionFragment2_to_onboardingFragment)
            } else {
                (activity as? MainActivity)?.preloadAd()
                popBack()
            }
        }
    }

    private fun initiatePurchase(productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails != null) {
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            val billingResult = billingClient.launchBillingFlow(requireActivity(), flowParams)
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
            }
        } else {
            showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
        }
    }


    private fun getSubscriptionDurationAndLabel(productId: String): Pair<String, String> {
        return when (productId) {
            Constants.weaklySubId -> Pair("Weekly", "Most popular")
            Constants.monthlySubId -> Pair("Monthly", "Best value")
            Constants.lifeTimePorductId -> Pair("Lifetime Access", "Enterprise")
            else -> Pair("Unknown", "N/A")
        }
    }

    override fun onPause() {
        Log.d(Constants.TAG, "Subscription screen onPause")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        Log.d(Constants.TAG, "Subscription screen onStop")
        super.onStop()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(Constants.TAG, "Subscription screen onDestroy")
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        _binding = null
    }
}
