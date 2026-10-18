package com.solthariel.pawchan.catalog

internal data class CatalogThreadAgeUi(
    val createdAtEpochSeconds: Long? = null,
    val text: String = "",
)

internal fun currentEpochSeconds(): Long = System.currentTimeMillis() / MILLIS_PER_SECOND

internal fun catalogThreadAge(
    createdAtEpochSeconds: Long?,
    nowEpochSeconds: Long,
): CatalogThreadAgeUi =
    CatalogThreadAgeUi(
        createdAtEpochSeconds = createdAtEpochSeconds,
        text = relativeThreadAge(createdAtEpochSeconds, nowEpochSeconds),
    )

/** Uses the OP timestamp, never the last reply date; unknown time is not guessed. */
internal fun relativeThreadAge(
    createdAtEpochSeconds: Long?,
    nowEpochSeconds: Long,
): String {
    if (createdAtEpochSeconds == null || createdAtEpochSeconds <= 0L) return ""
    val elapsed = if (createdAtEpochSeconds >= nowEpochSeconds) 0L else nowEpochSeconds - createdAtEpochSeconds
    return when {
        elapsed < MINUTE_SECONDS -> {
            "только что"
        }

        elapsed < HOUR_SECONDS -> {
            "${elapsed / MINUTE_SECONDS} мин назад"
        }

        elapsed < DAY_SECONDS -> {
            "${elapsed / HOUR_SECONDS} ч назад"
        }

        elapsed < WEEK_SECONDS -> {
            "${elapsed / DAY_SECONDS} дн назад"
        }

        elapsed < MONTH_SECONDS -> {
            "${elapsed / WEEK_SECONDS} нед назад"
        }

        elapsed < YEAR_SECONDS -> {
            "${elapsed / MONTH_SECONDS} мес назад"
        }

        else -> {
            val years = elapsed / YEAR_SECONDS
            "$years ${yearWord(years)} назад"
        }
    }
}

private fun yearWord(years: Long): String =
    when {
        years % HUNDRED in ELEVEN..FOURTEEN -> "лет"
        years % TEN == 1L -> "год"
        years % TEN in 2L..LAST_FEW_YEAR -> "года"
        else -> "лет"
    }

private const val MILLIS_PER_SECOND = 1000L
private const val MINUTE_SECONDS = 60L
private const val HOUR_SECONDS = 60L * MINUTE_SECONDS
private const val DAY_SECONDS = 24L * HOUR_SECONDS
private const val WEEK_SECONDS = 7L * DAY_SECONDS
private const val MONTH_SECONDS = 30L * DAY_SECONDS
private const val YEAR_SECONDS = 365L * DAY_SECONDS
private const val TEN = 10L
private const val HUNDRED = 100L
private const val ELEVEN = 11L
private const val FOURTEEN = 14L

private const val LAST_FEW_YEAR = 4L
