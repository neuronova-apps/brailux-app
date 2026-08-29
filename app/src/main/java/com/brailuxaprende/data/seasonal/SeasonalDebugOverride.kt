package com.brailuxaprende.data.seasonal

import com.brailuxaprende.BuildConfig

/**
 * Mecanismo exclusivo de DEBUG para forzar fechas simuladas y probar
 * visualmente los cuatro temas estacionales de Brailux en dispositivos físicos.
 *
 * REGLAS DE SEGURIDAD:
 * 1. En compilaciones RELEASE (BuildConfig.DEBUG == false), [effectiveDate] siempre
 *    devuelve la fecha real del dispositivo [realDate], ignorando [forcedDate].
 * 2. Por defecto [forcedDate] es `null`, conservando el comportamiento automático.
 *
 * PARA PROBAR CADA TEMPORADA EN DEBUG:
 * Cambiar el valor de [forcedDate] en esta clase:
 *
 * - San Valentín : AnnualDate(month = 2, day = 10)
 * - Halloween    : AnnualDate(month = 10, day = 31)
 * - Navidad      : AnnualDate(month = 12, day = 20)
 * - Año Nuevo    : AnnualDate(month = 12, day = 31)
 * - Automático   : null
 */
object SeasonalDebugOverride {

    /**
     * Fecha forzada para pruebas en DEBUG.
     * Dejar en `null` para funcionamiento automático real.
     */
    var forcedDate: AnnualDate? = null

    /**
     * Retorna la fecha efectiva para resolver el tema estacional y los banners.
     * - En compilaciones Release ([isDebug] == false): SIEMPRE retorna [realDate].
     * - En compilaciones Debug ([isDebug] == true): retorna [forcedDate] si no es null, o [realDate].
     */
    fun effectiveDate(
        realDate: AnnualDate,
        isDebug: Boolean = BuildConfig.DEBUG,
    ): AnnualDate {
        return if (isDebug && forcedDate != null) {
            forcedDate!!
        } else {
            realDate
        }
    }
}
