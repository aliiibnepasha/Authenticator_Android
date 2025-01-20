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
    @Inject
    lateinit var preferenceManager: PreferenceManager

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

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(purchaseUpdateListener)
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("weekly_plan")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            val result = billingClient.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val updatedSubscriptionDataList = mutableListOf<ModelSubscription>()

                result.productDetailsList?.forEach { productDetails ->
                    productDetailsMap[productDetails.productId] = productDetails

                    val price =
                        productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    val title = productDetails.name

                    updatedSubscriptionDataList.add(
                        ModelSubscription(
                            "Weakly",
                            "Most popular",
                            price!!,
                            productDetails.productId
                        )
                    )
                }
                withContext(Dispatchers.Main) { adapter.updateData(updatedSubscriptionDataList) }
            } else {
                withContext(Dispatchers.Main) {
                    showCustomToast("Failed to fetch product details: ${result.billingResult.debugMessage}")
                }
            }
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnCheckout.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                val intent = Intent(requireActivity(), AuthActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                requireActivity().startActivity(intent)

            } else {
                if (::selectedProductId.isInitialized && selectedProductId.isNotEmpty()) {
                    initiateSubscribe(selectedProductId)
                } else {
                    showCustomToast("Please select at least one")
                }
            }
        }
    }

    private fun initAdapter() {
        val defaultSubscriptionDataList = listOf(
            ModelSubscription("Weekly", "Most Popular", "", "weekly_plan"),
        )
        adapter = AdapterSubscription(defaultSubscriptionDataList) { selectedSubscription ->
            selectedProductId = selectedSubscription.productId
        }
        binding.rvSubscription.adapter = adapter
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
                showCustomToast("Failed to launch billing flow: ${billingResult.debugMessage}")
            }
        } else {
            showCustomToast("Product details not available for $productId")
        }
    }

    private val purchaseUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            preferenceManager.saveSubscriptionActive(true)
            requireActivity().runOnUiThread {
            }
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
