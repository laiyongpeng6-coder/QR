package com.qrscanfast.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Internal state for the advanced feature gate dialog.
 */
private enum class GateDialogState {
    /** Initial prompt asking user to watch an ad. */
    PROMPT,
    /** Ad is currently loading/showing. */
    LOADING,
    /** Ad failed to load, showing retry option. */
    FAILED
}

/**
 * Composable that manages the advanced feature unlock flow.
 *
 * This component renders a dialog flow for unlocking advanced features via ad viewing:
 * 1. Shows a prompt dialog explaining the user needs to watch an ad.
 * 2. On confirm, shows the interstitial ad.
 * 3. On success, calls [onUnlocked] and dismisses.
 * 4. On failure, shows a retry prompt.
 * 5. On cancel, calls [onCancelled] and dismisses.
 *
 * Usage:
 * ```kotlin
 * if (showUnlockDialog) {
 *     AdvancedFeatureUnlockDialog(
 *         activity = activity,
 *         unlockManager = unlockManager,
 *         featureName = "AI Templates",
 *         onUnlocked = { /* proceed with feature */ },
 *         onCancelled = { showUnlockDialog = false }
 *     )
 * }
 * ```
 *
 * @param activity The Activity context required for displaying ads.
 * @param unlockManager The [AdvancedFeatureUnlockManager] instance (typically injected).
 * @param featureName A human-readable name for the feature being unlocked (for display in the dialog).
 * @param onUnlocked Called when the feature is successfully unlocked (ad watched or premium user).
 * @param onCancelled Called when the user dismisses the dialog without watching the ad.
 */
@Composable
fun AdvancedFeatureUnlockDialog(
    activity: Activity,
    unlockManager: AdvancedFeatureUnlockManager,
    featureName: String,
    onUnlocked: () -> Unit,
    onCancelled: () -> Unit
) {
    var dialogState by remember { mutableStateOf(GateDialogState.PROMPT) }
    val scope = rememberCoroutineScope()

    when (dialogState) {
        GateDialogState.PROMPT -> {
            AlertDialog(
                onDismissRequest = onCancelled,
                title = {
                    Text(
                        text = "Unlock Feature",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Watch a short ad to unlock \"$featureName\"",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Premium members can use all features without ads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialogState = GateDialogState.LOADING
                            scope.launch {
                                val result = unlockManager.requestUnlock(activity)
                                when (result) {
                                    UnlockResult.Unlocked,
                                    UnlockResult.AlreadyUnlocked -> onUnlocked()
                                    UnlockResult.AdFailed -> dialogState = GateDialogState.FAILED
                                }
                            }
                        }
                    ) {
                        Text("Watch Ad")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelled) {
                        Text("Cancel")
                    }
                }
            )
        }

        GateDialogState.LOADING -> {
            AlertDialog(
                onDismissRequest = { /* Not dismissible while loading */ },
                title = {
                    Text(
                        text = "Loading Ad...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please wait while the ad loads...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {}
            )
        }

        GateDialogState.FAILED -> {
            AlertDialog(
                onDismissRequest = onCancelled,
                title = {
                    Text(
                        text = "Ad Load Failed",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = "The ad could not be loaded. Please check your network connection and try again.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialogState = GateDialogState.LOADING
                            scope.launch {
                                val result = unlockManager.requestUnlock(activity)
                                when (result) {
                                    UnlockResult.Unlocked,
                                    UnlockResult.AlreadyUnlocked -> onUnlocked()
                                    UnlockResult.AdFailed -> dialogState = GateDialogState.FAILED
                                }
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelled) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
