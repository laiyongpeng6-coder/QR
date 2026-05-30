package com.qrscanfast.core.domain.model

/**
 * Represents product information retrieved from an external product database.
 *
 * This domain model is returned by [ProductRepository.lookupProduct] after
 * querying either the local cache or the remote API. It contains the essential
 * product details displayed on the Product Detail screen.
 *
 * @property barcode The product barcode string (EAN-13, UPC-A, etc.) used as the lookup key.
 * @property name The product's display name.
 * @property description Optional detailed description of the product.
 * @property category Optional product category (e.g., "Electronics", "Food & Beverage").
 * @property imageUrl Optional URL to the product's image for display.
 *
 * @see com.qrscanfast.core.domain.repository.ProductRepository
 */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val imageUrl: String? = null
)
