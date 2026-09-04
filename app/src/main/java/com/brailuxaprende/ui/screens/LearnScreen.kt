package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.learning.LearningLessonStatus
import com.brailuxaprende.learning.LearningPath
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.theme.LocalBrailuxTheme

@Composable
fun LearnScreen(
    progress: LearningProgress,
    onOpenLesson: (LearningLesson) -> Unit,
    onBack: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.learn_title),
                subtitle = stringResource(R.string.learn_description),
                onBack = onBack,
                hasSeasonalBackground = hasSeasonalBackground,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LearningPath.lessons.forEach { lesson ->
                    val status = LearningPath.statusFor(lesson, progress.completedLessons)
                    LearningLessonCard(
                        lesson = lesson,
                        status = status,
                        onClick = { onOpenLesson(lesson) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LearningLessonCard(
    lesson: LearningLesson,
    status: LearningLessonStatus,
    onClick: () -> Unit,
) {
    val theme = LocalBrailuxTheme.current
    val statusText = stringResource(status.labelResource())
    val isEnabled = status != LearningLessonStatus.Locked
    Card(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = statusText
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = theme.visual.cardColor,
            disabledContainerColor = theme.visual.surfaceVariant,
        ),
        border = BorderStroke(1.dp, theme.visual.borderColor),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val badgeColor = when (status) {
                LearningLessonStatus.Available -> theme.visual.chipColor
                LearningLessonStatus.Completed -> theme.visual.chipColor
                LearningLessonStatus.Locked -> theme.visual.surfaceVariant
            }
            val badgeContentColor = when (status) {
                LearningLessonStatus.Available -> theme.visual.primary
                LearningLessonStatus.Completed -> theme.visual.primary
                LearningLessonStatus.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = badgeColor,
                contentColor = badgeContentColor,
            ) {
                Text(
                    text = lesson.number.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(lesson.titleResource()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) {
                        theme.visual.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = statusText,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (status) {
                        LearningLessonStatus.Completed -> theme.visual.primary
                        LearningLessonStatus.Available -> MaterialTheme.colorScheme.onSurfaceVariant
                        LearningLessonStatus.Locked -> MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}

internal fun LearningLesson.titleResource(): Int = when (this) {
    LearningLesson.SixDots -> R.string.learning_lesson_1_title
    LearningLesson.Vowels -> R.string.learning_lesson_2_title
    LearningLesson.LettersAtoJ -> R.string.learning_lesson_3_title
    LearningLesson.LettersKtoT -> R.string.learning_lesson_4_title
    LearningLesson.LettersUtoZAndEnye -> R.string.learning_lesson_5_title
}

internal fun LearningLessonStatus.labelResource(): Int = when (this) {
    LearningLessonStatus.Available -> R.string.learning_status_available
    LearningLessonStatus.Completed -> R.string.learning_status_completed
    LearningLessonStatus.Locked -> R.string.learning_status_locked
}
