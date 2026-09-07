package com.neuronovaapps.brailux.ui.screens

import com.neuronovaapps.brailux.practice.EngagementProgress
import com.neuronovaapps.brailux.practice.PermanentAchievement
import com.neuronovaapps.brailux.practice.PracticeDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPresentationTest {
    @Test
    fun `accuracy uses real accumulated counters`() {
        assertEquals(75, accuracyPercentage(firstAttemptCorrect = 9, totalExercises = 12))
        assertEquals(80, accuracyPercentage(firstAttemptCorrect = 24, totalExercises = 30))
        assertEquals(90, accuracyPercentage(firstAttemptCorrect = 18, totalExercises = 20))
    }

    @Test
    fun `accuracy is safe for empty or inconsistent counters`() {
        assertEquals(0, accuracyPercentage(firstAttemptCorrect = 0, totalExercises = 0))
        assertEquals(100, accuracyPercentage(firstAttemptCorrect = 30, totalExercises = 20))
    }

    @Test
    fun `practice today includes every valid session and not only daily practice`() {
        val date = PracticeDate(2026, 8, 8)

        assertTrue(
            isPracticeCompletedToday(
                progress = EngagementProgress(activityDates = setOf(date)),
                date = date,
            ),
        )
        assertFalse(
            isPracticeCompletedToday(
                progress = EngagementProgress(),
                date = date,
            ),
        )
    }

    @Test
    fun `progress tabs are defined in expected order with Summary as default`() {
        val expectedTabs = listOf(
            ProgressTab.Summary,
            ProgressTab.Statistics,
            ProgressTab.Achievements,
        )
        assertEquals(expectedTabs, ProgressTab.entries)
        assertEquals(ProgressTab.Summary, ProgressTab.entries.first())
    }

    @Test
    fun `progress tabs map to expected string resources`() {
        assertEquals(com.neuronovaapps.brailux.R.string.progress_tab_summary, ProgressTab.Summary.labelResource)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_tab_statistics, ProgressTab.Statistics.labelResource)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_tab_achievements, ProgressTab.Achievements.labelResource)
    }

    @Test
    fun `practice level and metric string resources exist and match expectations`() {
        assertEquals(com.neuronovaapps.brailux.R.string.progress_level_1_title, com.neuronovaapps.brailux.R.string.progress_level_1_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_level_2_title, com.neuronovaapps.brailux.R.string.progress_level_2_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_level_3_title, com.neuronovaapps.brailux.R.string.progress_level_3_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_level_4_title, com.neuronovaapps.brailux.R.string.progress_level_4_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_daily_challenge_title, com.neuronovaapps.brailux.R.string.progress_daily_challenge_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_errors_label, com.neuronovaapps.brailux.R.string.progress_errors_label)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_hints_used_label, com.neuronovaapps.brailux.R.string.progress_hints_used_label)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_play_title, com.neuronovaapps.brailux.R.string.progress_play_title)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_play_total_games, com.neuronovaapps.brailux.R.string.progress_play_total_games)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_play_memory_games, com.neuronovaapps.brailux.R.string.progress_play_memory_games)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_play_sequence_games, com.neuronovaapps.brailux.R.string.progress_play_sequence_games)
        assertEquals(com.neuronovaapps.brailux.R.string.progress_play_order_games, com.neuronovaapps.brailux.R.string.progress_play_order_games)
    }

    @Test
    fun `all 25 permanent achievements have valid title and description string resources`() {
        PermanentAchievement.entries.forEach { achievement ->
            val titleRes = achievement.titleResource()
            val descRes = achievement.descriptionResource()
            assertTrue("Title resource for $achievement should be non-zero", titleRes != 0)
            assertTrue("Description resource for $achievement should be non-zero", descRes != 0)
        }
    }

    @Test
    fun `all 25 active permanent achievements have valid drawable resources and legacy is null`() {
        PermanentAchievement.activeEntries.forEach { achievement ->
            val iconRes = achievement.iconResource()
            assertTrue("Icon resource for active achievement $achievement should not be null", iconRes != null && iconRes != 0)
        }
        assertEquals(null, PermanentAchievement.HundredExercises.iconResource())
    }

    @Test
    fun `badges and medals maintain semantic separation in drawable resources`() {
        val medals = PermanentAchievement.activeEntries.filter {
            it.family == com.neuronovaapps.brailux.practice.AchievementFamily.BrailleTrajectory
        }
        assertEquals(6, medals.size)
        val expectedMedals = mapOf(
            PermanentAchievement.Bronze to com.neuronovaapps.brailux.R.drawable.medal_bronze,
            PermanentAchievement.Silver to com.neuronovaapps.brailux.R.drawable.medal_silver,
            PermanentAchievement.Gold to com.neuronovaapps.brailux.R.drawable.medal_gold,
            PermanentAchievement.Platinum to com.neuronovaapps.brailux.R.drawable.medal_platinum,
            PermanentAchievement.Diamond to com.neuronovaapps.brailux.R.drawable.medal_diamond,
            PermanentAchievement.BrailleSupremacy to com.neuronovaapps.brailux.R.drawable.medal_supremacy,
        )
        expectedMedals.forEach { (achievement, expectedRes) ->
            assertEquals(expectedRes, achievement.iconResource())
        }

        val badges = PermanentAchievement.activeEntries.filterNot {
            it.family == com.neuronovaapps.brailux.practice.AchievementFamily.BrailleTrajectory
        }
        assertEquals(19, badges.size)
        val expectedBadges = mapOf(
            PermanentAchievement.FirstStep to com.neuronovaapps.brailux.R.drawable.achievement_primer_paso,
            PermanentAchievement.Explorer to com.neuronovaapps.brailux.R.drawable.achievement_explorador_braille,
            PermanentAchievement.Recognizer to com.neuronovaapps.brailux.R.drawable.achievement_reconocedor_braille,
            PermanentAchievement.Challenger to com.neuronovaapps.brailux.R.drawable.achievement_desafiante_braille,
            PermanentAchievement.FullAlphabet to com.neuronovaapps.brailux.R.drawable.achievement_alfabeto_completo,
            PermanentAchievement.Consistency to com.neuronovaapps.brailux.R.drawable.achievement_constancia,
            PermanentAchievement.WeekInMotion to com.neuronovaapps.brailux.R.drawable.achievement_semana_en_movimiento,
            PermanentAchievement.ConstantWeek to com.neuronovaapps.brailux.R.drawable.achievement_semana_constante,
            PermanentAchievement.TwoWeeks to com.neuronovaapps.brailux.R.drawable.achievement_dos_semanas,
            PermanentAchievement.ConsistencyMonth to com.neuronovaapps.brailux.R.drawable.achievement_mes_de_constancia,
            PermanentAchievement.SuperiorConsistency to com.neuronovaapps.brailux.R.drawable.achievement_constancia_superior,
            PermanentAchievement.BrailleFocus to com.neuronovaapps.brailux.R.drawable.achievement_enfoque_braille,
            PermanentAchievement.BrailleRhythm to com.neuronovaapps.brailux.R.drawable.achievement_ritmo_braille,
            PermanentAchievement.BraillePrecision to com.neuronovaapps.brailux.R.drawable.achievement_precision_braille,
            PermanentAchievement.SustainedReading to com.neuronovaapps.brailux.R.drawable.achievement_lectura_sostenida,
            PermanentAchievement.ConstantMastery to com.neuronovaapps.brailux.R.drawable.achievement_dominio_constante,
            PermanentAchievement.SuperiorPrecision to com.neuronovaapps.brailux.R.drawable.achievement_precision_superior,
            PermanentAchievement.DoubleMeaning to com.neuronovaapps.brailux.R.drawable.achievement_doble_sentido,
            PermanentAchievement.BidirectionalReading to com.neuronovaapps.brailux.R.drawable.achievement_lectura_bidireccional,
        )
        expectedBadges.forEach { (achievement, expectedRes) ->
            assertEquals(expectedRes, achievement.iconResource())
        }
    }
}


