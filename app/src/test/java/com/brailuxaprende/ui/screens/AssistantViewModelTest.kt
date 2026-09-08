package com.brailuxaprende.ui.screens

import com.brailuxaprende.ai.BrailuxAiClient
import com.brailuxaprende.ai.NetworkChecker
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.play.GameProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.practice.EngagementProgress
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantViewModelTest {

    @Test
    fun `empty message is not sent and does not call service`() {
        var calls = 0
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                "Respuesta"
            },
            networkChecker = { true },
        )

        viewModel.updateInput("")
        viewModel.send()

        assertEquals(0, calls)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(AssistantStatus.Idle, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `whitespace only message is not sent and does not call service`() {
        var calls = 0
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                "Respuesta"
            },
            networkChecker = { true },
        )

        viewModel.updateInput("   \n\t  ")
        viewModel.send()

        assertEquals(0, calls)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(AssistantStatus.Idle, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `valid message sends trimmed text to client once`() {
        val completed = CountDownLatch(1)
        var receivedQuery = ""
        val viewModel = AssistantViewModel(
            aiClient = { message ->
                receivedQuery = message
                "La letra A usa el punto 1."
            },
            networkChecker = { true },
        )

        viewModel.updateInput("  ¿Cómo se representa la A?  ")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 }.also {
            if (it) completed.countDown()
        })
        assertTrue(completed.await(1, TimeUnit.SECONDS))
        assertEquals("¿Cómo se representa la A?", receivedQuery)
        assertEquals(
            listOf(AssistantMessageAuthor.User, AssistantMessageAuthor.Assistant),
            viewModel.uiState.value.messages.map(AssistantMessage::author),
        )
        assertEquals("La letra A usa el punto 1.", viewModel.uiState.value.messages.last().text)
        assertEquals(AssistantStatus.Success, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.lastFailedQuery)
    }

    @Test
    fun `message longer than 500 characters is rejected with error and not sent`() {
        var calls = 0
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                "Respuesta"
            },
            networkChecker = { true },
        )

        val longText = "A".repeat(501)
        viewModel.updateInput(longText)

        assertTrue(viewModel.uiState.value.isInputTooLong)
        assertNotNull(viewModel.uiState.value.inputError)
        assertEquals(INPUT_TOO_LONG_ERROR, viewModel.uiState.value.inputError)
        assertFalse(viewModel.uiState.value.canSend)

        viewModel.send()

        assertEquals(0, calls)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `message with exactly 500 characters is allowed and sent`() {
        var calls = 0
        val completed = CountDownLatch(1)
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                "Respuesta de prueba"
            },
            networkChecker = { true },
        )

        val exactText = "B".repeat(500)
        viewModel.updateInput(exactText)

        assertFalse(viewModel.uiState.value.isInputTooLong)
        assertNull(viewModel.uiState.value.inputError)
        assertTrue(viewModel.uiState.value.canSend)

        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.messages.size == 2 }.also {
            if (it) completed.countDown()
        })
        assertTrue(completed.await(1, TimeUnit.SECONDS))
        assertEquals(1, calls)
    }

    @Test
    fun `loading state blocks duplicate concurrent sends`() {
        val releaseResponse = CountDownLatch(1)
        var calls = 0
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                releaseResponse.await(1, TimeUnit.SECONDS)
                "Respuesta"
            },
            networkChecker = { true },
        )

        viewModel.updateInput("Primera pregunta")
        viewModel.send()
        viewModel.updateInput("Segunda pregunta")
        viewModel.send()

        assertEquals(1, viewModel.uiState.value.messages.size)
        releaseResponse.countDown()
        assertTrue(waitUntil { !viewModel.uiState.value.isLoading })
        assertEquals(1, calls)
    }

    @Test
    fun `offline state prevents calling AI service and sets offline status`() {
        var calls = 0
        var isOnline = false
        val viewModel = AssistantViewModel(
            aiClient = {
                calls += 1
                "Respuesta"
            },
            networkChecker = { isOnline },
        )

        viewModel.updateInput("¿Cómo se escribe la letra C?")
        viewModel.send()

        assertEquals(0, calls)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(AssistantStatus.Offline, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isOffline)
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(OFFLINE_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals("¿Cómo se escribe la letra C?", viewModel.uiState.value.lastFailedQuery)
    }

    @Test
    fun `empty AI response produces Error status and error message`() {
        val viewModel = AssistantViewModel(
            aiClient = { "" },
            networkChecker = { true },
        )

        viewModel.updateInput("¿Qué es Braille?")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.status == AssistantStatus.Error })
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(AssistantStatus.Error, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(GENERIC_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals("¿Qué es Braille?", viewModel.uiState.value.lastFailedQuery)
    }

    @Test
    fun `service exception produces Error status with user friendly message`() {
        val viewModel = AssistantViewModel(
            aiClient = { throw RuntimeException("Firebase AppCheck provider rejected token") },
            networkChecker = { true },
        )

        viewModel.updateInput("¿Cómo se escribe la D?")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.status == AssistantStatus.Error })
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(AssistantStatus.Error, viewModel.uiState.value.status)
        assertEquals(SERVICE_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.errorMessage!!.contains("Firebase"))
        assertFalse(viewModel.uiState.value.errorMessage!!.contains("token"))
    }

    @Test
    fun `retry reuses original failed query without duplicating user message`() {
        var isOnline = false
        var calls = 0
        val viewModel = AssistantViewModel(
            aiClient = { message ->
                calls += 1
                assertEquals("¿Cómo es la E?", message)
                "La letra E usa los puntos 1 y 5."
            },
            networkChecker = { isOnline },
        )

        viewModel.updateInput("¿Cómo es la E?")
        viewModel.send()

        assertEquals(0, calls)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(AssistantStatus.Offline, viewModel.uiState.value.status)
        assertEquals("¿Cómo es la E?", viewModel.uiState.value.lastFailedQuery)

        // Tapping retry while still offline
        viewModel.retry()
        assertEquals(0, calls)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(AssistantStatus.Offline, viewModel.uiState.value.status)

        // Restoring internet connection and retrying
        isOnline = true
        viewModel.retry()

        assertTrue(waitUntil { viewModel.uiState.value.status == AssistantStatus.Success })
        assertEquals(1, calls)
        // Ensure user message was not duplicated in the list
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals(AssistantMessageAuthor.User, viewModel.uiState.value.messages[0].author)
        assertEquals(AssistantMessageAuthor.Assistant, viewModel.uiState.value.messages[1].author)
        assertEquals("La letra E usa los puntos 1 y 5.", viewModel.uiState.value.messages[1].text)
        assertNull(viewModel.uiState.value.lastFailedQuery)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `assistant usage does not mutate learning, practice or game progress`() {
        val initialLearning = LearningProgress()
        val initialPractice = PracticeProgress()
        val initialGame = GameProgress()
        val initialEngagement = EngagementProgress()

        val viewModel = AssistantViewModel(
            aiClient = { "Respuesta del asistente." },
            networkChecker = { true },
        )

        viewModel.updateInput("Consulta educativa")
        viewModel.send()

        assertTrue(waitUntil { viewModel.uiState.value.status == AssistantStatus.Success })

        // Validate that no learning, practice, game or engagement progress was modified
        assertEquals(LearningProgress(), initialLearning)
        assertEquals(PracticeProgress(), initialPractice)
        assertEquals(GameProgress(), initialGame)
        assertEquals(EngagementProgress(), initialEngagement)
    }

    @Test
    fun `about strings do not claim 100 percent offline operation and state assistant is optional`() {
        val stringsFile = File("src/main/res/values/strings.xml")
        val content = if (stringsFile.exists()) {
            stringsFile.readText()
        } else {
            File("app/src/main/res/values/strings.xml").readText()
        }

        assertFalse(
            "Acerca de no debe afirmar que funciona completamente sin conexión",
            content.contains("Brailux funciona sin conexión"),
        )
        assertFalse(
            "about_features no debe tener afirmación de funcionamiento 100% offline",
            content.contains("<item>Funcionamiento sin conexión.</item>"),
        )
        assertTrue(
            "about_privacy_description debe clarificar que el Asistente requiere Internet",
            content.contains("El Asistente Brailux es una función opcional que requiere conexión a Internet"),
        )
        assertTrue(
            "assistant_privacy_notice debe existir y estar presente",
            content.contains("name=\"assistant_privacy_notice\""),
        )
    }

    @Test
    fun `learning advice is shown as short plain text with a readable list`() {
        val viewModel = AssistantViewModel(
            aiClient = { message ->
                assertEquals("¿Cómo puedo aprender Braille?", message)
                """
                ## Primeros pasos
                **Aprende la celda de seis puntos** y practica con calma.

                * Empieza por pocas letras.
                - Repásalas con el tacto.

                __Avanza cuando las reconozcas con seguridad.__
                """.trimIndent()
            },
            networkChecker = { true },
        )

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
        val viewModel = AssistantViewModel(
            aiClient = { message ->
                assertEquals("¿Cómo se representa la letra A en Braille?", message)
                "**La letra A** se representa con el punto 1.\n\n__Activa solo ese punto.__"
            },
            networkChecker = { true },
        )

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
        val viewModel = AssistantViewModel(
            aiClient = { message ->
                assertEquals(
                    "Explícame con más detalle cómo se numeran los seis puntos de la celda Braille.",
                    message,
                )
                detailedResponse
            },
            networkChecker = { true },
        )

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

    private fun waitUntil(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (condition()) return true
            Thread.sleep(10)
        }
        return condition()
    }
}

