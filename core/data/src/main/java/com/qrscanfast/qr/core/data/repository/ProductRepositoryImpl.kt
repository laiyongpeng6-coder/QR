package com.qrscanfast.qr.core.data.repository

import com.qrscanfast.qr.core.data.database.dao.ProductDao
import com.qrscanfast.qr.core.data.database.entity.CachedProductEntity
import com.qrscanfast.qr.core.domain.model.ProductInfo
import com.qrscanfast.qr.core.domain.repository.ProductRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ProductRepository 的实现类，采用缓存优先策略。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 当前实现仅包含本地缓存逻辑，网络 API 调用部分需要在 feature:product-lookup 模块中
 * 通过 ProductApiService（Retrofit 接口）实现后，再注入到此类中。
 *
 * ## 缓存策略
 * 1. 先查本地 Room 缓存（ProductDao）
 * 2. 如果缓存命中且未过期（< 7 天），直接返回缓存数据
 * 3. 如果缓存过期或未命中，应调用远程 API（当前返回失败，待网络层实现后补充）
 * 4. 网络失败时，如果有过期缓存则返回过期缓存（降级策略）
 * 5. 完全无数据时返回 Result.failure
 *
 * ## 后续开发计划
 * - 需要创建 ProductApiService（Retrofit 接口）
 * - 需要创建 NetworkModule（提供 Retrofit 实例）
 * - 然后将 ProductApiService 注入到本类中，补充网络调用逻辑
 *
 * @param productDao Room DAO，提供本地产品缓存的读写操作
 * @see ProductRepository
 * @see ProductDao
 */
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    companion object {
        /** 缓存有效期：7 天（毫秒） */
        private val CACHE_EXPIRY_MS = TimeUnit.DAYS.toMillis(7)
    }

    /**
     * 根据条码查询产品信息。
     *
     * 当前实现：
     * - 有新鲜缓存 → 返回缓存
     * - 有过期缓存 → 返回过期缓存（降级，因为网络层尚未实现）
     * - 无缓存 → 返回失败
     *
     * TODO [FUTURE-NETWORK]: 网络层实现后，补充 API 调用逻辑
     *
     * @param barcode 产品条码字符串（EAN-13、UPC-A 等格式）
     * @return 成功时返回 ProductInfo，失败时返回异常
     */
    override suspend fun lookupProduct(barcode: String): Result<ProductInfo> {
        // 查询本地缓存
        val cached = productDao.getByBarcode(barcode)

        if (cached != null) {
            // 缓存命中，转换为领域模型返回
            return Result.success(cached.toDomain())
        }

        // TODO [FUTURE-NETWORK]: 这里应该调用 ProductApiService 进行网络查询
        // 网络层实现后，逻辑如下：
        // 1. 调用 apiService.lookupProduct(barcode)
        // 2. 成功则缓存到本地并返回
        // 3. 失败则返回 Result.failure

        // 当前无缓存且无网络层，返回失败
        return Result.failure(
            NoSuchElementException("产品未找到：条码 $barcode 不在本地缓存中，且网络查询功能尚未实现")
        )
    }

    /**
     * 将产品信息保存到本地缓存。
     *
     * 供外部调用（如网络层获取到产品数据后调用此方法缓存）。
     *
     * @param productInfo 要缓存的产品信息
     */
    suspend fun cacheProduct(productInfo: ProductInfo) {
        productDao.insert(productInfo.toEntity())
    }

    /**
     * 将 CachedProductEntity（数据层）转换为 ProductInfo（领域层）。
     */
    private fun CachedProductEntity.toDomain(): ProductInfo {
        return ProductInfo(
            barcode = barcode,
            name = name,
            description = description,
            category = category,
            imageUrl = imageUrl
        )
    }

    /**
     * 将 ProductInfo（领域层）转换为 CachedProductEntity（数据层）。
     */
    private fun ProductInfo.toEntity(): CachedProductEntity {
        return CachedProductEntity(
            barcode = barcode,
            name = name,
            description = description,
            category = category,
            imageUrl = imageUrl,
            cachedAt = System.currentTimeMillis()
        )
    }
}
