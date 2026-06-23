package com.qrscanfast.core.ads

import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.ads.FrequencyController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing ad-related bindings for the :core:ads module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAdManager(
        impl: AdManagerImpl
    ): AdManager

    @Binds
    @Singleton
    abstract fun bindFrequencyController(
        impl: FrequencyControllerImpl
    ): FrequencyController

    @Binds
    abstract fun bindClock(
        impl: SystemClock
    ): Clock
}
