package com.qrscanfast.qr.core.data.database.converter

import androidx.room.TypeConverter
import com.qrscanfast.qr.core.domain.model.ContentType
import com.qrscanfast.qr.core.domain.model.RecordSource
import java.time.Instant

/**
 * Room type converters for the Fast QR Scan database.
 *
 * Handles bidirectional conversion between domain types and their database-storable
 * representations. Room uses these converters automatically when reading/writing
 * entities that contain non-primitive types.
 *
 * Supported conversions:
 * - [Instant] ↔ [Long] (epoch milliseconds)
 * - [ContentType] ↔ [String] (enum name)
 * - [RecordSource] ↔ [String] (enum name)
 *
 * @see com.qrscanfast.qr.core.data.database.FastQrScanDatabase
 */
class Converters {

    /**
     * Converts an [Instant] to epoch milliseconds for database storage.
     *
     * @param instant The instant to convert, or null.
     * @return Epoch milliseconds, or null if the input is null.
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    /**
     * Converts epoch milliseconds back to an [Instant].
     *
     * @param epochMilli The epoch milliseconds value, or null.
     * @return The corresponding [Instant], or null if the input is null.
     */
    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.ofEpochMilli(it) }
    }

    /**
     * Converts a [ContentType] enum to its string name for database storage.
     *
     * @param contentType The content type to convert, or null.
     * @return The enum name as a string, or null if the input is null.
     */
    @TypeConverter
    fun fromContentType(contentType: ContentType?): String? {
        return contentType?.name
    }

    /**
     * Converts a string back to a [ContentType] enum value.
     *
     * @param value The string representation of the content type, or null.
     * @return The corresponding [ContentType], or null if the input is null.
     */
    @TypeConverter
    fun toContentType(value: String?): ContentType? {
        return value?.let { ContentType.valueOf(it) }
    }

    /**
     * Converts a [RecordSource] enum to its string name for database storage.
     *
     * @param source The record source to convert, or null.
     * @return The enum name as a string, or null if the input is null.
     */
    @TypeConverter
    fun fromRecordSource(source: RecordSource?): String? {
        return source?.name
    }

    /**
     * Converts a string back to a [RecordSource] enum value.
     *
     * @param value The string representation of the record source, or null.
     * @return The corresponding [RecordSource], or null if the input is null.
     */
    @TypeConverter
    fun toRecordSource(value: String?): RecordSource? {
        return value?.let { RecordSource.valueOf(it) }
    }
}
