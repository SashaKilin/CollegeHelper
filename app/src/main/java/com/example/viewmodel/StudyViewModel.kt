package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiBreakdownSuggestion
import com.example.data.model.Assignment
import com.example.data.model.AssignmentPriority
import com.example.data.model.ChecklistEntry
import com.example.data.model.ProgressStage
import com.example.data.model.Subject
import com.example.data.model.WorkType
import com.example.data.repository.StudyRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiBreakdownState {
    data object Idle : AiBreakdownState
    data object Loading : AiBreakdownState
    data class Success(val suggestion: AiBreakdownSuggestion) : AiBreakdownState
    data class Error(val message: String) : AiBreakdownState
}

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Success(val text: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

class StudyViewModel(
    private val repository: StudyRepository,
    private val currentUserId: String
) : ViewModel() {

    val subjectsState: StateFlow<UiState<List<Subject>>> = repository
        .observeSubjects(currentUserId)
        .map<List<Subject>, UiState<List<Subject>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing subjects", error)
            emit(UiState.Error(error.localizedMessage ?: "Не удалось загрузить предметы"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val assignmentsState: StateFlow<UiState<List<Assignment>>> = repository
        .observeAssignments(currentUserId)
        .map<List<Assignment>, UiState<List<Assignment>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing assignments", error)
            emit(UiState.Error(error.localizedMessage ?: "Не удалось загрузить задания"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    private val _selectedStageCode = MutableStateFlow<String?>(null)
    val selectedStageCode: StateFlow<String?> = _selectedStageCode.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _aiBreakdownState = MutableStateFlow<AiBreakdownState>(AiBreakdownState.Idle)
    val aiBreakdownState: StateFlow<AiBreakdownState> = _aiBreakdownState.asStateFlow()

    private val _aiStreamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val aiStreamState: StateFlow<AiStreamState> = _aiStreamState.asStateFlow()

    // Structured JSON schema for assignment breakdown & revision checklist
    private val breakdownSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "nextImmediateStep" to Schema.string(
                    description = "Конкретный первый шаг на 15-20 минут, с которого студенту проще всего продолжить"
                ),
                "revisionSummary" to Schema.string(
                    description = "Краткое резюме того, что именно нужно доработать и проверить перед сдачей"
                ),
                "subTasks" to Schema.array(
                    items = Schema.string(),
                    description = "Список из 4-5 конкретных шагов доработки для чек-листа"
                ),
                "studyTip" to Schema.string(
                    description = "Полезный совет по оформлению или защите этого типа задания в колледже"
                )
            )
        )
    }

    private val structuredAiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = breakdownSchema
            }
        )
    }

    private val chatAiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun selectSubjectFilter(subjectId: String?) {
        _selectedSubjectId.value = if (_selectedSubjectId.value == subjectId) null else subjectId
    }

    fun selectStageFilter(stageCode: String?) {
        _selectedStageCode.value = if (_selectedStageCode.value == stageCode) null else stageCode
    }

    fun clearError() {
        _actionError.value = null
    }

    fun resetAiBreakdown() {
        _aiBreakdownState.value = AiBreakdownState.Idle
    }

    fun addSubject(name: String, teacher: String, colorHex: String, iconKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = repository.createSubject(name, teacher, colorHex, iconKey)
            result.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Ошибка сохранения предмета"
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            val result = repository.deleteSubject(subjectId)
            if (_selectedSubjectId.value == subjectId) {
                _selectedSubjectId.value = null
            }
            result.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Ошибка удаления предмета"
            }
        }
    }

    fun seedStarterSubjects() {
        viewModelScope.launch {
            val starter = listOf(
                Triple("Программирование и алгоритмы", "ауд. 308 • Практика", "#7C5CFF" to "CODE"),
                Triple("Высшая математика", "ауд. 214 • Лекции и семинары", "#2DD4BF" to "MATH"),
                Triple("Базы данных и SQL", "ауд. 402 • Лабораторные", "#38BDF8" to "LAB"),
                Triple("Операционные системы", "ауд. 310 • Курсовой проект", "#F59E0B" to "SCIENCE"),
                Triple("Английский язык (IT)", "ауд. 105 • Семинар", "#EC4899" to "BOOK")
            )
            for ((name, teacher, meta) in starter) {
                repository.createSubject(name, teacher, meta.first, meta.second)
            }
        }
    }

    fun saveAssignment(
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
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = if (existingId.isNullOrBlank()) {
                repository.createAssignment(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    subjectColorHex = subject.colorHex,
                    title = title,
                    workType = workType.code,
                    stage = stage.code,
                    progressPercent = progressPercent,
                    currentProgressNote = currentProgressNote,
                    revisionNotes = revisionNotes,
                    checklistItems = checklistItems,
                    dueDateMillis = dueDateMillis,
                    priority = priority.code
                )
            } else {
                val currentList = (assignmentsState.value as? UiState.Success)?.data.orEmpty()
                val existing = currentList.firstOrNull { it.id == existingId }
                if (existing != null) {
                    repository.updateAssignment(
                        existing.copy(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            subjectColorHex = subject.colorHex,
                            title = title,
                            workType = workType.code,
                            stage = stage.code,
                            progressPercent = progressPercent.toLong(),
                            currentProgressNote = currentProgressNote,
                            revisionNotes = revisionNotes,
                            checklistItems = checklistItems,
                            dueDateMillis = dueDateMillis,
                            priority = priority.code
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Задание не найдено"))
                }
            }
            result.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Не удалось сохранить задание"
            }
        }
    }

    fun quickUpdateStageAndProgress(
        assignment: Assignment,
        newStage: ProgressStage,
        newPercent: Int,
        newProgressNote: String = assignment.currentProgressNote,
        newRevisionNotes: String = assignment.revisionNotes
    ) {
        viewModelScope.launch {
            val updated = assignment.copy(
                stage = newStage.code,
                progressPercent = newPercent.coerceIn(0, 100).toLong(),
                currentProgressNote = newProgressNote,
                revisionNotes = newRevisionNotes
            )
            repository.updateAssignment(updated).onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Не удалось обновить прогресс"
            }
        }
    }

    fun toggleChecklistItem(assignment: Assignment, itemIndex: Int) {
        viewModelScope.launch {
            val parsed = ChecklistEntry.parseList(assignment.checklistItems).toMutableList()
            if (itemIndex in parsed.indices) {
                val item = parsed[itemIndex]
                parsed[itemIndex] = item.copy(isDone = !item.isDone)
                val newList = parsed.map { it.toSerializedString() }
                // Automatically bump progress if items are completed
                val doneCount = parsed.count { it.isDone }
                val totalCount = parsed.size
                val calculatedPercent = if (totalCount > 0 && doneCount == totalCount) {
                    maxOf(assignment.progressPercent.toInt(), 95)
                } else {
                    assignment.progressPercent.toInt()
                }
                repository.updateAssignment(
                    assignment.copy(
                        checklistItems = newList,
                        progressPercent = calculatedPercent.toLong()
                    )
                ).onFailure { e ->
                    _actionError.value = e.localizedMessage ?: "Не удалось обновить чек-лист"
                }
            }
        }
    }

    fun addChecklistStepToAssignment(assignment: Assignment, newStepText: String) {
        val clean = newStepText.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val updatedItems = (assignment.checklistItems + "[ ] $clean").take(25)
            repository.updateAssignment(
                assignment.copy(checklistItems = updatedItems)
            ).onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Не удалось добавить пункт доработки"
            }
        }
    }

    fun deleteAssignment(assignmentId: String) {
        viewModelScope.launch {
            repository.deleteAssignment(assignmentId).onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Ошибка удаления задания"
            }
        }
    }

    fun generateAssignmentBreakdown(
        subjectName: String,
        title: String,
        workType: WorkType,
        stage: ProgressStage,
        currentProgressNote: String,
        revisionNotes: String
    ) {
        if (title.isBlank()) {
            _aiBreakdownState.value = AiBreakdownState.Error("Сначала введите название задания")
            return
        }
        viewModelScope.launch {
            _aiBreakdownState.value = AiBreakdownState.Loading
            val prompt = buildString {
                appendLine("Ты — опытный академический наставник для студента колледжа.")
                appendLine("Предмет: $subjectName")
                appendLine("Тип работы: ${workType.labelRu}")
                appendLine("Задание: $title")
                appendLine("Текущий этап: ${stage.labelRu}")
                if (currentProgressNote.isNotBlank()) {
                    appendLine("На каком прогрессе сейчас / на чём остановился: $currentProgressNote")
                }
                if (revisionNotes.isNotBlank()) {
                    appendLine("Что надо доработать / замечания: $revisionNotes")
                }
                appendLine("Составь конкретный план дальнейших шагов и доработок на русском языке.")
            }

            try {
                val response = withTimeout(60_000L) {
                    structuredAiModel.generateContent(prompt)
                }
                val jsonString = response.text
                    ?: throw IllegalStateException("Модель вернула пустой ответ.")
                val json = JSONObject(jsonString)
                val nextStep = json.optString("nextImmediateStep", "Открыть материалы и выполнить первый пункт")
                val revSummary = json.optString("revisionSummary", "Проверить оформление и закрыть замечания")
                val tip = json.optString("studyTip", "Проверьте соответствие методичке перед сдачей.")
                val arr = json.optJSONArray("subTasks")
                val subTasks = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotEmpty()) subTasks.add(s)
                    }
                }
                _aiBreakdownState.value = AiBreakdownState.Success(
                    AiBreakdownSuggestion(
                        nextImmediateStep = nextStep,
                        revisionSummary = revSummary,
                        subTasks = subTasks,
                        studyTip = tip
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI breakdown timed out", e)
                _aiBreakdownState.value = AiBreakdownState.Error(
                    "Превышено время ожидания ответа от Gemini (60 сек). Попробуйте ещё раз."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI breakdown failed", e)
                _aiBreakdownState.value = AiBreakdownState.Error(
                    e.localizedMessage ?: "Не удалось сгенерировать разбор задания"
                )
            }
        }
    }

    fun generateDailyStudyPlan(assignments: List<Assignment>) {
        viewModelScope.launch {
            _aiStreamState.value = AiStreamState.Loading
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("ru"))
            val activeAssignments = assignments.filter { !it.isSubmitted }
            val prompt = if (activeAssignments.isEmpty()) {
                "У студента колледжа сейчас нет активных долгов или несданных заданий. Напиши короткий вдохновляющий совет на русском языке, как эффективно вести учёт лабораторных, курсовых и правок в новом семестре."
            } else {
                buildString {
                    appendLine("Ты — умный помощник студента колледжа. Проанализируй текущие задания студента и составь чёткий, мотивирующий план работы на сегодня и ближайшие дни на русском языке.")
                    appendLine("Обрати особое внимание на то, на каком прогрессе студент остановился и что именно ему надо доработать:")
                    activeAssignments.forEachIndexed { idx, a ->
                        val dueStr = dateFormat.format(Date(a.dueDateMillis))
                        appendLine("${idx + 1}. [${a.subjectName}] ${a.title} (${a.parsedWorkType.labelRu})")
                        appendLine("   - Дедлайн: $dueStr, Приоритет: ${a.parsedPriority.labelRu}")
                        appendLine("   - Этап: ${a.parsedStage.labelRu} (${a.progressPercent}%)")
                        if (a.currentProgressNote.isNotBlank()) {
                            appendLine("   - На чём остановился: ${a.currentProgressNote}")
                        }
                        if (a.revisionNotes.isNotBlank()) {
                            appendLine("   - Что надо доработать: ${a.revisionNotes}")
                        }
                    }
                    appendLine("\nВыдели: 1) Главный фокус на сегодня (с чего начать прямо сейчас), 2) План закрытия доработок и правок, 3) Совет по распределению времени.")
                }
            }
            streamPrompt(prompt)
        }
    }

    fun askAiStudyCoach(userQuestion: String, assignments: List<Assignment>) {
        if (userQuestion.isBlank()) return
        viewModelScope.launch {
            _aiStreamState.value = AiStreamState.Loading
            val contextSummary = assignments.take(8).joinToString("\n") { a ->
                "- [${a.subjectName}] ${a.title}: этап ${a.parsedStage.labelRu} (${a.progressPercent}%), остановился на: '${a.currentProgressNote}', доработать: '${a.revisionNotes}'"
            }
            val prompt = buildString {
                appendLine("Ты — AI-помощник студента колледжа в приложении СтудПульс. Отвечай по делу, структурированно и на русском языке.")
                if (contextSummary.isNotBlank()) {
                    appendLine("Текущие задания студента для контекста:\n$contextSummary\n")
                }
                appendLine("Запрос студента: $userQuestion")
            }
            streamPrompt(prompt)
        }
    }

    private suspend fun streamPrompt(prompt: String) {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatAiModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    accumulated.append(piece)
                    _aiStreamState.value = AiStreamState.Streaming(accumulated.toString())
                }
            }
            if (accumulated.isNotBlank()) {
                _aiStreamState.value = AiStreamState.Success(accumulated.toString())
            } else {
                _aiStreamState.value = AiStreamState.Error("Gemini вернул пустой ответ.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI streaming timed out", e)
            _aiStreamState.value = AiStreamState.Error("Время ожидания ответа истекло (60 сек).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI streaming failed", e)
            _aiStreamState.value = AiStreamState.Error(
                e.localizedMessage ?: "Ошибка обращения к Gemini AI"
            )
        }
    }

    private companion object {
        const val TAG = "StudyViewModel"
    }
}
