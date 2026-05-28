package com.qrscanmax.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Locale-aware date formatting utilities for the QR Scan Max app.
 *
 * Provides relative time formatting (e.g., "2 minutes ago", "Yesterday")
 * and date header formatting for history grouping.
 */
object DateFormatUtils {

    /**
     * Formats an [Instant] as a relative time string for display in lists.
     *
     * Rules:
     * - Less than 1 minute ago → "Just now"
     * - Less than 60 minutes ago → "X minutes ago"
     * - Less than 24 hours ago → "X hours ago"
     * - Yesterday → "Yesterday"
     * - Within the current year → locale-formatted date without year (e.g., "Mar 15")
     * - Older → locale-formatted date with year (e.g., "Mar 15, 2023")
     *
     * @param instant The timestamp to format.
     * @param now The current time for relative calculations. Defaults to [Instant.now].
     * @param locale The locale for formatting. Defaults to system default.
     * @param zoneId The timezone for date calculations. Defaults to system default.
     * @return A human-readable relative time string.
     */
    fun formatRelativeTime(
        instant: Instant,
        now: Instant = Instant.now(),
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
        val hoursAgo = ChronoUnit.HOURS.between(instant, now)

        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "$minutesAgo minutes ago"
            hoursAgo < 24 -> "$hoursAgo hours ago"
            isYesterday(instant, now, zoneId) -> "Yesterday"
            isSameYear(instant, now, zoneId) -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
                instant.atZone(zoneId).format(formatter)
            }
            else -> {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(locale)
                instant.atZone(zoneId).format(formatter)
            }
        }
    }

    /**
     * Formats an [Instant] as a date header string for history grouping.
     *
     * Returns "Today", "Yesterday", or a locale-formatted date string
     * suitable for section headers in a timeline view.
     *
     * @param instant The timestamp to format as a date header.
     * @param now The current time for relative calculations. Defaults to [Instant.now].
     * @param locale The locale for formatting. Defaults to system default.
     * @param zoneId The timezone for date calculations. Defaults to system default.
     * @return A date header string (e.g., "Today", "Yesterday", "March 15, 2024").
     */
    fun formatDateHeader(
        instant: Instant,
        now: Instant = Instant.now(),
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return when {
            isToday(instant, now, zoneId) -> "Today"
            isYesterday(instant, now, zoneId) -> "Yesterday"
            else -> {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(locale)
                instant.atZone(zoneId).format(formatter)
            }
        }
    }

    /**
     * Extracts the [LocalDate] from an [Instant] for grouping purposes.
     *
     * @param instant The timestamp to extract the date from.
     * @param zoneId The timezone for conversion. Defaults to system default.
     * @return The local date corresponding to the instant.
     */
    fun toLocalDate(
        instant: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        return instant.atZone(zoneId).toLocalDate()
    }

    private fun isToday(instant: Instant, now: Instant, zoneId: ZoneId): Boolean {
        val instantDate = instant.atZone(zoneId).toLocalDate()
        val nowDate = now.atZone(zoneId).toLocalDate()
        return instantDate == nowDate
    }

    private fun isYesterday(instant: Instant, now: Instant, zoneId: ZoneId): Boolean {
        val instantDate = instant.atZone(zoneId).toLocalDate()
        val nowDate = now.atZone(zoneId).toLocalDate()
        return instantDate == nowDate.minusDays(1)
    }

    private fun isSameYear(instant: Instant, now: Instant, zoneId: ZoneId): Boolean {
        val instantYear = instant.atZone(zoneId).year
        val nowYear = now.atZone(zoneId).year
        return instantYear == nowYear
    }
}
