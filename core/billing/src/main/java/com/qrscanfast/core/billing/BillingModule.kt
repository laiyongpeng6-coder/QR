package com.qrscanfast.core.billing

import com.qrscanfast.core.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module providing billing-related bindings.
 *
 * Binds [SubscriptionRepositoryImpl] to the [SubscriptionRepository] interface,
 * making it available for injection throughout the app via the domain layer contract.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository
}

/**
 * Hilt module providing concrete instances for the billing module.
 *
 * Provides the [CoroutineScope] used by billing components for background operations
 * such as deriving the isPremium StateFlow.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingProvidesModule {

    @Provides
    @Singleton
    @BillingScope
    fun provideBillingCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
