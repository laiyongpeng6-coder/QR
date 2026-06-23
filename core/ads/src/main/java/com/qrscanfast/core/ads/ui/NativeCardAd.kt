package com.qrscanfast.core.ads.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.qrscanfast.core.ads.R
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.NativeAdState

/**
 * A Composable that displays a native ad card for the given [placement].
 *
 * Observes the [NativeAdState] from [adManager] and renders:
 * - **Loading**: Hidden (no space occupied, avoids layout jumps)
 * - **Loaded**: A Material3-styled card containing the native ad content
 * - **Failed / Hidden**: Completely hidden, no space occupied
 *
 * This component gracefully handles ad load failures by simply disappearing,
 * ensuring the host screen's flow is never interrupted.
 *
 * @param placement The ad placement scenario (e.g., [AdPlacement.NATIVE_ONBOARDING]).
 * @param adManager The [AdManager] instance to retrieve native ad state from.
 * @param modifier Optional modifier for the outer container.
 */
@Composable
fun NativeCardAd(
    placement: AdPlacement,
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    val adState by adManager.getNativeAdState(placement).collectAsState()

    val isVisible = adState is NativeAdState.Loaded

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        if (adState is NativeAdState.Loaded) {
            val nativeAd = (adState as NativeAdState.Loaded).nativeAd as? NativeAd
            if (nativeAd != null) {
                NativeAdCard(nativeAd = nativeAd)
            }
        }
    }
}

/**
 * Renders the actual native ad content inside a Material3 Card using AndroidView.
 */
@Composable
private fun NativeAdCard(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.native_card_ad_view, null) as NativeAdView
                bindNativeAdView(view, nativeAd)
                view
            },
            update = { view ->
                bindNativeAdView(view, nativeAd)
            }
        )
    }
}

/**
 * Binds a [NativeAd] object to the corresponding views in a [NativeAdView].
 */
private fun bindNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    // Headline
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    headlineView?.text = nativeAd.headline
    adView.headlineView = headlineView

    // Body
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    bodyView?.text = nativeAd.body
    bodyView?.visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
    adView.bodyView = bodyView

    // Icon
    val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
    val icon = nativeAd.icon
    if (icon != null) {
        iconView?.setImageDrawable(icon.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }
    adView.iconView = iconView

    // Call to action
    val ctaButton = adView.findViewById<Button>(R.id.ad_call_to_action)
    if (nativeAd.callToAction != null) {
        ctaButton?.text = nativeAd.callToAction
        ctaButton?.visibility = View.VISIBLE
    } else {
        ctaButton?.visibility = View.GONE
    }
    adView.callToActionView = ctaButton

    // Register the native ad object with the view
    adView.setNativeAd(nativeAd)
}
