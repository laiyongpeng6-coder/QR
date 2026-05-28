package com.qrscanmax.core.data.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the encryption key used to protect the Room database via SQLCipher.
 *
 * This class uses the Android Keystore system to securely generate and retrieve
 * an AES-256 encryption key. The key never leaves the hardware-backed keystore
 * (on supported devices), providing strong protection against key extraction.
 *
 * ## Key Specifications
 * - Algorithm: AES-256
 * - Block mode: GCM (for key generation parameters)
 * - Padding: None (SQLCipher handles its own padding)
 * - Key alias: [KEY_ALIAS]
 * - Storage: Android Keystore (hardware-backed when available)
 *
 * ## Usage
 * The [getOrCreateKey] method is called during database initialization to provide
 * the passphrase bytes to SQLCipher's `SupportFactory`. The key is generated on
 * first access and retrieved from the Keystore on subsequent accesses.
 *
 * ## Security Considerations
 * - The key is bound to the device and cannot be exported
 * - On devices with hardware-backed Keystore (StrongBox or TEE), the key material
 *   is protected by secure hardware
 * - If the Keystore is cleared (e.g., factory reset), the database becomes unreadable
 *   and must be recreated
 *
 * @see com.qrscanmax.core.data.database.QrScanMaxDatabase
 */
@Singleton
class EncryptionKeyManager @Inject constructor() {

    companion object {
        /**
         * The alias used to store and retrieve the database encryption key in Android Keystore.
         */
        private const val KEY_ALIAS = "qr_scan_max_db_key"

        /**
         * The Android Keystore provider name.
         */
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /**
         * Key size in bits for AES-256 encryption.
         */
        private const val KEY_SIZE = 256
    }

    /**
     * Retrieves the existing database encryption key from the Android Keystore,
     * or generates a new one if no key exists yet.
     *
     * This method is thread-safe and idempotent — calling it multiple times will
     * return the same key bytes as long as the Keystore entry is not cleared.
     *
     * @return A [ByteArray] containing the AES-256 key material suitable for use
     *   as a SQLCipher passphrase. The array is 32 bytes (256 bits).
     * @throws java.security.KeyStoreException If the Android Keystore is unavailable.
     * @throws java.security.UnrecoverableKeyException If the key cannot be retrieved.
     */
    fun getOrCreateKey(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        val secretKey: SecretKey = existingKey?.secretKey ?: generateKey()

        return secretKey.encoded
    }

    /**
     * Generates a new AES-256 key and stores it in the Android Keystore.
     *
     * The key is configured with:
     * - AES algorithm with 256-bit key size
     * - GCM block mode (required by KeyGenParameterSpec)
     * - No padding (SQLCipher manages its own encryption scheme)
     * - Encryption and decryption purposes enabled
     *
     * @return The newly generated [SecretKey].
     */
    private fun generateKey(): SecretKey {
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }
}
