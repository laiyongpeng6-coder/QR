package com.qrscanfast.qr.core.domain.repository

import com.qrscanfast.qr.core.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface for managing scan and generation history records.
 *
 * This interface defines the contract for history data access, abstracting
 * the underlying persistence mechanism (Room + SQLCipher). Implementations
 * are provided in the `:core:data` module and injected via Hilt.
 *
 * All query methods return [Flow] for reactive UI updates when data changes.
 *
 * @see HistoryRecord
 */
interface HistoryRepository {

    /**
     * Observes all history records ordered by timestamp descending (newest first).
     *
     * @return A [Flow] emitting the complete list of history records whenever the data changes.
     */
    fun getAllRecords(): Flow<List<HistoryRecord>>

    /**
     * Searches history records by matching the query string against
     * both [HistoryRecord.rawContent] and [HistoryRecord.displayTitle].
     *
     * @param query The search text to match (case-insensitive partial match).
     * @return A [Flow] emitting matching records ordered by timestamp descending.
     */
    fun searchRecords(query: String): Flow<List<HistoryRecord>>

    /**
     * Retrieves history records within a specific date range (inclusive).
     *
     * @param start The start of the date range (inclusive).
     * @param end The end of the date range (inclusive).
     * @return A [Flow] emitting records within the range ordered by timestamp descending.
     */
    fun getRecordsByDateRange(start: Instant, end: Instant): Flow<List<HistoryRecord>>

    /**
     * Inserts a new history record into the database.
     *
     * @param record The [HistoryRecord] to persist. The [HistoryRecord.id] field
     *   should be 0 for auto-generation.
     */
    suspend fun insert(record: HistoryRecord)

    /**
     * Deletes a history record by its unique identifier.
     *
     * @param id The unique ID of the record to delete.
     */
    suspend fun delete(id: Long)

    /**
     * Updates the favorite status of a history record.
     *
     * @param id The unique ID of the record to update.
     * @param isFavorite The new favorite status.
     */
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
