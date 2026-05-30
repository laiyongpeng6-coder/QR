package com.qrscanfast.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qrscanfast.core.data.database.entity.HistoryRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for history record operations.
 *
 * Provides Flow-based reactive queries for observing history changes in real-time,
 * as well as suspend functions for write operations. All queries operate on the
 * encrypted `history_records` table via SQLCipher.
 *
 * ## Reactive Queries
 * - [getAllRecords]: Observes the full history list in reverse-chronological order.
 * - [searchRecords]: Filters records by content or title matching a search query.
 * - [getRecordsByDateRange]: Filters records within a specific time window.
 *
 * ## Write Operations
 * - [insert]: Adds or replaces a history record.
 * - [deleteById]: Removes a single record by primary key.
 * - [updateFavorite]: Toggles the favorite/pin status of a record.
 *
 * ## One-Shot Queries
 * - [getById]: Retrieves a single record by its primary key.
 *
 * @see com.qrscanfast.core.data.database.entity.HistoryRecordEntity
 * @see com.qrscanfast.core.data.database.FastQrScanDatabase
 */
@Dao
interface HistoryDao {

    /**
     * Retrieves all history records ordered by timestamp descending (newest first).
     *
     * This is a reactive query — the returned [Flow] emits a new list whenever
     * the `history_records` table is modified (insert, update, or delete).
     *
     * @return A [Flow] emitting the complete list of history records in reverse-chronological order.
     */
    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HistoryRecordEntity>>

    /**
     * Searches history records by matching the query string against both
     * [HistoryRecordEntity.rawContent] and [HistoryRecordEntity.displayTitle].
     *
     * The search is case-insensitive and uses SQL LIKE with wildcard wrapping,
     * so partial matches are returned. Results are ordered newest-first.
     *
     * @param query The search text to match against raw content or display title.
     * @return A [Flow] emitting matching records whenever the table or query changes.
     */
    @Query("SELECT * FROM history_records WHERE raw_content LIKE '%' || :query || '%' OR display_title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<HistoryRecordEntity>>

    /**
     * Retrieves history records whose timestamp falls within the specified date range (inclusive).
     *
     * Useful for filtering history by day, week, or custom date range in the UI.
     * Results are ordered newest-first within the range.
     *
     * @param startMillis The start of the date range in epoch milliseconds (inclusive).
     * @param endMillis The end of the date range in epoch milliseconds (inclusive).
     * @return A [Flow] emitting records within the specified time window.
     */
    @Query("SELECT * FROM history_records WHERE timestamp BETWEEN :startMillis AND :endMillis ORDER BY timestamp DESC")
    fun getRecordsByDateRange(startMillis: Long, endMillis: Long): Flow<List<HistoryRecordEntity>>

    /**
     * Inserts a new history record or replaces an existing one with the same primary key.
     *
     * Uses [OnConflictStrategy.REPLACE] to handle re-scans of the same content
     * gracefully — the existing record is updated rather than causing a conflict.
     *
     * @param record The history record entity to insert or replace.
     * @return The row ID of the newly inserted (or replaced) record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HistoryRecordEntity): Long

    /**
     * Deletes a history record by its primary key.
     *
     * This is used by the swipe-to-delete gesture in the History screen.
     * The UI should provide an undo mechanism before calling this permanently.
     *
     * @param id The primary key of the record to delete.
     */
    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Updates the favorite/pin status of a history record.
     *
     * This is used by the swipe-to-favorite gesture in the History screen.
     * Favorited records may be displayed with a pin indicator in the UI.
     *
     * @param id The primary key of the record to update.
     * @param isFavorite The new favorite status (`true` to pin, `false` to unpin).
     */
    @Query("UPDATE history_records SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    /**
     * Retrieves a single history record by its primary key.
     *
     * Used for navigating to the detail screen of a specific history entry.
     *
     * @param id The primary key of the record to retrieve.
     * @return The matching [HistoryRecordEntity], or `null` if no record exists with the given ID.
     */
    @Query("SELECT * FROM history_records WHERE id = :id")
    suspend fun getById(id: Long): HistoryRecordEntity?
}
