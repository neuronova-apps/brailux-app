package com.brailuxaprende.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brailuxaprende.ai.BrailuxAiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantMessageAuthor {
    User,
    Assistant,
}

data class AssistantMessage(
    val author: AssistantMessageAuthor,
    val text: String,
)

data class AssistantUiState(
    val input: String = "",
    val messages: List<AssistantMessage> = emptyList(),
    val isLoading: Boolean = false,
)

class AssistantViewModel(
    private val aiClient: BrailuxAiClient,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _uiState = MutableStateFlow(AssistantUiState())

    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun updateInput(input: String) {
        _uiState.update { current -> current.copy(input = input) }
    }

    fun send() {
        val message = _uiState.value.input.trim()
        if (message.isEmpty() || _uiState.value.isLoading) return

        _uiState.update { current ->
            current.copy(
                input = "",
                messages = current.messages + AssistantMessage(
                    author = AssistantMessageAuthor.User,
                    text = message,
                ),
                isLoading = true,
            )
        }

        scope.launch {
            val response = try {
                aiClient.preguntar(message)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                UNEXPECTED_ERROR_RESPONSE
            }
            _uiState.update { current ->
                current.copy(
                    messages = current.messages + AssistantMessage(
                        author = AssistantMessageAuthor.Assistant,
                        text = cleanAssistantResponse(response),
                    ),
                    isLoading = false,
                )
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
    }
}

class AssistantViewModelFactory(
    private val aiClient: BrailuxAiClient,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
        return AssistantViewModel(aiClient) as T
    }
}

private const val UNEXPECTED_ERROR_RESPONSE =
    "No fue posible consultar al Asistente Brailux en este momento. Inténtalo de nuevo."

private const val EMPTY_FORMATTED_RESPONSE =
    "No recibí una respuesta legible. Intenta formular la pregunta de otra manera."

private val MARKDOWN_HEADING = Regex("""^\s{0,3}#{1,6}\s+""")
private val MARKDOWN_BULLET = Regex("""^(\s*)[-*+]\s+""")
private val MARKDOWN_HORIZONTAL_RULE = Regex("""^\s*(?:-{3,}|\*{3,}|_{3,})\s*$""")
private val MARKDOWN_CODE_FENCE = Regex("""^\s*```[\w+-]*\s*$""")
private val MARKDOWN_ITALIC_UNDERSCORES = Regex("""(?<!_)__(\S(?:.*?\S)?)__(?!_)""")
private val MARKDOWN_ITALIC_ASTERISKS = Regex("""(?<!\*)\*(\S(?:[^*]*?\S)?)\*(?!\*)""")
private val MARKDOWN_INLINE_CODE = Regex("""(?<!`)`([^`\n]+)`(?!`)""")
private val EXCESS_BLANK_LINES = Regex("""\n{3,}""")

internal fun cleanAssistantResponse(response: String): String {
    val cleaned = response
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map { originalLine ->
            if (
                MARKDOWN_HORIZONTAL_RULE.matches(originalLine) ||
                MARKDOWN_CODE_FENCE.matches(originalLine)
            ) {
                ""
            } else {
                originalLine
                    .replace(MARKDOWN_HEADING, "")
                    .replace(MARKDOWN_BULLET, "$1• ")
                    .replace("**", "")
                    .replace(MARKDOWN_ITALIC_UNDERSCORES, "$1")
                    .replace(MARKDOWN_ITALIC_ASTERISKS, "$1")
                    .replace(MARKDOWN_INLINE_CODE, "$1")
                    .trimEnd()
            }
        }
        .joinToString("\n")
        .replace(EXCESS_BLANK_LINES, "\n\n")
        .trim()

    return cleaned.ifEmpty { EMPTY_FORMATTED_RESPONSE }
}
