package com.qrscanmax.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.qrscanmax.core.data.database.QrScanMaxDatabase
import com.qrscanmax.core.data.database.dao.HistoryDao
import com.qrscanmax.core.data.database.dao.ProductDao
import com.qrscanmax.core.data.encryption.EncryptionKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing database and persistence-related dependencies.
 *
 * This module is installed in the [SingletonComponent], ensuring that the database,
 * DAOs, and DataStore instances are application-scoped singletons. This guarantees
 * a single database connection and consistent state across all features.
 *
 * ## Provided Dependencies
 * - [QrScanMaxDatabase]: SQLCipher-encrypted Room database instance.
 * - [HistoryDao]: DAO for scan/generation history record operations.
 * - [ProductDao]: DAO for product cache operations.
 * - [DataStore]<[Preferences]>: Jetpack DataStore for lightweight key-value preferences
 *   (e.g., onboarding completion state).
 *
 * ## Encryption
 * The database is encrypted using SQLCipher with an AES-256 key managed by
 * [EncryptionKeyManager]. The key is stored in the Android Keystore and never
 * leaves the device's secure hardware (on supported devices).
 *
 * @see QrScanMaxDatabase
 * @see EncryptionKeyManager
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the SQLCipher-encrypted Room database instance.
     *
     * The database is configured with:
     * - SQLCipher encryption via [SupportFactory] using the Keystore-managed passphrase.
     * - Standard Room builder with the application context.
     * - Database file named [QrScanMaxDatabase.DATABASE_NAME].
     *
     * @param context The application context for database file creation.
     * @param encryptionKeyManager Provides the AES-256 passphrase for SQLCipher.
     * @return A singleton [QrScanMaxDatabase] instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptionKeyManager: EncryptionKeyManager
    ): QrScanMaxDatabase {
        val passphrase = encryptionKeyManager.getOrCreateKey()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            QrScanMaxDatabase::class.java,
            QrScanMaxDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .build()
    }

    /**
     * Provides the [HistoryDao] from the database instance.
     *
     * @param database The Room database instance.
     * @return The [HistoryDao] for history record operations.
     */
    @Provides
    fun provideHistoryDao(database: QrScanMaxDatabase): HistoryDao = database.historyDao()

    /**
     * Provides the [ProductDao] from the database instance.
     *
     * @param database The Room database instance.
     * @return The [ProductDao] for product cache operations.
     */
    @Provides
    fun provideProductDao(database: QrScanMaxDatabase): ProductDao = database.productDao()

    /**
     * Provides the Jetpack DataStore instance for application preferences.
     *
     * The DataStore file is named `qr_scan_max_preferences` and stores lightweight
     * key-value pairs such as onboarding completion state and user settings.
     *
     * @param context The application context for DataStore file creation.
     * @return A singleton [DataStore]<[Preferences]> instance.
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("qr_scan_max_preferences")
        }
    }
}
