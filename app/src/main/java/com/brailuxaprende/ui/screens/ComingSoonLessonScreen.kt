package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun ComingSoonLessonScreen(
    lessonNumber: Int,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            BrailuxSectionCard(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                Text(
                    text = stringResource(R.string.lesson_coming_soon_specific),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
