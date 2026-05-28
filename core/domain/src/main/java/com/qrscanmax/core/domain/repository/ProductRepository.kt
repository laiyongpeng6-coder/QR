package com.qrscanmax.core.domain.repository

import com.qrscanmax.core.domain.model.ProductInfo

/**
 * Repository interface for product information lookup.
 *
 * This interface defines the contract for retrieving product details from
 * external databases using barcode values. Implementations follow a cache-first
 * strategy: check local Room cache → query remote API → fallback to cached/error.
 *
 * The cache is considered stale after 7 days and will trigger a refresh from
 * the remote API when network is available.
 *
 * @see ProductInfo
 */
interface ProductRepository {

    /**
     * Looks up product information by barcode string.
     *
     * The implementation should follow this strategy:
     * 1. Check local cache (Room) for a non-stale entry
     * 2. If cache miss or stale, query the remote API
     * 3. On network failure, return cached data if available
     * 4. On complete miss (no cache, no network), return a failure [Result]
     *
     * @param barcode The product barcode string (e.g., EAN-13, UPC-A format).
     * @return A [Result] containing [ProductInfo] on success, or an exception on failure.
     */
    suspend fun lookupProduct(barcode: String): Result<ProductInfo>
}
