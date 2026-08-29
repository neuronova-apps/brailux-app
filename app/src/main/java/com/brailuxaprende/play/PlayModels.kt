package com.brailuxaprende.play

import androidx.annotation.StringRes
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson

enum class PlayGame(
    @param:StringRes val titleResource: Int,
    @param:StringRes val descriptionResource: Int,
    @param:StringRes val requirementResource: Int,
) {
    Memory(
        titleResource = R.string.play_game_memory_title,
        descriptionResource = R.string.play_game_memory_description,
        requirementResource = R.string.play_game_memory_requirement,
    ),
    Sequence(
        titleResource = R.string.play_game_sequence_title,
        descriptionResource = R.string.play_game_sequence_description,
        requirementResource = R.string.play_game_sequence_requirement,
    ),
    Order(
        titleResource = R.string.play_game_order_title,
        descriptionResource = R.string.play_game_order_description,
        requirementResource = R.string.play_game_order_requirement,
    ),
}

enum class PlayGameStatus {
    Available,
    Locked,
}

object PlayRepertoireProvider {
    val spanishAlphabetOrder: List<Char> = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toList()

    val spanishCharComparator: Comparator<Char> = Comparator { c1, c2 ->
        val i1 = spanishAlphabetOrder.indexOf(c1.uppercaseChar())
        val i2 = spanishAlphabetOrder.indexOf(c2.uppercaseChar())
        when {
            i1 >= 0 && i2 >= 0 -> i1.compareTo(i2)
            i1 >= 0 -> -1
            i2 >= 0 -> 1
            else -> c1.compareTo(c2)
        }
    }

    val spanishBrailleCharacterComparator: Comparator<BrailleCharacter> =
        Comparator { b1, b2 ->
            spanishCharComparator.compare(b1.printedCharacter, b2.printedCharacter)
        }

    fun getAvailableRepertoire(learningProgress: LearningProgress): List<BrailleCharacter> {
        val completed = learningProgress.completedLessons
        return when {
            LearningLesson.LettersUtoZAndEnye in completed ->
                BrailleRepository.getLevel2Characters()
            LearningLesson.LettersKtoT in completed ->
                BrailleRepository.getLevel1Characters() + BrailleRepository.getLettersKToT()
            LearningLesson.LettersAtoJ in completed ->
                BrailleRepository.getLevel1Characters()
            LearningLesson.Vowels in completed ->
                BrailleRepository.getVowels()
            else ->
                emptyList()
        }
    }

    fun isGameUnlocked(game: PlayGame, learningProgress: LearningProgress): Boolean =
        when (game) {
            PlayGame.Memory -> LearningLesson.Vowels in learningProgress.completedLessons
            PlayGame.Sequence -> LearningLesson.LettersAtoJ in learningProgress.completedLessons
            PlayGame.Order -> LearningLesson.LettersKtoT in learningProgress.completedLessons
        }

    fun gameStatus(game: PlayGame, learningProgress: LearningProgress): PlayGameStatus =
        if (isGameUnlocked(game, learningProgress)) {
            PlayGameStatus.Available
        } else {
            PlayGameStatus.Locked
        }

    fun requiredLessonFor(game: PlayGame): LearningLesson = when (game) {
        PlayGame.Memory -> LearningLesson.Vowels
        PlayGame.Sequence -> LearningLesson.LettersAtoJ
        PlayGame.Order -> LearningLesson.LettersKtoT
    }
}
