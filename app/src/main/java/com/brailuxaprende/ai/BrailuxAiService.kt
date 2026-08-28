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

class BrailuxAiService : BrailuxAiClient {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = "gemini-3.5-flash-lite",
        systemInstruction = content {
            text(BRAILUX_SYSTEM_INSTRUCTIONS)
        }
    )

    suspend fun probarConexion(): String {
        val response = model.generateContent(
            "Responde exactamente: Brailux IA conectada"
        )

        return response.text ?: "Sin respuesta"
    }

    override suspend fun preguntar(mensaje: String): String {
        val mensajeLimpio = mensaje.trim()
        if (mensajeLimpio.isEmpty()) {
            return EMPTY_MESSAGE_RESPONSE
        }

        return try {
            model.generateContent(mensajeLimpio).text
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: EMPTY_MODEL_RESPONSE
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("BRAILUX_AI", "Error real al consultar Gemini", error)
            REQUEST_ERROR_RESPONSE
        }
    }
}
