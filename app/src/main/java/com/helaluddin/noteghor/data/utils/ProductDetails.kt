package com.helaluddin.noteghor.data.utils

import com.android.billingclient.api.ProductDetails

data class ProductDetails(
    val productId: String,
    val name: String,
    val title: String,
    val description: String,
    val productType: String,
    val oneTimePurchaseOfferDetails: ProductDetails.OneTimePurchaseOfferDetails?,
    val subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>?
)