package com.qrscanfast.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a scan/generation history record in the encrypted database.
 *
 * This entity maps to the `history_records` table and corresponds to the domain-layer
 * [com.qrscanfast.core.domain.model.HistoryRecord]. Type converters handle the mapping
 * of [ContentType] and [RecordSource] enums to their string representations for storage.
 *
 * The table is stored in an SQLCipher-encrypted database to protect user history data at rest.
 *
 * @property id Auto-generated primary key. Pass 0 for new records to let Room assign an ID.
 * @property contentType String representation of the classified content type (e.g., "URL", "WIFI").
 * @property rawContent The raw decoded string content of the barcode/QR code.
 * @property displayTitle A human-readable title derived from the content for list display.
 * @property timestamp Epoch milliseconds when the scan or generation occurred.
 * @property source String representation of the record source ("SCAN" or "GENERATED").
 * @property isFavorite Whether the user has pinned/favorited this record.
 * @property thumbnailPath Optional file path to a cached thumbnail image of the generated QR code.
 *
 * @see com.qrscanfast.core.data.database.converter.Converters
 * @see com.qrscanfast.core.domain.model.HistoryRecord
 */
@Entity(tableName = "history_records")
data class HistoryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "content_type")
    val contentType: String,

    @ColumnInfo(name = "raw_content")
    val rawContent: String,

    @ColumnInfo(name = "display_title")
    val displayTitle: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null
)
