package com.qrscanfast.core.data.di

import com.qrscanfast.core.data.repository.HistoryRepositoryImpl
import com.qrscanfast.core.data.repository.ProductRepositoryImpl
import com.qrscanfast.core.domain.repository.HistoryRepository
import com.qrscanfast.core.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块：将 Repository 实现类绑定到接口。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本模块使用 @Binds 声明接口到实现的映射关系。
 * 如果需要添加新的 Repository 绑定，按照相同模式添加即可：
 * 1. 在 core:domain 中定义接口
 * 2. 在 core:data 中实现该接口（使用 @Inject 构造函数）
 * 3. 在本模块中添加 @Binds 方法
 *
 * @see HistoryRepositoryImpl
 * @see ProductRepositoryImpl
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定 HistoryRepositoryImpl 为 HistoryRepository 接口的实现。
     */
    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    /**
     * 绑定 ProductRepositoryImpl 为 ProductRepository 接口的实现。
     */
    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
}
