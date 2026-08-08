package com.brailuxaprende.data.seasonal

import androidx.annotation.StringRes
import com.brailuxaprende.R
import java.util.Calendar

data class AnnualDate(
    val month: Int,
    val day: Int,
) : Comparable<AnnualDate> {
    init {
        require(month in 1..12) { "Month must be between 1 and 12" }
        require(day in 1..daysInMonth(month)) { "Day is not valid for month" }
    }

    override fun compareTo(other: AnnualDate): Int =
        compareValuesBy(this, other, AnnualDate::month, AnnualDate::day)

    companion object {
        fun today(calendar: Calendar = Calendar.getInstance()): AnnualDate = AnnualDate(
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )

        private fun daysInMonth(month: Int): Int = when (month) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}

enum class SeasonalAccent {
    Sky,
    Rose,
    Amber,
    Red,
    Cyan,
    Orange,
    Violet,
    Green,
    Gold,
}

enum class SeasonalVisualState {
    Braille,
    Heart,
    Book,
    Peru,
    WhiteCane,
    Halloween,
    Accessibility,
    Christmas,
    NewYear,
}

data class SeasonalEvent(
    val id: String,
    @param:StringRes val nameResource: Int,
    val start: AnnualDate,
    val end: AnnualDate,
    val priority: Int,
    val accent: SeasonalAccent? = null,
    val visualState: SeasonalVisualState,
    @param:StringRes val messageResource: Int? = null,
) {
    fun includes(date: AnnualDate): Boolean = if (start <= end) {
        date in start..end
    } else {
        date >= start || date <= end
    }
}

object SeasonalEvents {
    val all: List<SeasonalEvent> = listOf(
        SeasonalEvent(
            id = "world_braille_day",
            nameResource = R.string.season_world_braille_day,
            start = AnnualDate(1, 3),
            end = AnnualDate(1, 5),
            priority = 100,
            accent = SeasonalAccent.Sky,
            visualState = SeasonalVisualState.Braille,
            messageResource = R.string.season_message_world_braille_day,
        ),
        SeasonalEvent(
            id = "valentines_day",
            nameResource = R.string.season_valentines_day,
            start = AnnualDate(2, 13),
            end = AnnualDate(2, 15),
            priority = 50,
            accent = SeasonalAccent.Rose,
            visualState = SeasonalVisualState.Heart,
            messageResource = R.string.season_message_valentines_day,
        ),
        SeasonalEvent(
            id = "book_day",
            nameResource = R.string.season_book_day,
            start = AnnualDate(4, 22),
            end = AnnualDate(4, 24),
            priority = 70,
            accent = SeasonalAccent.Amber,
            visualState = SeasonalVisualState.Book,
            messageResource = R.string.season_message_book_day,
        ),
        SeasonalEvent(
            id = "peru_independence",
            nameResource = R.string.season_peru_independence,
            start = AnnualDate(7, 26),
            end = AnnualDate(7, 29),
            priority = 80,
            accent = SeasonalAccent.Red,
            visualState = SeasonalVisualState.Peru,
            messageResource = R.string.season_message_peru_independence,
        ),
        SeasonalEvent(
            id = "white_cane_day",
            nameResource = R.string.season_white_cane_day,
            start = AnnualDate(10, 14),
            end = AnnualDate(10, 16),
            priority = 90,
            accent = SeasonalAccent.Cyan,
            visualState = SeasonalVisualState.WhiteCane,
            messageResource = R.string.season_message_white_cane_day,
        ),
        SeasonalEvent(
            id = "halloween",
            nameResource = R.string.season_halloween,
            start = AnnualDate(10, 29),
            end = AnnualDate(10, 31),
            priority = 40,
            accent = SeasonalAccent.Orange,
            visualState = SeasonalVisualState.Halloween,
            messageResource = R.string.season_message_halloween,
        ),
        SeasonalEvent(
            id = "disability_day",
            nameResource = R.string.season_disability_day,
            start = AnnualDate(12, 2),
            end = AnnualDate(12, 4),
            priority = 95,
            accent = SeasonalAccent.Violet,
            visualState = SeasonalVisualState.Accessibility,
            messageResource = R.string.season_message_disability_day,
        ),
        SeasonalEvent(
            id = "christmas",
            nameResource = R.string.season_christmas,
            start = AnnualDate(12, 20),
            end = AnnualDate(12, 25),
            priority = 60,
            accent = SeasonalAccent.Green,
            visualState = SeasonalVisualState.Christmas,
            messageResource = R.string.season_message_christmas,
        ),
        SeasonalEvent(
            id = "new_year",
            nameResource = R.string.season_new_year,
            start = AnnualDate(12, 30),
            end = AnnualDate(1, 1),
            priority = 85,
            accent = SeasonalAccent.Gold,
            visualState = SeasonalVisualState.NewYear,
            messageResource = R.string.season_message_new_year,
        ),
    )
}

object SeasonalThemeResolver {
    fun activeEvent(
        date: AnnualDate,
        eventsEnabled: Boolean,
        events: List<SeasonalEvent> = SeasonalEvents.all,
    ): SeasonalEvent? {
        if (!eventsEnabled) return null

        return events
            .asSequence()
            .filter { event -> event.includes(date) }
            .maxWithOrNull(compareBy<SeasonalEvent>({ it.priority }, { it.id }))
    }
}
