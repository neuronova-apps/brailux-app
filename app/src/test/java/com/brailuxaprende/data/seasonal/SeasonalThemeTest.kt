package com.brailuxaprende.data.seasonal

import com.brailuxaprende.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [SeasonalThemeResolver.resolve] and [SeasonalThemeCatalog.resourcesFor].
 *
 * Covers every boundary date specified in the product requirements, plus:
 *   - The Navidad / Año Nuevo boundary (27 dic → NAVIDAD, 28 dic → ANO_NUEVO).
 *   - The cross-year Año Nuevo boundary (31 dic → ANO_NUEVO, 1 ene → ANO_NUEVO, 4 ene → ANO_NUEVO, 5 ene → NONE).
 */
class SeasonalThemeTest {

    // ──────────────────────────────────────────────────────────────
    //  SAN VALENTÍN  —  1 feb → 15 feb
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `31 enero debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 1, day = 31))
    }

    @Test
    fun `1 febrero debe ser SAN_VALENTIN`() {
        assertEquals(SeasonalTheme.SAN_VALENTIN, resolve(month = 2, day = 1))
    }

    @Test
    fun `15 febrero debe ser SAN_VALENTIN`() {
        assertEquals(SeasonalTheme.SAN_VALENTIN, resolve(month = 2, day = 15))
    }

    @Test
    fun `16 febrero debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 2, day = 16))
    }

    // ──────────────────────────────────────────────────────────────
    //  HALLOWEEN  —  15 oct → 2 nov
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `14 octubre debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 10, day = 14))
    }

    @Test
    fun `15 octubre debe ser HALLOWEEN`() {
        assertEquals(SeasonalTheme.HALLOWEEN, resolve(month = 10, day = 15))
    }

    @Test
    fun `31 octubre debe ser HALLOWEEN`() {
        assertEquals(SeasonalTheme.HALLOWEEN, resolve(month = 10, day = 31))
    }

    @Test
    fun `1 noviembre debe ser HALLOWEEN`() {
        assertEquals(SeasonalTheme.HALLOWEEN, resolve(month = 11, day = 1))
    }

    @Test
    fun `2 noviembre debe ser HALLOWEEN`() {
        assertEquals(SeasonalTheme.HALLOWEEN, resolve(month = 11, day = 2))
    }

    @Test
    fun `3 noviembre debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 11, day = 3))
    }

    // ──────────────────────────────────────────────────────────────
    //  NAVIDAD  —  1 dic → 27 dic
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `30 noviembre debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 11, day = 30))
    }

    @Test
    fun `1 diciembre debe ser NAVIDAD`() {
        assertEquals(SeasonalTheme.NAVIDAD, resolve(month = 12, day = 1))
    }

    @Test
    fun `27 diciembre debe ser NAVIDAD`() {
        assertEquals(SeasonalTheme.NAVIDAD, resolve(month = 12, day = 27))
    }

    // ──────────────────────────────────────────────────────────────
    //  AÑO NUEVO  —  28 dic → 4 ene (cruce de año)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `28 diciembre debe ser ANO_NUEVO`() {
        assertEquals(SeasonalTheme.ANO_NUEVO, resolve(month = 12, day = 28))
    }

    @Test
    fun `31 diciembre debe ser ANO_NUEVO`() {
        assertEquals(SeasonalTheme.ANO_NUEVO, resolve(month = 12, day = 31))
    }

    @Test
    fun `1 enero debe ser ANO_NUEVO`() {
        assertEquals(SeasonalTheme.ANO_NUEVO, resolve(month = 1, day = 1))
    }

    @Test
    fun `4 enero debe ser ANO_NUEVO`() {
        assertEquals(SeasonalTheme.ANO_NUEVO, resolve(month = 1, day = 4))
    }

    @Test
    fun `5 enero debe ser NONE`() {
        assertEquals(SeasonalTheme.NONE, resolve(month = 1, day = 5))
    }

    @Test
    fun `5 enero debe ser NONE incluso con World Braille Day activo en SeasonalEvent`() {
        val activeEvent = SeasonalThemeResolver.activeEvent(AnnualDate(1, 5), eventsEnabled = true)
        assertEquals("world_braille_day", activeEvent?.id)
        assertEquals(SeasonalTheme.NONE, resolve(month = 1, day = 5))
    }

    // ──────────────────────────────────────────────────────────────
    //  PRIORIDAD NAVIDAD / AÑO NUEVO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `27 diciembre debe ser NAVIDAD y no ANO_NUEVO`() {
        assertEquals(SeasonalTheme.NAVIDAD, resolve(month = 12, day = 27))
    }

    @Test
    fun `28 diciembre debe ser ANO_NUEVO y no NAVIDAD`() {
        assertEquals(SeasonalTheme.ANO_NUEVO, resolve(month = 12, day = 28))
    }

    // ──────────────────────────────────────────────────────────────
    //  CATÁLOGO DE RECURSOS  —  SeasonalThemeCatalog
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `NONE no debe tener recursos`() {
        assertNull(SeasonalThemeCatalog.resourcesFor(SeasonalTheme.NONE))
    }

    @Test
    fun `SAN_VALENTIN tiene los recursos exactos`() {
        val resources = SeasonalThemeCatalog.resourcesFor(SeasonalTheme.SAN_VALENTIN)
        assertNotNull(resources)
        assertEquals(R.drawable.bg_brailux_san_valentin, resources!!.backgroundResource)
        assertEquals(R.drawable.decor_brailux_corona_san_valentin, resources.logoDecorationResource)
        assertEquals(R.drawable.decor_brailux_practica_san_valentin, resources.dailyPracticeDecorationResource)
        assertEquals(R.drawable.decor_brailux_desafio_san_valentin, resources.dailyChallengeDecorationResource)
    }

    @Test
    fun `HALLOWEEN tiene los recursos exactos`() {
        val resources = SeasonalThemeCatalog.resourcesFor(SeasonalTheme.HALLOWEEN)
        assertNotNull(resources)
        assertEquals(R.drawable.bg_brailux_halloween, resources!!.backgroundResource)
        assertEquals(R.drawable.decor_brailux_sombrero_halloween, resources.logoDecorationResource)
        assertEquals(R.drawable.decor_brailux_practica_halloween, resources.dailyPracticeDecorationResource)
        assertEquals(R.drawable.decor_brailux_desafio_halloween, resources.dailyChallengeDecorationResource)
    }

    @Test
    fun `NAVIDAD tiene los recursos exactos`() {
        val resources = SeasonalThemeCatalog.resourcesFor(SeasonalTheme.NAVIDAD)
        assertNotNull(resources)
        assertEquals(R.drawable.bg_brailux_navidad, resources!!.backgroundResource)
        assertEquals(R.drawable.decor_brailux_gorro_navidad, resources.logoDecorationResource)
        assertEquals(R.drawable.decor_brailux_practica_navidad, resources.dailyPracticeDecorationResource)
        assertEquals(R.drawable.decor_brailux_desafio_navidad, resources.dailyChallengeDecorationResource)
    }

    @Test
    fun `ANO_NUEVO tiene los recursos exactos`() {
        val resources = SeasonalThemeCatalog.resourcesFor(SeasonalTheme.ANO_NUEVO)
        assertNotNull(resources)
        assertEquals(R.drawable.bg_brailux_ano_nuevo, resources!!.backgroundResource)
        assertEquals(R.drawable.decor_brailux_corona_ano_nuevo, resources.logoDecorationResource)
        assertEquals(R.drawable.decor_brailux_practica_ano_nuevo, resources.dailyPracticeDecorationResource)
        assertEquals(R.drawable.decor_brailux_desafio_ano_nuevo, resources.dailyChallengeDecorationResource)
    }

    // ──────────────────────────────────────────────────────────────
    //  OVERRIDE DE DEBUG  —  SeasonalDebugOverride
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `SeasonalDebugOverride sin fecha forzada retorna la fecha real`() {
        SeasonalDebugOverride.forcedDate = null
        val realDate = AnnualDate(month = 6, day = 15)
        val effective = SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)
        assertEquals(realDate, effective)
    }

    @Test
    fun `SeasonalDebugOverride con fecha forzada en modo DEBUG retorna la fecha simulada`() {
        val simulated = AnnualDate(month = 10, day = 31)
        SeasonalDebugOverride.forcedDate = simulated
        val realDate = AnnualDate(month = 6, day = 15)
        val effective = SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)
        assertEquals(simulated, effective)
        assertEquals(SeasonalTheme.HALLOWEEN, SeasonalThemeDetector.resolve(effective))
        SeasonalDebugOverride.forcedDate = null
    }

    @Test
    fun `SeasonalDebugOverride en modo RELEASE siempre ignora la fecha forzada y usa la real`() {
        val simulated = AnnualDate(month = 12, day = 25)
        SeasonalDebugOverride.forcedDate = simulated
        val realDate = AnnualDate(month = 6, day = 15)
        val effective = SeasonalDebugOverride.effectiveDate(realDate, isDebug = false)
        assertEquals(realDate, effective)
        assertEquals(SeasonalTheme.NONE, SeasonalThemeDetector.resolve(effective))
        SeasonalDebugOverride.forcedDate = null
    }

    @Test
    fun `SeasonalDebugOverride permite probar los cuatro temas estacionales en DEBUG`() {
        val realDate = AnnualDate(month = 6, day = 15)

        // San Valentín
        SeasonalDebugOverride.forcedDate = AnnualDate(month = 2, day = 10)
        assertEquals(
            SeasonalTheme.SAN_VALENTIN,
            SeasonalThemeDetector.resolve(SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)),
        )

        // Halloween
        SeasonalDebugOverride.forcedDate = AnnualDate(month = 10, day = 31)
        assertEquals(
            SeasonalTheme.HALLOWEEN,
            SeasonalThemeDetector.resolve(SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)),
        )

        // Navidad
        SeasonalDebugOverride.forcedDate = AnnualDate(month = 12, day = 20)
        assertEquals(
            SeasonalTheme.NAVIDAD,
            SeasonalThemeDetector.resolve(SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)),
        )

        // Año Nuevo
        SeasonalDebugOverride.forcedDate = AnnualDate(month = 12, day = 31)
        assertEquals(
            SeasonalTheme.ANO_NUEVO,
            SeasonalThemeDetector.resolve(SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)),
        )

        // Restaurar estado limpio
        SeasonalDebugOverride.forcedDate = null
        assertEquals(
            SeasonalTheme.NONE,
            SeasonalThemeDetector.resolve(SeasonalDebugOverride.effectiveDate(realDate, isDebug = true)),
        )
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper
    // ──────────────────────────────────────────────────────────────

    private fun resolve(month: Int, day: Int): SeasonalTheme =
        SeasonalThemeDetector.resolve(AnnualDate(month, day))
}
