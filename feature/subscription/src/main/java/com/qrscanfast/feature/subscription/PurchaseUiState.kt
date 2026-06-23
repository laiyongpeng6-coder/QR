package com.qrscanfast.feature.subscription

/**
 * Represents the current state of the purchase flow in the UI layer.
 *
 * Used by [SubscriptionViewModel] to drive UI feedback during
 * purchase, restore, and error scenarios.
 */
sealed interface PurchaseUiState {

    /** No purchase operation in progress. */
    data object Idle : PurchaseUiState

    /** A purchase or restore operation is currently in progress. */
    data object Loading : PurchaseUiState

    /** Purchase completed successfully. */
    data object Success : PurchaseUiState

    /** Purchase was cancelled by the user. */
    data object Cancelled : PurchaseUiState

    /** Purchase failed with an error message. */
    data class Error(val message: String) : PurchaseUiState

    /** Restore purchases completed — no active subscription found. */
    data object RestoreEmpty : PurchaseUiState

    /** Restore purchases completed — subscription restored successfully. */
    data object RestoreSuccess : PurchaseUiState
}
