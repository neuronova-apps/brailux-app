package com.brailuxaprende.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException

private val BRAILUX_SYSTEM_INSTRUCTIONS =
    """
    Eres el asistente educativo de Brailux, una aplicación de NeuroNova
    especializada en el aprendizaje y la práctica del sistema Braille.

    Responde de manera clara, breve y pedagógica para ayudar al usuario a
    comprender, aprender y practicar Braille.

    Cuando expliques un símbolo Braille, indica con precisión los números de
    los puntos que lo componen. No inventes símbolos, combinaciones ni reglas.
    Si no dispones de información suficiente para responder con seguridad,
    indícalo claramente.

    Cuando posteriormente se te proporcione contexto procedente del banco
    oficial de Brailux, priorízalo como fuente principal.

    Limita las preguntas ajenas al propósito educativo de Brailux. Si una
    pregunta no está relacionada con Braille o con el aprendizaje ofrecido por
    Brailux, explica brevemente que solo puedes ayudar dentro de ese ámbito.

    Puedes generar ejercicios breves y explicar los errores del usuario.

    Tus respuestas se mostrarán en una pantalla móvil. Escribe de forma breve,
    pedagógica y directa, con párrafos cortos. En una consulta habitual,
    prioriza una extensión aproximada de 60 a 120 palabras. Amplía la respuesta
    solo cuando el usuario solicite explícitamente más detalle.

    Usa texto simple y evita cualquier marca Markdown visible. No uses **, __,
    encabezados Markdown, tablas ni bloques de código, salvo que un bloque de
    código sea estrictamente necesario para responder. Evita las listas largas.
    Si una lista breve ayuda, escribe pocos elementos con texto simple.

    Cuando expliques una letra o un signo Braille, indica los puntos de forma
    clara y directa; por ejemplo: "La letra A se forma con el punto 1".
    """.trimIndent()

private const val EMPTY_MESSAGE_RESPONSE =
    "Escribe una pregunta sobre Braille para poder ayudarte."
private const val EMPTY_MODEL_RESPONSE =
    "No recibí una respuesta. Intenta formular la pregunta de otra manera."
private const val REQUEST_ERROR_RESPONSE =
    "No fue posible consultar a Brailux IA en este momento. Inténtalo de nuevo."

fun interface BrailuxAiClient {
    suspend fun preguntar(mensaje: String): String
}

internal object BrailuxDiagnosticLogger {
    fun d(tag: String, message: String) {
        if (com.brailuxaprende.BuildConfig.DEBUG) {
            runCatching { Log.d(tag, message) }
        }
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (com.brailuxaprende.BuildConfig.DEBUG) {
            runCatching {
                if (error != null) {
                    Log.e(tag, message, error)
                } else {
                    Log.e(tag, message)
                }
            }
        }
    }
}

class BrailuxAiService(
    private val modelName: String = "gemini-2.5-flash-lite",
) : BrailuxAiClient {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = modelName,
        systemInstruction = content {
            text(BRAILUX_SYSTEM_INSTRUCTIONS)
        }
    )

    override suspend fun preguntar(mensaje: String): String {
        val mensajeLimpio = mensaje.trim()
        if (mensajeLimpio.isEmpty()) {
            return EMPTY_MESSAGE_RESPONSE
        }

        BrailuxDiagnosticLogger.d(
            "BrailuxDiagnostic",
            "BrailuxAiService.preguntar(): Inicio de consulta. Longitud: ${mensajeLimpio.length} chars, Modelo: $modelName",
        )

        return try {
            val responseText = model.generateContent(mensajeLimpio).text
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: throw IllegalStateException("Respuesta vacía del servicio de IA")
            BrailuxDiagnosticLogger.d(
                "BrailuxDiagnostic",
                "BrailuxAiService.preguntar(): Consulta exitosa. Longitud respuesta: ${responseText.length} chars",
            )
            responseText
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val errorClass = error.javaClass.name
            val rawMsg = error.message.orEmpty()
            val causeClass = error.cause?.javaClass?.name
            val causeMsg = error.cause?.message.orEmpty()

            val isAppCheckFailure = errorClass.contains("AppCheck", ignoreCase = true) ||
                rawMsg.contains("appcheck", ignoreCase = true) ||
                rawMsg.contains("app check", ignoreCase = true) ||
                rawMsg.contains("403") ||
                rawMsg.contains("permission_denied", ignoreCase = true) ||
                rawMsg.contains("unauthorized", ignoreCase = true) ||
                causeMsg.contains("appcheck", ignoreCase = true)

            val isModelFailure = rawMsg.contains("model", ignoreCase = true) ||
                rawMsg.contains("404") ||
                rawMsg.contains("not found", ignoreCase = true) ||
                rawMsg.contains("unsupported", ignoreCase = true) ||
                rawMsg.contains("invalid", ignoreCase = true) ||
                causeMsg.contains("model", ignoreCase = true) ||
                causeMsg.contains("404")

            val isBackendFailure = rawMsg.contains("500") ||
                rawMsg.contains("503") ||
                rawMsg.contains("unavailable", ignoreCase = true) ||
                rawMsg.contains("backend", ignoreCase = true) ||
                causeMsg.contains("500") ||
                causeMsg.contains("503")

            BrailuxDiagnosticLogger.e(
                "BrailuxDiagnostic",
                "BrailuxAiService.preguntar(): Fallo en consulta. " +
                    "Excepción: $errorClass, " +
                    "Mensaje técnico: $rawMsg, " +
                    "Causa: $causeClass [$causeMsg], " +
                    "Fallo AppCheck: $isAppCheckFailure, " +
                    "Fallo Modelo: $isModelFailure, " +
                    "Fallo Backend: $isBackendFailure",
                error,
            )
            throw error
        }
    }
}
