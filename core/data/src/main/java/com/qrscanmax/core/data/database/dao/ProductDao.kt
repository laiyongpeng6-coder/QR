package com.qrscanmax.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qrscanmax.core.data.database.entity.CachedProductEntity

/**
 * Data Access Object for product cache operations.
 *
 * Provides queries for the `product_cache` table, supporting the cache-first
 * lookup strategy used by the Product Repository. Cached products are stored
 * with a timestamp to enable staleness checks (7-day expiry).
 *
 * ## Cache Strategy
 * 1. [getByBarcode] checks for a cached entry.
 * 2. If found and fresh (< 7 days old), return cached data.
 * 3. If stale or missing, fetch from API and [insert] the result.
 * 4. [deleteStale] is called periodically to purge expired entries.
 * 5. [deleteByBarcode] removes a specific entry (e.g., on manual refresh).
 *
 * @see com.qrscanmax.core.data.database.entity.CachedProductEntity
 * @see com.qrscanmax.core.data.database.QrScanMaxDatabase
 */
@Dao
interface ProductDao {

    /**
     * Retrieves a cached product by its barcode.
     *
     * Returns the cached entry regardless of staleness — the caller (repository)
     * is responsible for checking [CachedProductEntity.cachedAt] against the
     * 7-day freshness threshold.
     *
     * @param barcode The product barcode string (EAN-13, UPC-A, etc.) to look up.
     * @return The cached product entity, or `null` if not found in cache.
     */
    @Query("SELECT * FROM product_cache WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): CachedProductEntity?

    /**
     * Inserts or replaces a product in the cache.
     *
     * Uses [OnConflictStrategy.REPLACE] so that refreshing a product's data
     * from the API simply overwrites the existing cache entry (including the
     * updated [CachedProductEntity.cachedAt] timestamp).
     *
     * @param product The product entity to cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: CachedProductEntity)

    /**
     * Deletes all cached products older than the specified threshold.
     *
     * Used to purge stale cache entries. The threshold is typically calculated as:
     * `System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)`
     *
     * @param staleThresholdMillis Epoch milliseconds threshold. Products with
     *   [CachedProductEntity.cachedAt] before this time are deleted.
     */
    @Query("DELETE FROM product_cache WHERE cached_at < :staleThresholdMillis")
    suspend fun deleteStale(staleThresholdMillis: Long)

    /**
     * Deletes a specific cached product by its barcode.
     *
     * Useful for forcing a fresh API lookup on the next access, or when
     * the user manually triggers a refresh for a specific product.
     *
     * @param barcode The barcode of the product to remove from cache.
     */
    @Query("DELETE FROM product_cache WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)
}
