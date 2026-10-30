package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Assignment
import com.example.data.model.AssignmentPriority
import com.example.data.model.ChecklistEntry
import com.example.data.model.ProgressStage
import com.example.data.model.Subject
import com.example.data.model.WorkType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElevatedCardBg
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.SubjectPresetColors
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AiBreakdownState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun parseColorSafe(hex: String, fallback: Color = ElectricViolet): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, teacher: String, colorHex: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(SubjectPresetColors.first()) }
    var selectedIconKey by remember { mutableStateOf("CODE") }

    val iconOptions = listOf(
        "CODE" to "IT / Код",
        "MATH" to "Математика",
        "LAB" to "Лабораторные",
        "SCIENCE" to "Наука",
        "BOOK" to "Гуманитарный"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Text(
                text = "Новый предмет",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название дисциплины") },
                    placeholder = { Text("Например: Базы данных") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Преподаватель / Аудитория") },
                    placeholder = { Text("Например: Смирнов А.В., ауд. 305") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_teacher_input")
                )

                Text(
                    text = "Категория",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedIconKey == key,
                            onClick = { selectedIconKey = key },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "Цветовой маркер предмета",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubjectPresetColors.forEach { hex ->
                        val c = parseColorSafe(hex)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Выбран цвет",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, teacher, selectedColor, selectedIconKey)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssignmentEditorSheet(
    subjects: List<Subject>,
    initialAssignment: Assignment?,
    aiBreakdownState: AiBreakdownState,
    onRequestAiBreakdown: (
        subjectName: String,
        title: String,
        workType: WorkType,
        stage: ProgressStage,
        currentProgressNote: String,
        revisionNotes: String
    ) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        subject: Subject,
        title: String,
        workType: WorkType,
        stage: ProgressStage,
        progressPercent: Int,
        currentProgressNote: String,
        revisionNotes: String,
        checklistItems: List<String>,
        dueDateMillis: Long,
        priority: AssignmentPriority
    ) -> Unit,
    onOpenAddSubject: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultSubject = subjects.firstOrNull { it.id == initialAssignment?.subjectId }
        ?: subjects.firstOrNull()

    var selectedSubject by remember(subjects, initialAssignment) {
        mutableStateOf(defaultSubject)
    }
    var title by remember { mutableStateOf(initialAssignment?.title.orEmpty()) }
    var workType by remember {
        mutableStateOf(initialAssignment?.parsedWorkType ?: WorkType.LAB)
    }
    var stage by remember {
        mutableStateOf(initialAssignment?.parsedStage ?: ProgressStage.DRAFTING)
    }
    var progressPercent by remember {
        mutableIntStateOf(initialAssignment?.progressPercent?.toInt() ?: stage.defaultPercent)
    }
    var currentProgressNote by remember {
        mutableStateOf(initialAssignment?.currentProgressNote.orEmpty())
    }
    var revisionNotes by remember {
        mutableStateOf(initialAssignment?.revisionNotes.orEmpty())
    }
    val checklist = remember {
        mutableStateListOf<String>().apply {
            addAll(initialAssignment?.checklistItems.orEmpty())
        }
    }
    var newChecklistStep by remember { mutableStateOf("") }
    var dueDateMillis by remember {
        mutableLongStateOf(
            initialAssignment?.dueDateMillis ?: (System.currentTimeMillis() + 86_400_000L * 3)
        )
    }
    var priority by remember {
        mutableStateOf(initialAssignment?.parsedPriority ?: AssignmentPriority.MEDIUM)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dueDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать дату")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialAssignment == null) "Новое задание" else "Редактировать задание",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // 1. Subject selection
            Text(
                text = "1. Предмет (дисциплина)",
                style = MaterialTheme.typography.labelLarge,
                color = SoftViolet
            )

            if (subjects.isEmpty()) {
                OutlinedButton(
                    onClick = onOpenAddSubject,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сначала добавьте предмет")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subj ->
                        val isSelected = selectedSubject?.id == subj.id
                        val accent = parseColorSafe(subj.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onOpenAddSubject,
                        label = { Text("+ Предмет") }
                    )
                }
            }

            // 2. Title & WorkType
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название задания / тема") },
                placeholder = { Text("Напр.: Лабораторная №4 по SQL-запросам") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assignment_title_input")
            )

            Text(
                text = "Тип работы",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkType.entries.forEach { type ->
                    FilterChip(
                        selected = workType == type,
                        onClick = { workType = type },
                        label = { Text(type.labelRu) }
                    )
                }
            }

            // 3. Date Selection (Выбор даты) & Priority
            Text(
                text = "2. Срок сдачи (Дедлайн) и приоритет",
                style = MaterialTheme.typography.labelLarge,
                color = SoftViolet
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = NeonTeal
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Дата сдачи",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = dateFormatter.format(Date(dueDateMillis)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.testTag("open_date_picker_button")
                        ) {
                            Text("Выбрать дату")
                        }
                    }

                    // Quick date presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dayMs = 86_400_000L
                        listOf(
                            "Завтра" to 1,
                            "Через 3 дня" to 3,
                            "Через неделю" to 7,
                            "Через 2 недели" to 14
                        ).forEach { (label, days) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    dueDateMillis = System.currentTimeMillis() + dayMs * days
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssignmentPriority.entries.forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.labelRu) }
                            )
                        }
                    }
                }
            }

            // 4. Current Progress Stage & Note (Уточнение на каком именно прогрессе я сейчас)
            Text(
                text = "3. На каком прогрессе вы сейчас?",
                style = MaterialTheme.typography.labelLarge,
                color = NeonTeal
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProgressStage.entries.forEach { st ->
                            val selected = stage == st
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    stage = st
                                    progressPercent = st.defaultPercent
                                },
                                label = { Text(st.labelRu) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonTeal.copy(alpha = 0.22f),
                                    selectedLabelColor = NeonTeal
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stage.shortHintRu,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonTeal
                        )
                    }

                    Slider(
                        value = progressPercent.toFloat(),
                        onValueChange = { progressPercent = it.roundToInt() },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonTeal,
                            activeTrackColor = NeonTeal
                        )
                    )

                    OutlinedTextField(
                        value = currentProgressNote,
                        onValueChange = { currentProgressNote = it },
                        label = { Text("На чём именно остановился?") },
                        placeholder = {
                            Text("Напр.: Написал теорию и 2 таблицы, остановился на выводе и графике")
                        },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("current_progress_note_input")
                    )
                }
            }

            // 5. What needs to be revised / fixed (Что надо доработать)
            Text(
                text = "4. Что надо доработать и исправить?",
                style = MaterialTheme.typography.labelLarge,
                color = AmberWarning
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = revisionNotes,
                        onValueChange = { revisionNotes = it },
                        label = { Text("Замечания преподавателя / что доделать") },
                        placeholder = {
                            Text("Напр.: Переделать список литературы по ГОСТу, исправить формулу на стр. 8")
                        },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("revision_notes_input")
                    )

                    // Checklist items for revisions
                    Text(
                        text = "Чек-лист доработок (${checklist.size}/25)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newChecklistStep,
                            onValueChange = { newChecklistStep = it },
                            placeholder = { Text("Добавить конкретный шаг доработки...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newChecklistStep.isNotBlank() && checklist.size < 25) {
                                    checklist.add("[ ] ${newChecklistStep.trim()}")
                                    newChecklistStep = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricViolet.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить пункт",
                                tint = SoftViolet
                            )
                        }
                    }

                    ChecklistEntry.parseList(checklist).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MidnightSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${entry.text}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { checklist.removeAt(entry.index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Удалить шаг",
                                    tint = CoralDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Gemini AI Breakdown Assistant inside the Editor
                    OutlinedButton(
                        onClick = {
                            onRequestAiBreakdown(
                                selectedSubject?.name ?: "Общий предмет",
                                title,
                                workType,
                                stage,
                                currentProgressNote,
                                revisionNotes
                            )
                        },
                        enabled = aiBreakdownState !is AiBreakdownState.Loading && title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_breakdown_button")
                    ) {
                        if (aiBreakdownState is AiBreakdownState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeonTeal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini анализирует задание...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonTeal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Разбить на шаги доработки с Gemini AI")
                        }
                    }

                    when (aiBreakdownState) {
                        is AiBreakdownState.Success -> {
                            val s = aiBreakdownState.suggestion
                            Surface(
                                color = ElectricViolet.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Рекомендация Gemini AI",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NeonTeal
                                    )
                                    Text(
                                        text = "Следующий шаг (15 мин): ${s.nextImmediateStep}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Совет: ${s.studyTip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = {
                                            if (revisionNotes.isBlank()) {
                                                revisionNotes = s.revisionSummary
                                            }
                                            s.subTasks.forEach { step ->
                                                if (checklist.size < 25) {
                                                    checklist.add("[ ] $step")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Добавить ${s.subTasks.size} шагов в чек-лист доработок")
                                    }
                                }
                            }
                        }
                        is AiBreakdownState.Error -> {
                            Text(
                                text = aiBreakdownState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralDanger
                            )
                        }
                        else -> Unit
                    }
                }
            }

            Button(
                onClick = {
                    val targetSubject = selectedSubject ?: return@Button
                    onSave(
                        initialAssignment?.id,
                        targetSubject,
                        title,
                        workType,
                        stage,
                        progressPercent,
                        currentProgressNote,
                        revisionNotes,
                        checklist.toList(),
                        dueDateMillis,
                        priority
                    )
                },
                enabled = title.isNotBlank() && selectedSubject != null,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_assignment_button")
            ) {
                Text(
                    text = if (initialAssignment == null) "Сохранить задание" else "Обновить задание",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
