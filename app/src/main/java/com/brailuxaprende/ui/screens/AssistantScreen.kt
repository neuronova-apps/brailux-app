package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenTitle = stringResource(R.string.assistant_title)
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val visibleConversationItems = state.messages.size + if (state.isLoading) 1 else 0

    LaunchedEffect(visibleConversationItems) {
        if (visibleConversationItems > 0) {
            // The header is the first item, so the latest conversation item uses this index.
            listState.animateScrollToItem(visibleConversationItems)
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
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                }
                if (state.messages.isEmpty() && !state.isLoading) {
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
            }
            AssistantInput(
                input = state.input,
                isLoading = state.isLoading,
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
private fun AssistantMessageCard(
    message: AssistantMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.author == AssistantMessageAuthor.User
    val author = stringResource(
        if (isUser) R.string.assistant_user_label else R.string.assistant_label,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$author. ${message.text}"
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
                text = author,
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
private fun AssistantInput(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    val canSend = input.isNotBlank() && !isLoading
    Column(modifier = modifier) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            label = { Text(stringResource(R.string.assistant_input_label)) },
            placeholder = { Text(stringResource(R.string.assistant_input_placeholder)) },
            minLines = 1,
            maxLines = 4,
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
                .padding(top = 10.dp),
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
            onBack = {},
        )
    }
}
