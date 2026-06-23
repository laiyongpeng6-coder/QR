package com.qrscanfast.core.billing

import javax.inject.Qualifier

/**
 * Qualifier annotation for the CoroutineScope used within the billing module.
 *
 * This scope is tied to the application lifecycle and provides a dispatcher
 * for background billing operations (state derivation, purchase monitoring).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BillingScope
