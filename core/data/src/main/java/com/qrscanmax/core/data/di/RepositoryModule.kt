package com.qrscanmax.core.data.di

import com.qrscanmax.core.data.repository.HistoryRepositoryImpl
import com.qrscanmax.core.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module that binds repository implementations to their interfaces.
 *
 * This module is installed in the [SingletonComponent], ensuring that repository bindings
 * are application-scoped singletons. It uses [Binds] to declare interface-to-implementation
 * mappings without requiring manual instantiation — Hilt resolves the concrete class
 * via its `@Inject` constructor.
 *
 * ## Provided Bindings
 * - [HistoryRepository] → [HistoryRepositoryImpl]: Scan/generation history data access.
 *
 * @see HistoryRepositoryImpl
 * @see HistoryRepository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [HistoryRepositoryImpl] as the concrete implementation of [HistoryRepository].
     *
     * The binding is scoped as [Singleton] to ensure a single repository instance
     * is shared across all consumers (ViewModels, use cases) in the application.
     *
     * @param impl The [HistoryRepositoryImpl] instance constructed by Hilt.
     * @return The [HistoryRepository] interface backed by the Room-based implementation.
     */
    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
