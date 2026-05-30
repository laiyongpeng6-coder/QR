package com.qrscanfast.core.data.repository

import com.qrscanfast.core.data.database.dao.HistoryDao
import com.qrscanfast.core.data.database.entity.HistoryRecordEntity
import com.qrscanfast.core.domain.model.ContentType
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.model.RecordSource
import com.qrscanfast.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Implementation of [HistoryRepository] backed by Room persistence via [HistoryDao].
 *
 * This class bridges the domain layer and the data layer by mapping between
 * [HistoryRecord] (domain model) and [HistoryRecordEntity] (Room entity).
 * All reactive queries delegate to the DAO's [Flow]-based methods and transform
 * entity lists into domain model lists using [Flow.map].
 *
 * Injected by Hilt as the concrete binding for [HistoryRepository] across the app.
 *
 * @property historyDao The Room DAO providing direct database access for history records.
 *
 * @see HistoryRepository
 * @see HistoryDao
 * @see HistoryRecordEntity
 */
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    /**
     * Observes all history records ordered by timestamp descending (newest first).
     *
     * Delegates to [HistoryDao.getAllRecords] and maps each emitted entity list
     * to a list of domain [HistoryRecord] instances.
     *
     * @return A [Flow] emitting the complete list of history records whenever the data changes.
     */
    override fun getAllRecords(): Flow<List<HistoryRecord>> {
        return historyDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Searches history records by matching the query string against
     * both raw content and display title fields.
     *
     * Delegates to [HistoryDao.searchRecords] and maps results to domain models.
     *
     * @param query The search text to match (case-insensitive partial match).
     * @return A [Flow] emitting matching records ordered by timestamp descending.
     */
    override fun searchRecords(query: String): Flow<List<HistoryRecord>> {
        return historyDao.searchRecords(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Retrieves history records within a specific date range (inclusive).
     *
     * Converts [Instant] parameters to epoch milliseconds for the DAO query.
     *
     * @param start The start of the date range (inclusive).
     * @param end The end of the date range (inclusive).
     * @return A [Flow] emitting records within the range ordered by timestamp descending.
     */
    override fun getRecordsByDateRange(start: Instant, end: Instant): Flow<List<HistoryRecord>> {
        return historyDao.getRecordsByDateRange(
            startMillis = start.toEpochMilli(),
            endMillis = end.toEpochMilli()
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Inserts a new history record into the database.
     *
     * Converts the domain [HistoryRecord] to a [HistoryRecordEntity] before
     * delegating to [HistoryDao.insert].
     *
     * @param record The [HistoryRecord] to persist.
     */
    override suspend fun insert(record: HistoryRecord) {
        historyDao.insert(record.toEntity())
    }

    /**
     * Deletes a history record by its unique identifier.
     *
     * @param id The unique ID of the record to delete.
     */
    override suspend fun delete(id: Long) {
        historyDao.deleteById(id)
    }

    /**
     * Updates the favorite status of a history record.
     *
     * @param id The unique ID of the record to update.
     * @param isFavorite The new favorite status.
     */
    override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        historyDao.updateFavorite(id, isFavorite)
    }

    /**
     * Maps a [HistoryRecordEntity] to a domain [HistoryRecord].
     *
     * Converts string-based enum fields back to their typed enum counterparts
     * and transforms the epoch-millisecond timestamp to an [Instant].
     */
    private fun HistoryRecordEntity.toDomain(): HistoryRecord {
        return HistoryRecord(
            id = id,
            contentType = ContentType.valueOf(contentType),
            rawContent = rawContent,
            displayTitle = displayTitle,
            timestamp = Instant.ofEpochMilli(timestamp),
            source = RecordSource.valueOf(source),
            isFavorite = isFavorite,
            thumbnailPath = thumbnailPath
        )
    }

    /**
     * Maps a domain [HistoryRecord] to a [HistoryRecordEntity] for persistence.
     *
     * Converts typed enum fields to their string name representations and
     * transforms the [Instant] timestamp to epoch milliseconds.
     */
    private fun HistoryRecord.toEntity(): HistoryRecordEntity {
        return HistoryRecordEntity(
            id = id,
            contentType = contentType.name,
            rawContent = rawContent,
            displayTitle = displayTitle,
            timestamp = timestamp.toEpochMilli(),
            source = source.name,
            isFavorite = isFavorite,
            thumbnailPath = thumbnailPath
        )
    }
}
