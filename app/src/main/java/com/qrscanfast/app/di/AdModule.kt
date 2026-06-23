package com.qrscanfast.app.di

import com.qrscanfast.core.ads.AdUnitIdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the [AdUnitIdProvider] binding.
 *
 * This bridges the app module's BuildConfig ad unit IDs into the
 * :core:ads module via dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdModule {

    @Binds
    @Singleton
    abstract fun bindAdUnitIdProvider(impl: AdUnitIdProviderImpl): AdUnitIdProvider
}
