package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class WorkType(val code: String, val labelRu: String) {
    LAB("LAB", "Лабораторная"),
    COURSEWORK("COURSEWORK", "Курсовая"),
    ESSAY("ESSAY", "Реферат / Доклад"),
    PRACTICE("PRACTICE", "Практическая"),
    SEMINAR("SEMINAR", "Семинар / ДЗ"),
    EXAM("EXAM", "Зачёт / Экзамен"),
    PROJECT("PROJECT", "Проект");

    companion object {
        fun fromCode(code: String): WorkType =
            entries.firstOrNull { it.code == code } ?: PRACTICE
    }
}

enum class ProgressStage(
    val code: String,
    val labelRu: String,
    val shortHintRu: String,
    val defaultPercent: Int
) {
    NOT_STARTED("NOT_STARTED", "Не начато", "Ждёт старта", 0),
    RESEARCH("RESEARCH", "Сбор базы", "Изучение темы и материалов", 25),
    DRAFTING("DRAFTING", "В процессе", "Пишу основную часть / код", 55),
    REVISION("REVISION", "На доработке", "Исправляю замечания и правки", 75),
    READY("READY", "Готово к сдаче", "Осталось показать или защитить", 95),
    SUBMITTED("SUBMITTED", "Сдано", "Задание полностью закрыто!", 100);

    companion object {
        fun fromCode(code: String): ProgressStage =
            entries.firstOrNull { it.code == code } ?: NOT_STARTED
    }
}

enum class AssignmentPriority(val code: String, val labelRu: String) {
    LOW("LOW", "Обычный"),
    MEDIUM("MEDIUM", "Средний"),
    HIGH("HIGH", "Важный"),
    CRITICAL("CRITICAL", "Горит дедлайн");

    companion object {
        fun fromCode(code: String): AssignmentPriority =
            entries.firstOrNull { it.code == code } ?: MEDIUM
    }
}

@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val teacher: String = "",
    val colorHex: String = "#7C5CFF",
    val iconKey: String = "BOOK",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectColorHex: String = "#7C5CFF",
    val title: String = "",
    val workType: String = WorkType.PRACTICE.code,
    val stage: String = ProgressStage.NOT_STARTED.code,
    val progressPercent: Long = 0L,
    val currentProgressNote: String = "",
    val revisionNotes: String = "",
    val checklistItems: List<String> = emptyList(),
    val dueDateMillis: Long = System.currentTimeMillis() + 86_400_000L * 3,
    val priority: String = AssignmentPriority.MEDIUM.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedWorkType: WorkType
        get() = WorkType.fromCode(workType)

    val parsedStage: ProgressStage
        get() = ProgressStage.fromCode(stage)

    val parsedPriority: AssignmentPriority
        get() = AssignmentPriority.fromCode(priority)

    val isSubmitted: Boolean
        get() = stage == ProgressStage.SUBMITTED.code || progressPercent >= 100L

    val needsRevision: Boolean
        get() = stage == ProgressStage.REVISION.code ||
                revisionNotes.isNotBlank() ||
                checklistItems.any { it.startsWith("[ ]") }
}

data class ChecklistEntry(
    val index: Int,
    val text: String,
    val isDone: Boolean
) {
    fun toSerializedString(): String = if (isDone) "[x] $text" else "[ ] $text"

    companion object {
        fun parseList(raw: List<String>): List<ChecklistEntry> =
            raw.mapIndexed { idx, item ->
                when {
                    item.startsWith("[x] ", ignoreCase = true) ->
                        ChecklistEntry(idx, item.substring(4).trim(), true)
                    item.startsWith("[ ] ") ->
                        ChecklistEntry(idx, item.substring(4).trim(), false)
                    else ->
                        ChecklistEntry(idx, item.trim(), false)
                }
            }
    }
}

data class AiBreakdownSuggestion(
    val nextImmediateStep: String,
    val revisionSummary: String,
    val subTasks: List<String>,
    val studyTip: String
)
