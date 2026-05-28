package com.qrscanmax.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a cached product lookup result.
 *
 * This entity maps to the `product_cache` table and stores product information
 * retrieved from the external product database API. Cached entries are used to
 * reduce network calls and provide offline access to previously looked-up products.
 *
 * Cache staleness is determined by comparing [cachedAt] against the current time.
 * Entries older than 7 days are considered stale and will trigger a fresh API lookup.
 *
 * @property barcode The product barcode string (EAN-13, UPC-A, etc.) serving as the primary key.
 * @property name The product's display name.
 * @property description Optional detailed description of the product.
 * @property category Optional product category (e.g., "Electronics", "Food & Beverage").
 * @property imageUrl Optional URL to the product's image for display.
 * @property cachedAt Epoch milliseconds when this product was cached. Used for staleness checks.
 *
 * @see com.qrscanmax.core.domain.model.ProductInfo
 */
@Entity(tableName = "product_cache")
data class CachedProductEntity(
    @PrimaryKey
    val barcode: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
