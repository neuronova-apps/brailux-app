package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

object PracticeSessionSnapshotCodec {
    fun encode(snapshot: PracticeSessionSnapshot): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(snapshot.version)
            output.writeUTF(snapshot.sessionId)
            output.writeUTF(snapshot.state.session.level.name)
            output.writeUTF(snapshot.state.session.mode.name)
            output.writeCustomConfiguration(snapshot.state.session.customConfiguration)
            output.writeExercises(snapshot.state.session.exercises)
            output.writeSessionState(snapshot.state)
            output.writeUTF(snapshot.phase.name)
            output.writeInt(snapshot.creditAttempt)
            output.writeReward(snapshot.engagementReward)
        }
        return bytes.toByteArray().toHexString()
    }

    fun decode(encoded: String): PracticeSessionSnapshot? {
        if (encoded.length > MAX_ENCODED_LENGTH) return null
        val bytes = encoded.hexToByteArrayOrNull() ?: return null
        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                requireDecoded(input.readInt() == MAGIC)
                val version = input.readInt()
                requireDecoded(version == PracticeSessionSnapshot.CURRENT_VERSION)
                val sessionId = input.readUTF()
                val level = input.readEnumValue<PracticeLevel>()
                val mode = input.readEnumValue<PracticeMode>()
                val customConfiguration = input.readCustomConfiguration()
                val exercises = input.readExercises()
                val session = PracticeSession(
                    level = level,
                    mode = mode,
                    exercises = exercises,
                    customConfiguration = customConfiguration,
                )
                val state = input.readSessionState(session, sessionId)
                val phase = input.readEnumValue<PracticeSessionPhase>()
                val creditAttempt = input.readInt()
                val reward = input.readReward()
                requireDecoded(input.available() == 0)
                PracticeSessionSnapshot(
                    state = state,
                    phase = phase,
                    creditAttempt = creditAttempt,
                    engagementReward = reward,
                    version = version,
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun DataOutputStream.writeCustomConfiguration(
    configuration: CustomPracticeConfiguration?,
) {
    writeBoolean(configuration != null)
    if (configuration == null) return
    val groups = configuration.additionalContentGroups.sortedBy { it.name }
    writeInt(groups.size)
    groups.forEach { writeUTF(it.name) }
    writeUTF(configuration.exerciseCount.name)
    writeUTF(configuration.mode.name)
    writeBoolean(configuration.hintsEnabled)
    writeBoolean(configuration.showPointNumbers)
}

private fun DataInputStream.readCustomConfiguration(): CustomPracticeConfiguration? {
    if (!readBoolean()) return null
    val groups = buildSet {
        repeat(readBoundedCount(PracticeContentGroup.entries.size)) {
            add(readEnumValue<PracticeContentGroup>())
        }
    }
    return CustomPracticeConfiguration(
        additionalContentGroups = groups,
        exerciseCount = readEnumValue(),
        mode = readEnumValue(),
        hintsEnabled = readBoolean(),
        showPointNumbers = readBoolean(),
    )
}

private fun DataOutputStream.writeExercises(exercises: List<PracticeExercise>) {
    writeInt(exercises.size)
    exercises.forEach { exercise ->
        writeChar(exercise.target.printedCharacter.code)
        writeUTF(exercise.type.name)
        writeInt(exercise.options.size)
        exercise.options.forEach { option -> writeChar(option.printedCharacter.code) }
    }
}

private fun DataInputStream.readExercises(): List<PracticeExercise> = buildList {
    repeat(readBoundedCount(MAX_EXERCISES)) {
        val target = readBrailleCharacter()
        val type = readEnumValue<PracticeExerciseType>()
        val options = buildList {
            repeat(readBoundedCount(MAX_OPTIONS)) { add(readBrailleCharacter()) }
        }
        add(PracticeExercise(target = target, type = type, options = options))
    }
}

private fun DataOutputStream.writeSessionState(state: PracticeSessionState) {
    writeInt(state.currentExerciseIndex)
    writeNullableChar(state.selectedCharacter)
    writeInt(state.attemptsOnCurrentExercise)
    writeInt(state.firstAttemptCorrect)
    writeInt(state.errors)
    writeInt(state.currentFirstAttemptCorrectStreak)
    writeInt(state.longestFirstAttemptCorrectStreak)
    writeUTF(state.validation.name)
    writeBoolean(state.showPointNumbers)
    writeInt(state.revealedHintCount)
    writeInt(state.hintsUsed)
    writeBoolean(state.isCompleted)
    writeInt(state.completedAnswers.size)
    state.completedAnswers.forEach { answer ->
        writeInt(answer.exerciseIndex)
        writeCharacterList(answer.responses)
    }
    writeCharacterList(state.currentExerciseAnswers)
}

private fun DataInputStream.readSessionState(
    session: PracticeSession,
    sessionId: String,
): PracticeSessionState {
    val currentExerciseIndex = readInt()
    val selectedCharacter = readNullableChar()
    val attemptsOnCurrentExercise = readInt()
    val firstAttemptCorrect = readInt()
    val errors = readInt()
    val currentFirstAttemptCorrectStreak = readInt()
    val longestFirstAttemptCorrectStreak = readInt()
    val validation = readEnumValue<PracticeValidationState>()
    val showPointNumbers = readBoolean()
    val revealedHintCount = readInt()
    val hintsUsed = readInt()
    val isCompleted = readBoolean()
    val completedAnswers = buildList {
        repeat(readBoundedCount(MAX_EXERCISES)) {
            add(
                PracticeCompletedAnswer(
                    exerciseIndex = readInt(),
                    responses = readCharacterList(),
                ),
            )
        }
    }
    val currentExerciseAnswers = readCharacterList()
    return PracticeSessionState(
        session = session,
        currentExerciseIndex = currentExerciseIndex,
        selectedCharacter = selectedCharacter,
        attemptsOnCurrentExercise = attemptsOnCurrentExercise,
        firstAttemptCorrect = firstAttemptCorrect,
        errors = errors,
        currentFirstAttemptCorrectStreak = currentFirstAttemptCorrectStreak,
        longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
        validation = validation,
        showPointNumbers = showPointNumbers,
        revealedHintCount = revealedHintCount,
        hintsUsed = hintsUsed,
        isCompleted = isCompleted,
        completedAnswers = completedAnswers,
        currentExerciseAnswers = currentExerciseAnswers,
        sessionId = sessionId,
    )
}

private fun DataOutputStream.writeReward(reward: EngagementReward?) {
    writeBoolean(reward != null)
    if (reward == null) return
    writeInt(reward.xpEarned)
    writeBoolean(reward.addedPracticeDay)
    writeInt(reward.weeklyPracticeDays)
    writeInt(reward.currentStreak)
    writeBoolean(reward.miniAchievementCompleted != null)
    reward.miniAchievementCompleted?.let { writeUTF(it.name) }
    val achievements = reward.newlyUnlockedAchievements.sortedBy { it.name }
    writeInt(achievements.size)
    achievements.forEach { writeUTF(it.name) }
}

private fun DataInputStream.readReward(): EngagementReward? {
    if (!readBoolean()) return null
    val xpEarned = readInt()
    val addedPracticeDay = readBoolean()
    val weeklyPracticeDays = readInt()
    val currentStreak = readInt()
    val miniAchievement = if (readBoolean()) readEnumValue<DailyMiniAchievement>() else null
    val achievements = buildSet {
        repeat(readBoundedCount(PermanentAchievement.entries.size)) {
            add(readEnumValue<PermanentAchievement>())
        }
    }
    return EngagementReward(
        xpEarned = xpEarned,
        addedPracticeDay = addedPracticeDay,
        weeklyPracticeDays = weeklyPracticeDays,
        currentStreak = currentStreak,
        miniAchievementCompleted = miniAchievement,
        newlyUnlockedAchievements = achievements,
    )
}

private fun DataOutputStream.writeNullableChar(value: Char?) {
    writeBoolean(value != null)
    value?.let { writeChar(it.code) }
}

private fun DataInputStream.readNullableChar(): Char? = if (readBoolean()) readChar() else null

private fun DataOutputStream.writeCharacterList(values: List<Char>) {
    writeInt(values.size)
    values.forEach { writeChar(it.code) }
}

private fun DataInputStream.readCharacterList(): List<Char> = buildList {
    repeat(readBoundedCount(MAX_RESPONSES)) { add(readChar()) }
}

private fun DataInputStream.readBrailleCharacter(): BrailleCharacter {
    val character = readChar()
    val restored = BrailleRepository.findCharacter(character)
        ?: throw IOException("Unknown Braille character in practice snapshot.")
    if (restored.printedCharacter != character) {
        throw IOException("Non-canonical Braille character in practice snapshot.")
    }
    return restored
}

private fun DataInputStream.readBoundedCount(maximum: Int): Int {
    val count = readInt()
    requireDecoded(count in 0..maximum)
    return count
}

private inline fun <reified T : Enum<T>> DataInputStream.readEnumValue(): T {
    val storedName = readUTF()
    return enumValues<T>().firstOrNull { it.name == storedName }
        ?: throw IOException("Unknown enum value in practice snapshot.")
}

private fun requireDecoded(condition: Boolean) {
    if (!condition) throw IOException("Invalid practice session snapshot.")
}

private fun ByteArray.toHexString(): String {
    val bytes = this
    return buildString(bytes.size * 2) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }
}

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) return null
    val result = ByteArray(length / 2)
    var index = 0
    while (index < length) {
        val high = Character.digit(this[index], 16)
        val low = Character.digit(this[index + 1], 16)
        if (high < 0 || low < 0) return null
        result[index / 2] = ((high shl 4) or low).toByte()
        index += 2
    }
    return result
}

private const val MAGIC = 0x42524149
private const val MAX_ENCODED_LENGTH = 1_000_000
private const val MAX_EXERCISES = 100
private const val MAX_OPTIONS = 32
private const val MAX_RESPONSES = 10_000
private const val HEX_DIGITS = "0123456789abcdef"
