package com.qrscanfast.qr.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qrscanfast.qr.core.data.database.converter.Converters
import com.qrscanfast.qr.core.data.database.dao.HistoryDao
import com.qrscanfast.qr.core.data.database.dao.ProductDao
import com.qrscanfast.qr.core.data.database.entity.CachedProductEntity
import com.qrscanfast.qr.core.data.database.entity.HistoryRecordEntity

/**
 * Room database for the Fast QR Scan application.
 *
 * This database is encrypted at rest using SQLCipher with an AES-256 key managed
 * by the Android Keystore via [com.qrscanfast.qr.core.data.encryption.EncryptionKeyManager].
 * It stores scan/generation history records and cached product lookup results.
 *
 * ## Entities
 * - [HistoryRecordEntity]: User's scan and generation history (encrypted)
 * - [CachedProductEntity]: Cached product information from external API lookups
 *
 * ## Database Configuration
 * - Version: 1 (initial schema)
 * - Export schema: true (for migration testing)
 * - Encryption: SQLCipher with Android Keystore-managed AES-256 key
 *
 * ## Usage with Hilt
 * The database instance is provided via the `DatabaseModule` Hilt module, which
 * handles SQLCipher passphrase injection and Room builder configuration.
 *
 * @see HistoryDao
 * @see ProductDao
 * @see Converters
 * @see com.qrscanfast.qr.core.data.encryption.EncryptionKeyManager
 */
@Database(
    entities = [
        HistoryRecordEntity::class,
        CachedProductEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FastQrScanDatabase : RoomDatabase() {

    /**
     * Provides access to history record data access operations.
     *
     * @return The [HistoryDao] for querying and modifying history records.
     */
    abstract fun historyDao(): HistoryDao

    /**
     * Provides access to product cache data access operations.
     *
     * @return The [ProductDao] for querying and modifying cached product data.
     */
    abstract fun productDao(): ProductDao

    companion object {
        /**
         * The database file name used when building the Room database instance.
         */
        const val DATABASE_NAME = "fast_qr_scan.db"
    }
}
