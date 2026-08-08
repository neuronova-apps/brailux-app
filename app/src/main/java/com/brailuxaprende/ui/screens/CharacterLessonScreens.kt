package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.learning.LearningPath
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard

private const val CHARACTER_PRESENTATION_STAGE = 0
private const val CHARACTER_PRACTICE_STAGE = 1
private const val CHARACTER_COMPLETED_STAGE = 2

@Composable
fun VowelsLessonScreen(
    onCompleted: () -> Unit,
    onNextLesson: () -> Unit,
    onBackToLearn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GuidedCharacterLessonScreen(
        lessonNumber = 2,
        title = stringResource(R.string.learning_lesson_2_title),
        characters = LearningPath.vowels,
        practiceCharacters = LearningPath.vowels.map(BrailleCharacter::printedCharacter),
        explanation = { character -> stringResource(vowelExplanationResource(character)) },
        groupLabel = { stringResource(R.string.vowels_group_label) },
        nextLessonLabel = stringResource(R.string.lesson_next_letters_a_j),
        onCompleted = onCompleted,
        onNextLesson = onNextLesson,
        onBackToLearn = onBackToLearn,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun LettersAtoJLessonScreen(
    onCompleted: () -> Unit,
    onBackToLearn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val characters = LearningPath.lettersAtoJ
    GuidedCharacterLessonScreen(
        lessonNumber = 3,
        title = stringResource(R.string.learning_lesson_3_title),
        characters = characters,
        practiceCharacters = listOf('A', 'C', 'F', 'H', 'J'),
        explanation = { character -> stringResource(letterComparisonResource(character)) },
        groupLabel = { index ->
            stringResource(
                if (index < 5) R.string.letters_group_a_e else R.string.letters_group_f_j,
            )
        },
        nextLessonLabel = null,
        onCompleted = onCompleted,
        onNextLesson = null,
        onBackToLearn = onBackToLearn,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GuidedCharacterLessonScreen(
    lessonNumber: Int,
    title: String,
    characters: List<BrailleCharacter>,
    practiceCharacters: List<Char>,
    explanation: @Composable (Char) -> String,
    groupLabel: @Composable (Int) -> String,
    nextLessonLabel: String?,
    onCompleted: () -> Unit,
    onNextLesson: (() -> Unit)?,
    onBackToLearn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stage by rememberSaveable { mutableIntStateOf(CHARACTER_PRESENTATION_STAGE) }
    var characterIndex by rememberSaveable { mutableIntStateOf(0) }
    var practiceIndex by rememberSaveable { mutableIntStateOf(0) }

    fun repeatLesson() {
        stage = CHARACTER_PRESENTATION_STAGE
        characterIndex = 0
        practiceIndex = 0
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = title,
                subtitle = stringResource(R.string.learning_lesson_number, lessonNumber),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(22.dp))

            when (stage) {
                CHARACTER_PRESENTATION_STAGE -> {
                    val character = characters[characterIndex]
                    CharacterPresentationCard(
                        character = character,
                        position = characterIndex + 1,
                        total = characters.size,
                        group = groupLabel(characterIndex),
                        explanation = explanation(character.printedCharacter),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BrailuxSecondaryButton(
                            text = stringResource(R.string.lesson_previous),
                            onClick = { characterIndex-- },
                            enabled = characterIndex > 0,
                            modifier = Modifier.weight(1f),
                        )
                        BrailuxPrimaryButton(
                            text = stringResource(
                                if (characterIndex == characters.lastIndex) {
                                    R.string.lesson_start_guided_practice
                                } else {
                                    R.string.lesson_next
                                },
                            ),
                            onClick = {
                                if (characterIndex == characters.lastIndex) {
                                    stage = CHARACTER_PRACTICE_STAGE
                                } else {
                                    characterIndex++
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                CHARACTER_PRACTICE_STAGE -> {
                    val practiceCharacter = practiceCharacters[practiceIndex]
                    Text(
                        text = stringResource(
                            R.string.lesson_practice_progress,
                            practiceIndex + 1,
                            practiceCharacters.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.lesson_form_character,
                            practiceCharacter.toString(),
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    GuidedBrailleExercise(
                        character = practiceCharacter,
                        continueLabel = stringResource(
                            if (practiceIndex == practiceCharacters.lastIndex) {
                                R.string.lesson_finish
                            } else {
                                R.string.practice_next_exercise
                            },
                        ),
                        onSolved = {
                            if (practiceIndex == practiceCharacters.lastIndex) {
                                onCompleted()
                                stage = CHARACTER_COMPLETED_STAGE
                            } else {
                                practiceIndex++
                            }
                        },
                    )
                }

                else -> LessonCompletionActions(
                    nextLabel = nextLessonLabel,
                    onNext = onNextLesson,
                    onRepeat = ::repeatLesson,
                    onBackToLearn = onBackToLearn,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CharacterPresentationCard(
    character: BrailleCharacter,
    position: Int,
    total: Int,
    group: String,
    explanation: String,
) {
    val points = character.cell.activePoints().joinToString(", ")
    BrailuxSectionCard(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
        Text(
            text = group,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.lesson_character_position, position, total),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = character.printedCharacter.toString(),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        BrailleCellView(
            cell = character.cell,
            contentDescription = character.accessibleDescription,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.lesson_active_points, points),
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = explanation,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun vowelExplanationResource(character: Char): Int = when (character) {
    'A' -> R.string.vowel_a_explanation
    'E' -> R.string.vowel_e_explanation
    'I' -> R.string.vowel_i_explanation
    'O' -> R.string.vowel_o_explanation
    'U' -> R.string.vowel_u_explanation
    else -> error("Unsupported vowel: $character")
}

private fun letterComparisonResource(character: Char): Int = when (character) {
    'A' -> R.string.letter_a_comparison
    'B' -> R.string.letter_b_comparison
    'C' -> R.string.letter_c_comparison
    'D' -> R.string.letter_d_comparison
    'E' -> R.string.letter_e_comparison
    'F' -> R.string.letter_f_comparison
    'G' -> R.string.letter_g_comparison
    'H' -> R.string.letter_h_comparison
    'I' -> R.string.letter_i_comparison
    'J' -> R.string.letter_j_comparison
    else -> error("Unsupported character: $character")
}
