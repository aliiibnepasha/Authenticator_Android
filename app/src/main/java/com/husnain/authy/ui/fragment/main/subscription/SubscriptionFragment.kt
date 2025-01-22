package com.husnain.authy.ui.fragment.main.subscription

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.fragment.main.subscription.adapter.AdapterSubscription
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class SubscriptionFragment : Fragment() {
    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var billingClient: BillingClient
    private lateinit var adapter: AdapterSubscription
    private lateinit var selectedProductId: String
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        setupBillingClient()
        initAdapter()
        setOnClickListener()
        return binding.root
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnCheckout.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            } else {
                if (::selectedProductId.isInitialized && selectedProductId.isNotEmpty()) {
                    if (selectedProductId == Constants.lifeTimePorductId){
                        initiatePurchase(selectedProductId)
                    }else{
                        initiateSubscribe(selectedProductId)
                    }
                } else {
                    showCustomToast(getString(R.string.string_please_select_at_least_one))
                }
            }
        }
    }

    private fun initAdapter() {
        val defaultSubscriptionDataList = listOf(ModelSubscription("Weekly", "Most Popular", "$7.99", "weekly_plan"),)
        adapter = AdapterSubscription(defaultSubscriptionDataList) { selectedSubscription ->
            selectedProductId = selectedSubscription.productId
        }
        binding.rvSubscription.adapter = adapter
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

            val sortedSubscriptionDataList = updatedSubscriptionDataList.sortedWith(compareBy { subscription ->
                when (subscription.productId) {
                    Constants.weaklySubId -> 1
                    Constants.monthlySubId -> 2
                    Constants.lifeTimePorductId -> 3
                    else -> Int.MAX_VALUE
                }
            })

            // Update the UI with the sorted results
            withContext(Dispatchers.Main) { adapter.updateData(sortedSubscriptionDataList) }
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
                    "lifetime_pro" -> handleLifetimePurchase(purchase)
                    else -> handleSubscribe(purchase)
                }

            }
        }
    }

    private fun handleSubscribe(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            preferenceManager.saveSubscriptionActive(true)
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

    private fun handleLifetimePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            preferenceManager.saveLifeTimeAccessActive(true)
            showCustomToast("Lifetime purchase completed successfully!")
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

    override fun onDestroyView() {
        super.onDestroyView()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        _binding = null
    }
}
