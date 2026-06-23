package com.qrscanfast.core.domain.ads

/**
 * Represents an item in a list that may contain either real content or an ad slot.
 *
 * Used by [ListAdInserter] to produce a mixed list suitable for rendering
 * in RecyclerView or LazyColumn with interleaved native ads.
 *
 * @param T The type of the actual content data.
 */
sealed class ListItem<out T> {

    /**
     * A content item wrapping the original data.
     *
     * @property data The original list item data.
     */
    data class Content<T>(val data: T) : ListItem<T>()

    /**
     * A placeholder indicating where a native ad should be rendered.
     */
    data object AdSlot : ListItem<Nothing>()
}

/**
 * Utility that inserts [ListItem.AdSlot] markers into a content list at regular intervals.
 *
 * This enables feature modules to build mixed-content lists for native ad display
 * without knowing anything about the ad SDK. The resulting list preserves the
 * relative order of content items and spaces ad slots evenly.
 *
 * Example: given items `[A, B, C, D, E, F, G]` and interval `3`, the output is:
 * `[Content(A), Content(B), Content(C), AdSlot, Content(D), Content(E), Content(F), AdSlot, Content(G)]`
 *
 * Properties guaranteed by this implementation:
 * - Content items maintain their original relative order.
 * - Between adjacent AdSlots there are exactly [interval] Content items.
 * - The total number of Content items equals the size of the input list.
 *
 * @see ListItem
 */
object ListAdInserter {

    /**
     * Inserts ad slots into the given [items] list at every [interval] content items.
     *
     * @param T The type of items in the list.
     * @param items The original content list.
     * @param interval The number of content items between each ad slot. Must be > 0.
     * @return A list of [ListItem] with [ListItem.AdSlot] inserted at regular intervals.
     * @throws IllegalArgumentException if [interval] is not positive.
     */
    fun <T> insertAds(items: List<T>, interval: Int = 5): List<ListItem<T>> {
        require(interval > 0) { "interval must be positive, was $interval" }

        if (items.isEmpty()) return emptyList()

        val result = mutableListOf<ListItem<T>>()
        items.forEachIndexed { index, item ->
            result.add(ListItem.Content(item))
            // Insert an AdSlot after every `interval` content items (but not at the end)
            if ((index + 1) % interval == 0 && index + 1 < items.size) {
                result.add(ListItem.AdSlot)
            }
        }
        return result
    }
}
