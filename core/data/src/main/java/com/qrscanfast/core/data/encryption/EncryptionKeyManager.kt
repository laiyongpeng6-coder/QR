package com.qrscanfast.core.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理用于 SQLCipher 数据库加密的密钥。
 *
 * ## 重要设计说明（给其他 AI 开发者）
 *
 * Android Keystore 的 hardware-backed key 调用 `secretKey.encoded` 会返回 null，
 * 因此不能直接将 Keystore 中的 key 作为 SQLCipher 的 passphrase。
 *
 * 本类采用的方案是：
 * 1. 生成一个随机的 32 字节 passphrase（用于 SQLCipher）
 * 2. 使用 Android Keystore 中的 AES-GCM key 对该 passphrase 进行加密
 * 3. 将加密后的 passphrase 存储在 SharedPreferences 中
 * 4. 每次需要时，从 SharedPreferences 读取密文，用 Keystore key 解密得到原始 passphrase
 *
 * 这样既利用了 Keystore 的硬件安全保护，又能获得可用的字节数组给 SQLCipher。
 *
 * ## 安全特性
 * - 加密密钥存储在 Android Keystore（硬件安全模块保护）
 * - 实际 passphrase 以加密形式存储，明文仅在内存中短暂存在
 * - 设备恢复出厂设置后，Keystore 被清除，数据库将无法解密（需重建）
 *
 * @param context 应用上下文，用于访问 SharedPreferences
 * @see com.qrscanfast.core.data.database.FastQrScanDatabase
 */
@Singleton
class EncryptionKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** Keystore 中加密密钥的别名 */
        private const val KEY_ALIAS = "fast_qr_scan_db_key"

        /** Android Keystore 提供者名称 */
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /** AES 密钥长度（位） */
        private const val KEY_SIZE = 256

        /** SharedPreferences 文件名，存储加密后的 passphrase */
        private const val PREFS_NAME = "fast_qr_scan_crypto"

        /** 加密后的 passphrase 在 SharedPreferences 中的 key */
        private const val PREF_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"

        /** GCM IV 在 SharedPreferences 中的 key */
        private const val PREF_PASSPHRASE_IV = "db_passphrase_iv"

        /** GCM 认证标签长度（位） */
        private const val GCM_TAG_LENGTH = 128

        /** SQLCipher passphrase 长度（字节），即 32 字节 = 256 位 */
        private const val PASSPHRASE_LENGTH = 32
    }

    /**
     * 获取或创建数据库加密 passphrase。
     *
     * 首次调用时会：
     * 1. 在 Keystore 中生成 AES-256-GCM 密钥
     * 2. 生成随机 32 字节 passphrase
     * 3. 用 Keystore 密钥加密 passphrase 并存储
     *
     * 后续调用时会：
     * 1. 从 SharedPreferences 读取加密的 passphrase
     * 2. 用 Keystore 密钥解密并返回
     *
     * @return 32 字节的 passphrase，可直接传给 SQLCipher 的 SupportFactory
     * @throws java.security.KeyStoreException 如果 Keystore 不可用
     */
    fun getOrCreateKey(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(PREF_PASSPHRASE_IV, null)

        return if (encryptedBase64 != null && ivBase64 != null) {
            // 已有加密的 passphrase，解密后返回
            val encrypted = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
            val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)
            decryptPassphrase(encrypted, iv)
        } else {
            // 首次运行，生成新的 passphrase 并加密存储
            val passphrase = generateRandomPassphrase()
            val (encrypted, iv) = encryptPassphrase(passphrase)

            prefs.edit()
                .putString(PREF_ENCRYPTED_PASSPHRASE, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .putString(PREF_PASSPHRASE_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .apply()

            passphrase
        }
    }

    /**
     * 生成随机的 32 字节 passphrase。
     * 使用 SecureRandom 确保密码学安全的随机性。
     */
    private fun generateRandomPassphrase(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)
        return passphrase
    }

    /**
     * 使用 Keystore 中的 AES-GCM 密钥加密 passphrase。
     *
     * @param passphrase 要加密的原始 passphrase
     * @return Pair<加密后的数据, IV>
     */
    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(passphrase)
        val iv = cipher.iv
        return Pair(encrypted, iv)
    }

    /**
     * 使用 Keystore 中的 AES-GCM 密钥解密 passphrase。
     *
     * @param encrypted 加密后的 passphrase 数据
     * @param iv 加密时使用的初始化向量
     * @return 解密后的原始 passphrase
     */
    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encrypted)
    }

    /**
     * 从 Keystore 获取或创建 AES-256-GCM 密钥。
     * 该密钥仅用于加密/解密 SQLCipher 的 passphrase，不直接用作数据库密钥。
     */
    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // 如果密钥已存在，直接返回
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        // 生成新的 Keystore 密钥
        val keyGenSpec = KeyGenParameterSpec.Builder(
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
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
}
