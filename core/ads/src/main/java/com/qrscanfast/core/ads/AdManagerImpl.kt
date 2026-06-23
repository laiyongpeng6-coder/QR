package com.qrscanfast.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.ads.FrequencyController
import com.qrscanfast.core.common.RemoteConfigManager
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.AdShowResult
import com.qrscanfast.core.domain.model.AdType
import com.qrscanfast.core.domain.model.NativeAdState
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production implementation of [AdManager].
 *
 * Manages full-screen ads (app open and interstitial) and native ads with:
 * - Premium user bypass (no ads shown for subscribers)
 * - Frequency control for interstitials
 * - 10-second timeout for ad loading
 * - Graceful degradation on load failure
 * - Per-placement native ad state management via StateFlow
 */
@Singleton
class AdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    private val frequencyController: FrequencyController,
    private val adUnitIdProvider: AdUnitIdProvider,
    private val remoteConfig: RemoteConfigManager
) : AdManager {

    companion object {
        private const val AD_LOAD_TIMEOUT_MS = 10_000L
    }

    /** Cached loaded interstitial ads keyed by placement. */
    private val interstitialCache = mutableMapOf<AdPlacement, InterstitialAd>()

    /** Cached loaded app open ads keyed by placement. */
    private val appOpenCache = mutableMapOf<AdPlacement, AppOpenAd>()

    /** Per-placement native ad state flows. Thread-safe map for concurrent access. */
    private val nativeAdStates = ConcurrentHashMap<AdPlacement, MutableStateFlow<NativeAdState>>()

    // ─── shouldShowAd ────────────────────────────────────────────────────────────

    override fun shouldShowAd(placement: AdPlacement): Boolean {
        // Remote Config 全局广告开关
        if (!remoteConfig.adsEnabled) return false

        // Premium users never see ads
        if (subscriptionRepository.isPremium.value) return false

        // Interstitial ads are additionally gated by frequency control
        if (placement.type == AdType.INTERSTITIAL) {
            return frequencyController.canShowInterstitial()
        }

        return true
    }

    // ─── preload ─────────────────────────────────────────────────────────────────

    override suspend fun preload(placement: AdPlacement) {
        // Don't preload for premium users
        if (subscriptionRepository.isPremium.value) return

        when (placement.type) {
            AdType.APP_OPEN -> preloadAppOpenAd(placement)
            AdType.INTERSTITIAL -> preloadInterstitialAd(placement)
            AdType.NATIVE -> requestNativeAd(placement)
        }
    }

    private suspend fun preloadAppOpenAd(placement: AdPlacement) {
        val adUnitId = adUnitIdProvider.getAdUnitId(placement)
        suspendCancellableCoroutine { continuation ->
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                adUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenCache[placement] = ad
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            )
        }
    }

    private suspend fun preloadInterstitialAd(placement: AdPlacement) {
        val adUnitId = adUnitIdProvider.getAdUnitId(placement)
        suspendCancellableCoroutine { continuation ->
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialCache[placement] = ad
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            )
        }
    }

    // ─── showFullScreenAd ────────────────────────────────────────────────────────

    override suspend fun showFullScreenAd(
        activity: Activity,
        placement: AdPlacement
    ): AdShowResult {
        // Check premium status first
        if (subscriptionRepository.isPremium.value) {
            return AdShowResult.PremiumUser
        }

        // Check frequency control for interstitials
        if (placement.type == AdType.INTERSTITIAL && !frequencyController.canShowInterstitial()) {
            return AdShowResult.FrequencyLimited
        }

        return try {
            withTimeout(AD_LOAD_TIMEOUT_MS) {
                when (placement.type) {
                    AdType.APP_OPEN -> showAppOpenAd(activity, placement)
                    AdType.INTERSTITIAL -> showInterstitialAd(activity, placement)
                    else -> AdShowResult.LoadFailed
                }
            }
        } catch (_: TimeoutCancellationException) {
            AdShowResult.LoadFailed
        }
    }

    private suspend fun showAppOpenAd(
        activity: Activity,
        placement: AdPlacement
    ): AdShowResult {
        // Try to use a cached ad or load a new one
        val ad = appOpenCache.remove(placement) ?: loadAppOpenAd(placement)
            ?: return AdShowResult.LoadFailed

        return suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (continuation.isActive) continuation.resume(AdShowResult.Shown)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    if (continuation.isActive) continuation.resume(AdShowResult.LoadFailed)
                }
            }
            ad.show(activity)
        }
    }

    private suspend fun showInterstitialAd(
        activity: Activity,
        placement: AdPlacement
    ): AdShowResult {
        // Try to use a cached ad or load a new one
        val ad = interstitialCache.remove(placement) ?: loadInterstitialAd(placement)
            ?: return AdShowResult.LoadFailed

        val result = suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (continuation.isActive) continuation.resume(AdShowResult.Shown)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    if (continuation.isActive) continuation.resume(AdShowResult.LoadFailed)
                }
            }
            ad.show(activity)
        }

        // Record the show for frequency control
        if (result == AdShowResult.Shown) {
            frequencyController.recordInterstitialShow()
        }

        return result
    }

    // ─── Ad loading helpers ──────────────────────────────────────────────────────

    private suspend fun loadAppOpenAd(placement: AdPlacement): AppOpenAd? {
        val adUnitId = adUnitIdProvider.getAdUnitId(placement)
        return suspendCancellableCoroutine { continuation ->
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                adUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }
    }

    private suspend fun loadInterstitialAd(placement: AdPlacement): InterstitialAd? {
        val adUnitId = adUnitIdProvider.getAdUnitId(placement)
        return suspendCancellableCoroutine { continuation ->
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }
    }

    // ─── getNativeAdState ───────────────────────────────────────────────────────

    override fun getNativeAdState(placement: AdPlacement): StateFlow<NativeAdState> {
        // Premium users never see native ads — return Hidden immediately
        if (subscriptionRepository.isPremium.value) {
            return MutableStateFlow(NativeAdState.Hidden).asStateFlow()
        }

        // Get or create the state flow for this placement, triggering a load if new
        val stateFlow = nativeAdStates.getOrPut(placement) {
            MutableStateFlow<NativeAdState>(NativeAdState.Loading).also {
                // Trigger ad loading on first access
                loadNativeAd(placement, it)
            }
        }

        return stateFlow.asStateFlow()
    }

    /**
     * Requests a native ad load for the given [placement].
     *
     * This can be called to refresh or retry loading a native ad.
     * Sets state to [NativeAdState.Loading] then loads via AdMob [AdLoader].
     * On success, emits [NativeAdState.Loaded]; on failure, emits [NativeAdState.Failed].
     */
    fun requestNativeAd(placement: AdPlacement) {
        // Premium users — ensure state is Hidden
        if (subscriptionRepository.isPremium.value) {
            nativeAdStates[placement]?.value = NativeAdState.Hidden
            return
        }

        val stateFlow = nativeAdStates.getOrPut(placement) {
            MutableStateFlow(NativeAdState.Loading)
        }

        stateFlow.value = NativeAdState.Loading
        loadNativeAd(placement, stateFlow)
    }

    /**
     * Internal helper that builds and fires the AdMob [AdLoader] for a native ad.
     * Updates [stateFlow] based on load result.
     */
    private fun loadNativeAd(placement: AdPlacement, stateFlow: MutableStateFlow<NativeAdState>) {
        val adUnitId = adUnitIdProvider.getAdUnitId(placement)

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // Ad loaded successfully — expose via state (typed as Any in domain layer)
                stateFlow.value = NativeAdState.Loaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Load failed — UI layer will hide the ad area based on Failed state
                    stateFlow.value = NativeAdState.Failed
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}
