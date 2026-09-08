package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenTitle = stringResource(R.string.assistant_title)
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val visibleConversationItems = state.messages.size +
        (if (state.isLoading) 1 else 0) +
        (if (state.hasError) 1 else 0)

    LaunchedEffect(visibleConversationItems) {
        if (visibleConversationItems > 0) {
            listState.animateScrollToItem(visibleConversationItems + 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .semantics { paneTitle = screenTitle },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(top = 4.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BrailuxScreenHeader(
                        title = screenTitle,
                        subtitle = stringResource(R.string.assistant_description),
                        onBack = onBack,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                item {
                    AssistantPrivacyNotice(
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (state.messages.isEmpty() && !state.isLoading && !state.hasError) {
                    item {
                        Text(
                            text = stringResource(R.string.assistant_empty_conversation),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.messages) { message ->
                    AssistantMessageCard(message = message)
                }
                if (state.isLoading) {
                    item {
                        AssistantLoadingIndicator()
                    }
                }
                if (state.hasError && state.errorMessage != null) {
                    item {
                        AssistantErrorCard(
                            message = state.errorMessage,
                            onRetry = onRetry,
                        )
                    }
                }
            }
            AssistantInput(
                input = state.input,
                isLoading = state.isLoading,
                isInputTooLong = state.isInputTooLong,
                inputError = state.inputError,
                canSend = state.canSend,
                onInputChange = onInputChange,
                onSend = onSend,
                focusManager = focusManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun AssistantPrivacyNotice(modifier: Modifier = Modifier) {
    val noticeText = stringResource(R.string.assistant_privacy_notice)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = noticeText },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssistantMessageCard(
    message: AssistantMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.author == AssistantMessageAuthor.User
    val authorLabel = stringResource(
        if (isUser) R.string.assistant_user_label else R.string.assistant_label,
    )
    val accessibilityDescription = stringResource(
        if (isUser) {
            R.string.assistant_user_message_accessibility
        } else {
            R.string.assistant_response_accessibility
        },
        message.text,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibilityDescription
                if (!isUser) liveRegion = LiveRegionMode.Polite
            },
        shape = MaterialTheme.shapes.large,
        color = if (isUser) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isUser) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = authorLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message.text,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun AssistantLoadingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.assistant_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AssistantErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryLabel = stringResource(R.string.assistant_retry)
    val retryAccessibility = stringResource(R.string.assistant_retry_accessibility)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$message. $retryAccessibility"
                liveRegion = LiveRegionMode.Polite
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
            BrailuxSecondaryButton(
                text = retryLabel,
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AssistantInput(
    input: String,
    isLoading: Boolean,
    isInputTooLong: Boolean,
    inputError: String?,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            isError = isInputTooLong,
            label = { Text(stringResource(R.string.assistant_input_label)) },
            placeholder = { Text(stringResource(R.string.assistant_input_placeholder)) },
            minLines = 1,
            maxLines = 4,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (inputError != null) {
                        Text(
                            text = inputError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = stringResource(
                            R.string.assistant_char_counter,
                            input.length,
                            MAX_ASSISTANT_INPUT_LENGTH,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInputTooLong) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (canSend) {
                        onSend()
                        focusManager.clearFocus()
                    }
                },
            ),
        )
        BrailuxPrimaryButton(
            text = stringResource(R.string.assistant_send),
            onClick = {
                onSend()
                focusManager.clearFocus()
            },
            enabled = canSend,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

@Preview(name = "Asistente Brailux", showBackground = true, heightDp = 820)
@Composable
private fun AssistantScreenPreview() {
    BrailuxPreviewTheme {
        AssistantScreen(
            state = AssistantUiState(
                messages = listOf(
                    AssistantMessage(AssistantMessageAuthor.User, "¿Cómo se representa la A?"),
                    AssistantMessage(
                        AssistantMessageAuthor.Assistant,
                        "La letra A se representa con el punto 1.",
                    ),
                ),
            ),
            onInputChange = {},
            onSend = {},
            onRetry = {},
            onBack = {},
        )
    }
}

