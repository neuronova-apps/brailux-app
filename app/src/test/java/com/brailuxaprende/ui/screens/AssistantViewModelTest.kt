package com.brailuxaprende.ui.screens

import com.brailuxaprende.ai.BrailuxAiClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantViewModelTest {

    @Test
    fun `blank messages are not sent`() {
        var calls = 0
        val viewModel = AssistantViewModel(BrailuxAiClient {
            calls += 1
            "Respuesta"
        })

        viewModel.updateInput("   ")
        viewModel.send()

        assertEquals(0, calls)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `send records the user and assistant messages`() {
        val completed = CountDownLatch(1)
        val viewModel = AssistantViewModel(BrailuxAiClient { message ->
            assertEquals("¿Cómo se representa la A?", message)
            "La letra A usa el punto 1."
        })

        viewModel.updateInput("  ¿Cómo se representa la A?  ")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 }.also {
            if (it) completed.countDown()
        })
        assertTrue(completed.await(1, TimeUnit.SECONDS))
        assertEquals(
            listOf(AssistantMessageAuthor.User, AssistantMessageAuthor.Assistant),
            viewModel.uiState.value.messages.map(AssistantMessage::author),
        )
        assertEquals("La letra A usa el punto 1.", viewModel.uiState.value.messages.last().text)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `learning advice is shown as short plain text with a readable list`() {
        val viewModel = AssistantViewModel(BrailuxAiClient { message ->
            assertEquals("¿Cómo puedo aprender Braille?", message)
            """
            ## Primeros pasos
            **Aprende la celda de seis puntos** y practica con calma.

            * Empieza por pocas letras.
            - Repásalas con el tacto.

            __Avanza cuando las reconozcas con seguridad.__
            """.trimIndent()
        })

        viewModel.updateInput("¿Cómo puedo aprender Braille?")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 })
        assertEquals(
            """
            Primeros pasos
            Aprende la celda de seis puntos y practica con calma.

            • Empieza por pocas letras.
            • Repásalas con el tacto.

            Avanza cuando las reconozcas con seguridad.
            """.trimIndent(),
            viewModel.uiState.value.messages.last().text,
        )
    }

    @Test
    fun `braille point explanation keeps point numbers and useful line breaks`() {
        val viewModel = AssistantViewModel(BrailuxAiClient { message ->
            assertEquals("¿Cómo se representa la letra A en Braille?", message)
            "**La letra A** se representa con el punto 1.\n\n__Activa solo ese punto.__"
        })

        viewModel.updateInput("¿Cómo se representa la letra A en Braille?")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 })
        assertEquals(
            "La letra A se representa con el punto 1.\n\nActiva solo ese punto.",
            viewModel.uiState.value.messages.last().text,
        )
    }

    @Test
    fun `explicit detail request is cleaned without truncating its response`() {
        val detailedResponse = buildString {
            append("**Numeración de la celda**\n\n")
            repeat(30) { index ->
                append("Detalle ${index + 1}: puntos 1, 2, 3, 4, 5 y 6. ")
            }
            append("El signo _ y la operación 3 * 2 conservan su significado.")
        }
        val viewModel = AssistantViewModel(BrailuxAiClient { message ->
            assertEquals(
                "Explícame con más detalle cómo se numeran los seis puntos de la celda Braille.",
                message,
            )
            detailedResponse
        })

        viewModel.updateInput(
            "Explícame con más detalle cómo se numeran los seis puntos de la celda Braille.",
        )
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 })
        val displayedResponse = viewModel.uiState.value.messages.last().text
        assertFalse(displayedResponse.contains("**"))
        assertTrue(displayedResponse.contains("Detalle 30"))
        assertTrue(displayedResponse.contains("puntos 1, 2, 3, 4, 5 y 6"))
        assertTrue(displayedResponse.endsWith("3 * 2 conservan su significado."))
    }

    @Test
    fun `a second send is ignored while a request is active`() {
        val releaseResponse = CountDownLatch(1)
        var calls = 0
        val viewModel = AssistantViewModel(BrailuxAiClient {
            calls += 1
            releaseResponse.await(1, TimeUnit.SECONDS)
            "Respuesta"
        })

        viewModel.updateInput("Primera pregunta")
        viewModel.send()
        viewModel.updateInput("Segunda pregunta")
        viewModel.send()

        assertEquals(1, viewModel.uiState.value.messages.size)
        releaseResponse.countDown()
        assertTrue(waitUntil { !viewModel.uiState.value.isLoading })
        assertEquals(1, calls)
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (condition()) return true
            Thread.sleep(10)
        }
        return condition()
    }
}
