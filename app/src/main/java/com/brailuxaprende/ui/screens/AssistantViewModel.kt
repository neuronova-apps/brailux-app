package com.brailuxaprende.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brailuxaprende.ai.BrailuxAiClient
import com.brailuxaprende.ai.NetworkChecker
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
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

const val MAX_ASSISTANT_INPUT_LENGTH = 500

enum class AssistantMessageAuthor {
    User,
    Assistant,
}

data class AssistantMessage(
    val author: AssistantMessageAuthor,
    val text: String,
)

enum class AssistantStatus {
    Idle,
    Loading,
    Success,
    Offline,
    Error,
}

data class AssistantUiState(
    val input: String = "",
    val messages: List<AssistantMessage> = emptyList(),
    val status: AssistantStatus = AssistantStatus.Idle,
    val errorMessage: String? = null,
    val inputError: String? = null,
    val lastFailedQuery: String? = null,
) {
    val isLoading: Boolean get() = status == AssistantStatus.Loading
    val isOffline: Boolean get() = status == AssistantStatus.Offline
    val hasError: Boolean get() = status == AssistantStatus.Error || status == AssistantStatus.Offline
    val isInputTooLong: Boolean get() = input.length > MAX_ASSISTANT_INPUT_LENGTH
    val canSend: Boolean get() = input.trim().isNotEmpty() && !isInputTooLong && !isLoading
}

class AssistantViewModel(
    private val aiClient: BrailuxAiClient,
    private val networkChecker: NetworkChecker = NetworkChecker { true },
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _uiState = MutableStateFlow(AssistantUiState())

    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun updateInput(input: String) {
        _uiState.update { current ->
            current.copy(
                input = input,
                inputError = if (input.length > MAX_ASSISTANT_INPUT_LENGTH) {
                    INPUT_TOO_LONG_ERROR
                } else {
                    null
                },
            )
        }
    }

    fun send() {
        val rawInput = _uiState.value.input
        val message = rawInput.trim()
        if (message.isEmpty() || rawInput.length > MAX_ASSISTANT_INPUT_LENGTH || _uiState.value.isLoading) {
            return
        }

        val isOnline = networkChecker.isConnected()
        com.brailuxaprende.ai.BrailuxDiagnosticLogger.d(
            "BrailuxDiagnostic",
            "AssistantViewModel.send(): NetworkChecker.isConnected() = $isOnline",
        )
        if (!isOnline) {
            _uiState.update { current ->
                current.copy(
                    input = "",
                    inputError = null,
                    messages = current.messages + AssistantMessage(
                        author = AssistantMessageAuthor.User,
                        text = message,
                    ),
                    status = AssistantStatus.Offline,
                    errorMessage = OFFLINE_MESSAGE,
                    lastFailedQuery = message,
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                input = "",
                inputError = null,
                messages = current.messages + AssistantMessage(
                    author = AssistantMessageAuthor.User,
                    text = message,
                ),
                status = AssistantStatus.Loading,
                errorMessage = null,
                lastFailedQuery = null,
            )
        }

        scope.launch {
            executeAiQuery(message)
        }
    }

    fun retry() {
        val query = _uiState.value.lastFailedQuery ?: return
        if (_uiState.value.isLoading) return

        val isOnline = networkChecker.isConnected()
        com.brailuxaprende.ai.BrailuxDiagnosticLogger.d(
            "BrailuxDiagnostic",
            "AssistantViewModel.retry(): NetworkChecker.isConnected() = $isOnline",
        )
        if (!isOnline) {
            _uiState.update { current ->
                current.copy(
                    status = AssistantStatus.Offline,
                    errorMessage = OFFLINE_MESSAGE,
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                status = AssistantStatus.Loading,
                errorMessage = null,
            )
        }

        scope.launch {
            executeAiQuery(query)
        }
    }

    private suspend fun executeAiQuery(query: String) {
        try {
            val response = aiClient.preguntar(query)
            val cleaned = cleanAssistantResponse(response)
            if (cleaned.isBlank() || cleaned == EMPTY_FORMATTED_RESPONSE) {
                _uiState.update { current ->
                    current.copy(
                        status = AssistantStatus.Error,
                        errorMessage = GENERIC_ERROR_MESSAGE,
                        lastFailedQuery = query,
                    )
                }
            } else {
                _uiState.update { current ->
                    current.copy(
                        messages = current.messages + AssistantMessage(
                            author = AssistantMessageAuthor.Assistant,
                            text = cleaned,
                        ),
                        status = AssistantStatus.Success,
                        errorMessage = null,
                        lastFailedQuery = null,
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val errorClass = error.javaClass.name
            val rawMsg = error.message.orEmpty()
            com.brailuxaprende.ai.BrailuxDiagnosticLogger.e(
                "BrailuxDiagnostic",
                "AssistantViewModel.executeAiQuery() falló. Excepción: $errorClass, Mensaje: $rawMsg",
                error,
            )
            _uiState.update { current ->
                current.copy(
                    status = AssistantStatus.Error,
                    errorMessage = resolveErrorMessage(error),
                    lastFailedQuery = query,
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
    private val networkChecker: NetworkChecker = NetworkChecker { true },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
        return AssistantViewModel(aiClient, networkChecker) as T
    }
}

internal const val OFFLINE_MESSAGE =
    "No hay conexión a Internet."

internal const val SERVICE_ERROR_MESSAGE =
    "El Asistente no está disponible temporalmente. Inténtalo nuevamente."

internal const val TEMPORARY_ERROR_MESSAGE =
    "No se pudo conectar con el Asistente. Inténtalo nuevamente."

internal const val GENERIC_ERROR_MESSAGE =
    "Ocurrió un problema al generar la respuesta."

internal const val INPUT_TOO_LONG_ERROR =
    "La consulta no puede superar los 500 caracteres."

internal fun resolveErrorMessage(error: Throwable): String {
    val errorName = error.javaClass.simpleName
    val message = error.message.orEmpty().lowercase()

    return when {
        error is TimeoutException || error is SocketTimeoutException || message.contains("timeout") -> {
            TEMPORARY_ERROR_MESSAGE
        }
        message.contains("appcheck") || message.contains("app check") ||
            message.contains("service") || message.contains("unavailable") ||
            message.contains("503") || message.contains("403") ||
            message.contains("firebase") || errorName.contains("AppCheck") ||
            errorName.contains("Firebase") -> {
            SERVICE_ERROR_MESSAGE
        }
        else -> GENERIC_ERROR_MESSAGE
    }
}

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
