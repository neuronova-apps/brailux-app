package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.brailuxaprende.R
import com.brailuxaprende.practice.CustomExerciseCount
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.PracticeContentGroup
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun CustomPracticeConfigurationScreen(
    initialConfiguration: CustomPracticeConfiguration,
    onStartPractice: (CustomPracticeConfiguration) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var additionalGroups by rememberSaveable(initialConfiguration) {
        mutableStateOf(initialConfiguration.additionalContentGroups.map { it.name }.toSet())
    }
    var exerciseCountName by rememberSaveable(initialConfiguration) {
        mutableStateOf(initialConfiguration.exerciseCount.name)
    }
    var modeName by rememberSaveable(initialConfiguration) {
        mutableStateOf(initialConfiguration.mode.name)
    }
    var hintsEnabled by rememberSaveable(initialConfiguration) {
        mutableStateOf(initialConfiguration.hintsEnabled)
    }
    var showPointNumbers by rememberSaveable(initialConfiguration) {
        mutableStateOf(initialConfiguration.showPointNumbers)
    }
    val availableAdditionalGroups = PracticeContentGroup.entries.filter {
        it != PracticeContentGroup.SpanishAlphabet && it.isAvailable
    }
    val configuration = CustomPracticeConfiguration(
        additionalContentGroups = additionalGroups.mapNotNull { storedName ->
            PracticeContentGroup.entries.firstOrNull { it.name == storedName && it.isAvailable }
        }.toSet(),
        exerciseCount = CustomExerciseCount.valueOf(exerciseCountName),
        mode = PracticeMode.valueOf(modeName),
        hintsEnabled = hintsEnabled,
        showPointNumbers = showPointNumbers,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.custom_practice_configure_title),
                subtitle = stringResource(R.string.practice_level_4_title),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(18.dp))
            ConfigurationSection(title = R.string.custom_practice_content_title) {
                MandatoryAlphabetOption()
                PracticeContentGroup.entries
                    .filterNot { it == PracticeContentGroup.SpanishAlphabet }
                    .forEach { group ->
                        if (group.isAvailable) {
                            AvailableContentOption(
                                group = group,
                                selected = group.name in additionalGroups,
                                onSelectedChange = { selected ->
                                    additionalGroups = if (selected) {
                                        additionalGroups + group.name
                                    } else {
                                        additionalGroups - group.name
                                    }
                                },
                            )
                        } else {
                            UpcomingContentOption(group)
                        }
                    }
                if (availableAdditionalGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = {
                            additionalGroups = availableAdditionalGroups.map { it.name }.toSet()
                        }) {
                            Text(stringResource(R.string.custom_practice_select_all))
                        }
                        TextButton(onClick = { additionalGroups = emptySet() }) {
                            Text(stringResource(R.string.custom_practice_remove_additional))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ConfigurationSection(title = R.string.custom_practice_exercise_count_title) {
                Column(Modifier.selectableGroup()) {
                    CustomExerciseCount.entries.forEach { count ->
                        ConfigurationRadioOption(
                            title = stringResource(
                                R.string.custom_practice_exercise_count_option,
                                count.value,
                            ),
                            selected = exerciseCountName == count.name,
                            onSelect = { exerciseCountName = count.name },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ConfigurationSection(title = R.string.custom_practice_mode_title) {
                Column(Modifier.selectableGroup()) {
                    PracticeMode.entries.forEach { mode ->
                        ConfigurationRadioOption(
                            title = stringResource(mode.titleResource()),
                            description = stringResource(mode.descriptionResource()),
                            selected = modeName == mode.name,
                            onSelect = { modeName = mode.name },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ConfigurationSection(title = R.string.custom_practice_help_title) {
                ConfigurationSwitchOption(
                    title = stringResource(R.string.custom_practice_hints),
                    description = stringResource(R.string.custom_practice_hints_description),
                    checked = hintsEnabled,
                    onCheckedChange = { hintsEnabled = it },
                )
                ConfigurationSwitchOption(
                    title = stringResource(R.string.custom_practice_point_numbers),
                    description = stringResource(R.string.custom_practice_point_numbers_description),
                    checked = showPointNumbers,
                    onCheckedChange = { showPointNumbers = it },
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            BrailuxPrimaryButton(
                text = stringResource(R.string.custom_practice_start),
                onClick = { onStartPractice(configuration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConfigurationSection(
    @StringRes title: Int,
    content: @Composable () -> Unit,
) {
    BrailuxSectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
    ) {
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Column(modifier = Modifier.padding(top = 8.dp)) { content() }
    }
}

@Composable
private fun MandatoryAlphabetOption() {
    val state = stringResource(R.string.custom_practice_selected_required_available)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = true, onCheckedChange = null, enabled = false)
        ContentOptionText(
            title = stringResource(R.string.custom_practice_alphabet),
            description = stringResource(R.string.custom_practice_alphabet_description),
            status = stringResource(R.string.custom_practice_required),
        )
    }
}

@Composable
private fun AvailableContentOption(
    group: PracticeContentGroup,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    val state = stringResource(
        if (selected) R.string.custom_practice_selected_available
        else R.string.custom_practice_not_selected_available,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = onSelectedChange,
            )
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        ContentOptionText(
            title = stringResource(group.titleResource()),
            description = stringResource(group.descriptionResource()),
        )
    }
}

@Composable
private fun UpcomingContentOption(group: PracticeContentGroup) {
    val comingSoon = stringResource(R.string.practice_coming_soon)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {
                disabled()
                stateDescription = comingSoon
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentOptionText(
            title = stringResource(group.titleResource()),
            description = stringResource(group.descriptionResource()),
            status = stringResource(R.string.practice_coming_soon),
        )
    }
}

@Composable
private fun ContentOptionText(
    title: String,
    description: String,
    status: String? = null,
) {
    Column(modifier = Modifier.padding(start = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = description,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        status?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfigurationRadioOption(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    description: String? = null,
) {
    val state = stringResource(
        if (selected) R.string.custom_practice_selected_available
        else R.string.custom_practice_not_selected_available,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfigurationSwitchOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val state = stringResource(
        if (checked) R.string.custom_practice_enabled_available
        else R.string.custom_practice_disabled_available,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@StringRes
private fun PracticeContentGroup.titleResource(): Int = when (this) {
    PracticeContentGroup.SpanishAlphabet -> R.string.custom_practice_alphabet
    PracticeContentGroup.AccentuationAndDiaeresis -> R.string.custom_practice_accents
    PracticeContentGroup.Punctuation -> R.string.custom_practice_punctuation
    PracticeContentGroup.Numbers -> R.string.custom_practice_numbers
    PracticeContentGroup.Capitals -> R.string.custom_practice_capitals
}

@StringRes
private fun PracticeContentGroup.descriptionResource(): Int = when (this) {
    PracticeContentGroup.SpanishAlphabet -> R.string.custom_practice_alphabet_description
    PracticeContentGroup.AccentuationAndDiaeresis -> R.string.custom_practice_accents_description
    PracticeContentGroup.Punctuation -> R.string.custom_practice_punctuation_description
    PracticeContentGroup.Numbers -> R.string.custom_practice_numbers_description
    PracticeContentGroup.Capitals -> R.string.custom_practice_capitals_description
}

@StringRes
private fun PracticeMode.titleResource(): Int = when (this) {
    PracticeMode.SignToCharacter -> R.string.practice_mode_sign_to_character_character
    PracticeMode.CharacterToSign -> R.string.practice_mode_character_to_sign_character
    PracticeMode.Mixed -> R.string.practice_mode_mixed
}

@StringRes
private fun PracticeMode.descriptionResource(): Int = when (this) {
    PracticeMode.SignToCharacter -> R.string.practice_mode_sign_to_character_description
    PracticeMode.CharacterToSign -> R.string.practice_mode_character_to_sign_description
    PracticeMode.Mixed -> R.string.practice_mode_mixed_description
}
