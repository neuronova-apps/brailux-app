package com.brailuxaprende.data.seasonal

import androidx.annotation.DrawableRes
import com.brailuxaprende.R

/**
 * Represents the four visual seasonal themes that Brailux can display
 * automatically based on the device's local date.
 *
 * Resolution priority when a seasonal theme is active:
 * 1. High contrast (overrides all — seasonal hidden)
 * 2. Active seasonal theme (this enum)
 * 3. User's selected custom background
 * 4. Default Material background
 */
enum class SeasonalTheme {
    NONE,
    SAN_VALENTIN,
    HALLOWEEN,
    NAVIDAD,
    ANO_NUEVO,
}

/**
 * Drawable resources associated with a seasonal theme.
 *
 * @param backgroundResource Full-screen background image (used in Home, Learn, Play, Progress).
 * @param logoDecorationResource Decorative image displayed centered above/over the Brailux logo
 *   in the Home header. Not interactive; contentDescription must be null.
 * @param dailyPracticeDecorationResource Decorative image shown inside the Daily Practice card
 *   in Home. Not interactive; contentDescription must be null.
 * @param dailyChallengeDecorationResource Decorative image shown inside the Daily Challenge card
 *   in Home. Not interactive; contentDescription must be null.
 */
data class SeasonalThemeResources(
    @param:DrawableRes val backgroundResource: Int,
    @param:DrawableRes val logoDecorationResource: Int,
    @param:DrawableRes val dailyPracticeDecorationResource: Int,
    @param:DrawableRes val dailyChallengeDecorationResource: Int,
)

/**
 * Centralized catalog that maps each [SeasonalTheme] to its exact drawable resources.
 * All resources must already exist in drawable-nodpi; this object never creates them.
 */
object SeasonalThemeCatalog {

    /**
     * Returns the [SeasonalThemeResources] for the given [theme], or null if [theme] is [SeasonalTheme.NONE].
     */
    fun resourcesFor(theme: SeasonalTheme): SeasonalThemeResources? = when (theme) {
        SeasonalTheme.NONE -> null
        SeasonalTheme.SAN_VALENTIN -> SeasonalThemeResources(
            backgroundResource = R.drawable.bg_brailux_san_valentin,
            logoDecorationResource = R.drawable.decor_brailux_corona_san_valentin,
            dailyPracticeDecorationResource = R.drawable.decor_brailux_practica_san_valentin,
            dailyChallengeDecorationResource = R.drawable.decor_brailux_desafio_san_valentin,
        )
        SeasonalTheme.HALLOWEEN -> SeasonalThemeResources(
            backgroundResource = R.drawable.bg_brailux_halloween,
            logoDecorationResource = R.drawable.decor_brailux_sombrero_halloween,
            dailyPracticeDecorationResource = R.drawable.decor_brailux_practica_halloween,
            dailyChallengeDecorationResource = R.drawable.decor_brailux_desafio_halloween,
        )
        SeasonalTheme.NAVIDAD -> SeasonalThemeResources(
            backgroundResource = R.drawable.bg_brailux_navidad,
            logoDecorationResource = R.drawable.decor_brailux_gorro_navidad,
            dailyPracticeDecorationResource = R.drawable.decor_brailux_practica_navidad,
            dailyChallengeDecorationResource = R.drawable.decor_brailux_desafio_navidad,
        )
        SeasonalTheme.ANO_NUEVO -> SeasonalThemeResources(
            backgroundResource = R.drawable.bg_brailux_ano_nuevo,
            logoDecorationResource = R.drawable.decor_brailux_corona_ano_nuevo,
            dailyPracticeDecorationResource = R.drawable.decor_brailux_practica_ano_nuevo,
            dailyChallengeDecorationResource = R.drawable.decor_brailux_desafio_ano_nuevo,
        )
    }
}

/**
 * Detects the active [SeasonalTheme] for a given [AnnualDate].
 *
 * Design principles:
 * - Single Source of Truth: date ranges are defined exclusively in [SeasonalEvents.all].
 *   This detector queries the catalog and returns the associated [SeasonalTheme].
 * - Pure function: accepts the date as a parameter so it is easily unit-testable.
 * - No side effects, no storage, no network access.
 * - The caller (MainActivity) provides [AnnualDate.today] so the app always reflects
 *   the device's current local date on launch or Activity recreation.
 */
object SeasonalThemeDetector {

    /**
     * Returns the [SeasonalTheme] active on [date], derived from [events].
     *
     * Official date ranges (defined in [SeasonalEvents.all]):
     * - SAN_VALENTIN : 1 Feb – 15 Feb
     * - HALLOWEEN    : 15 Oct – 2 Nov
     * - NAVIDAD      : 1 Dec – 27 Dec
     * - ANO_NUEVO    : 28 Dec – 4 Jan (crosses year boundary)
     * - NONE         : any other date
     */
    fun resolve(
        date: AnnualDate,
        events: List<SeasonalEvent> = SeasonalEvents.all,
    ): SeasonalTheme {
        return events
            .firstOrNull { event -> event.theme != SeasonalTheme.NONE && event.includes(date) }
            ?.theme ?: SeasonalTheme.NONE
    }
}
