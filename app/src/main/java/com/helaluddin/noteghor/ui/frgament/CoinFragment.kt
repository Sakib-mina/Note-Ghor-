package com.helaluddin.noteghor.ui.frgament

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.helaluddin.noteghor.databinding.FragmentCoinBinding
import com.helaluddin.noteghor.ui.viewModel.AuthViewModel
import com.helaluddin.noteghor.ui.viewModel.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoinFragment : Fragment(), PurchasesUpdatedListener {

    private lateinit var binding: FragmentCoinBinding
    private lateinit var billingClient: BillingClient
    private val authViewModel: AuthViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()

    private val skuList = listOf(
        "coins_30", "coins_61", "coins_122", "coins_183",
        "coins_244", "coins_305", "coins_610", "coins_915"
    )

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCoinBinding.inflate(inflater, container, false)

        setupBillingClient()
        setupClickListeners()

        return binding.root
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
            .enablePendingPurchases()
            .setListener(this)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                } else {
                    Toast.makeText(requireContext(), "Billing setup failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(requireContext(), "Billing disconnected", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                skuList.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            ).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productList.forEach { product ->
                    productDetailsMap[product.productId] = product
                    Log.d("CoinFragment", "Loaded: ${product.productId} - ${product.title}")
                }
            } else {
                Log.e("CoinFragment", "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    private fun launchPurchase(productId: String) {
        val product = productDetailsMap[productId] ?: run {
            Toast.makeText(requireContext(), "Product not available", Toast.LENGTH_SHORT).show()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(requireActivity(), flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(requireContext(), "Purchase cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Purchase failed: ${billingResult.debugMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Successfully consumed, now add coins to user
                    val coins = when (purchase.products.firstOrNull()) {
                        "coins_30" -> 30
                        "coins_61" -> 61
                        "coins_122" -> 122
                        "coins_183" -> 183
                        "coins_244" -> 244
                        "coins_305" -> 305
                        "coins_610" -> 610
                        "coins_915" -> 915
                        else -> 0
                    }
                    addCoinsToUser(coins)
                } else {
                    Toast.makeText(requireContext(), "Failed to consume purchase: ${billingResult.debugMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addCoinsToUser(coins: Int) {
        val user = authViewModel.getCurrentUser() ?: return
        lifecycleScope.launch {
            val currentCoins = noteViewModel.getUserData(user.uid)?.coins ?: 0
            noteViewModel.updateUserCoins(user.uid, currentCoins + coins)
            Toast.makeText(requireContext(), "$coins coins added!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnBuy30.setOnClickListener { launchPurchase("coins_30") }
        binding.btnBuy61.setOnClickListener { launchPurchase("coins_61") }
        binding.btnBuy112.setOnClickListener { launchPurchase("coins_122") }
        binding.btnBuy183.setOnClickListener { launchPurchase("coins_183") }
        binding.btnBuy244.setOnClickListener { launchPurchase("coins_244") }
        binding.btnBuy305.setOnClickListener { launchPurchase("coins_305") }
        binding.btnBuy610.setOnClickListener { launchPurchase("coins_610") }
        binding.btnBuy915.setOnClickListener { launchPurchase("coins_915") }
    }
}
