package com.qrscanfast.app.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.qrscanfast.app.R
import com.qrscanfast.core.domain.model.NativeAdState
import kotlinx.coroutines.flow.StateFlow

/**
 * A reusable Composable that renders a native ad card.
 *
 * Observes the given [adState] flow and:
 * - Shows the native ad when state is [NativeAdState.Loaded]
 * - Hides completely (no space) when state is [NativeAdState.Hidden], [NativeAdState.Failed], or [NativeAdState.Loading]
 *
 * @param adState The native ad state flow from [AdManager.getNativeAdState].
 * @param modifier Modifier applied to the ad container.
 * @param maxHeight Maximum height constraint for the ad card (default 80dp).
 */
@Composable
fun NativeCardAd(
    adState: StateFlow<NativeAdState>,
    modifier: Modifier = Modifier,
    maxHeight: Int = 80
) {
    val state by adState.collectAsState()

    val isVisible = state is NativeAdState.Loaded

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val nativeAd = (state as? NativeAdState.Loaded)?.nativeAd as? NativeAd
        if (nativeAd != null) {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight.dp),
                factory = { context ->
                    val view = LayoutInflater.from(context)
                        .inflate(R.layout.native_ad_card, null) as NativeAdView
                    view
                },
                update = { adView ->
                    bindNativeAdView(adView, nativeAd)
                }
            )
        }
    }
}

/**
 * Binds a [NativeAd] object to the [NativeAdView] layout.
 */
private fun bindNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    // Find views
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val iconView = adView.findViewById<ImageView>(R.id.ad_icon)

    // Populate views
    headlineView.text = nativeAd.headline
    bodyView.text = nativeAd.body
    callToActionView.text = nativeAd.callToAction

    val icon = nativeAd.icon
    if (icon != null) {
        iconView.setImageDrawable(icon.drawable)
        iconView.visibility = android.view.View.VISIBLE
    } else {
        iconView.visibility = android.view.View.GONE
    }

    // Register views with NativeAdView
    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = callToActionView
    adView.iconView = iconView

    // Set the native ad
    adView.setNativeAd(nativeAd)
}
