package com.qrscanfast.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.qrscanfast.core.common.RemoteConfigManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FastQrScanApplication : Application() {

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    override fun onCreate() {
        super.onCreate()
        initializeAds()
        remoteConfigManager.initialize()
    }

    private fun initializeAds() {
        MobileAds.initialize(this)

        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf(
                "DEVICE_ID_HERE" // Replace with actual test device ID from logcat
            )
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }
    }
}
