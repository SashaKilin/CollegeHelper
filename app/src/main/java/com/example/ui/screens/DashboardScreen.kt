package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOutFromGoogle
import com.example.data.model.Assignment
import com.example.data.model.ChecklistEntry
import com.example.data.model.ProgressStage
import com.example.data.model.Subject
import com.example.data.repository.StudyRepository
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElevatedCardBg
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudyViewModel
import com.example.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MainStudyDashboard(
    currentUserId: String,
    userEmail: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudyViewModel(StudyRepository(db), currentUserId)
            }
        }
    )

    val subjectsState by viewModel.subjectsState.collectAsStateWithLifecycle()
    val assignmentsState by viewModel.assignmentsState.collectAsStateWithLifecycle()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedStageCode by viewModel.selectedStageCode.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val aiBreakdownState by viewModel.aiBreakdownState.collectAsStateWithLifecycle()
    val aiStreamState by viewModel.aiStreamState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddSubjectDialog by rememberSaveable { mutableStateOf(false) }
    var showAssignmentSheet by rememberSaveable { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionError) {
        actionError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val allAssignments = (assignmentsState as? UiState.Success)?.data.orEmpty()

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, teacher, colorHex, iconKey ->
                viewModel.addSubject(name, teacher, colorHex, iconKey)
                showAddSubjectDialog = false
            }
        )
    }

    if (showAssignmentSheet) {
        AssignmentEditorSheet(
            subjects = subjects,
            initialAssignment = editingAssignment,
            aiBreakdownState = aiBreakdownState,
            onRequestAiBreakdown = { subjName, title, workType, stage, progNote, revNote ->
                viewModel.generateAssignmentBreakdown(
                    subjName,
                    title,
                    workType,
                    stage,
                    progNote,
                    revNote
                )
            },
            onDismiss = {
                showAssignmentSheet = false
                editingAssignment = null
                viewModel.resetAiBreakdown()
            },
            onSave = { id, subj, title, wType, stage, pct, progNote, revNote, checklist, dueMs, prio ->
                viewModel.saveAssignment(
                    id,
                    subj,
                    title,
                    wType,
                    stage,
                    pct,
                    progNote,
                    revNote,
                    checklist,
                    dueMs,
                    prio
                )
                showAssignmentSheet = false
                editingAssignment = null
                viewModel.resetAiBreakdown()
            },
            onOpenAddSubject = {
                showAddSubjectDialog = true
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopHeaderBar(
                userEmail = userEmail,
                allAssignments = allAssignments,
                onSignOut = {
                    signOutFromGoogle(
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignedOut,
                        scope = scope
                    )
                }
            )
        },
        floatingActionButton = {
            when (currentTab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = {
                        editingAssignment = null
                        viewModel.resetAiBreakdown()
                        if (subjects.isEmpty()) {
                            showAddSubjectDialog = true
                        } else {
                            showAssignmentSheet = true
                        }
                    },
                    containerColor = ElectricViolet,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_add_assignment)) },
                    modifier = Modifier.testTag("fab_add_assignment")
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = { showAddSubjectDialog = true },
                    containerColor = NeonTeal,
                    contentColor = ObsidianBg,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_add_subject)) },
                    modifier = Modifier.testTag("fab_add_subject")
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MidnightSurface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_assignments)) },
                    modifier = Modifier.testTag("nav_tab_assignments")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_subjects)) },
                    modifier = Modifier.testTag("nav_tab_subjects")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_ai_assistant)) },
                    modifier = Modifier.testTag("nav_tab_ai")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> AssignmentsTabContent(
                    subjects = subjects,
                    assignmentsState = assignmentsState,
                    selectedSubjectId = selectedSubjectId,
                    selectedStageCode = selectedStageCode,
                    onSelectSubject = viewModel::selectSubjectFilter,
                    onSelectStage = viewModel::selectStageFilter,
                    onSeedSubjects = viewModel::seedStarterSubjects,
                    onAddAssignmentClick = {
                        editingAssignment = null
                        viewModel.resetAiBreakdown()
                        if (subjects.isEmpty()) showAddSubjectDialog = true else showAssignmentSheet = true
                    },
                    onEditAssignment = { assignment ->
                        editingAssignment = assignment
                        viewModel.resetAiBreakdown()
                        showAssignmentSheet = true
                    },
                    onQuickStageChange = { assignment, stage ->
                        viewModel.quickUpdateStageAndProgress(
                            assignment = assignment,
                            newStage = stage,
                            newPercent = stage.defaultPercent
                        )
                    },
                    onToggleChecklist = viewModel::toggleChecklistItem,
                    onAddChecklistStep = viewModel::addChecklistStepToAssignment,
                    onDeleteAssignment = viewModel::deleteAssignment
                )
                1 -> SubjectsTabContent(
                    subjectsState = subjectsState,
                    allAssignments = allAssignments,
                    onSeedStarterSubjects = viewModel::seedStarterSubjects,
                    onAddSubject = { showAddSubjectDialog = true },
                    onDeleteSubject = viewModel::deleteSubject,
                    onOpenSubjectAssignments = { subjId ->
                        viewModel.selectSubjectFilter(subjId)
                        currentTab = 0
                    }
                )
                2 -> AiAssistantScreen(
                    assignments = allAssignments,
                    aiStreamState = aiStreamState,
                    onGenerateDailyPlan = { viewModel.generateDailyStudyPlan(allAssignments) },
                    onAskQuestion = { q -> viewModel.askAiStudyCoach(q, allAssignments) }
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    userEmail: String?,
    allAssignments: List<Assignment>,
    onSignOut: () -> Unit
) {
    val totalCount = allAssignments.size
    val submittedCount = allAssignments.count { it.isSubmitted }
    val revisionCount = allAssignments.count { !it.isSubmitted && it.needsRevision }
    val avgProgress = if (allAssignments.isEmpty()) {
        0
    } else {
        allAssignments.map { it.progressPercent }.average().toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightSurface, ObsidianBg)
                )
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricViolet.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = NeonTeal
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userEmail ?: "Студент колледжа",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.btn_sign_out),
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPill(
                label = "Общий прогресс",
                value = "$avgProgress%",
                accent = NeonTeal,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "На доработке",
                value = "$revisionCount",
                accent = AmberWarning,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Сдано",
                value = "$submittedCount/$totalCount",
                accent = SoftViolet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ElevatedCardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AssignmentsTabContent(
    subjects: List<Subject>,
    assignmentsState: UiState<List<Assignment>>,
    selectedSubjectId: String?,
    selectedStageCode: String?,
    onSelectSubject: (String?) -> Unit,
    onSelectStage: (String?) -> Unit,
    onSeedSubjects: () -> Unit,
    onAddAssignmentClick: () -> Unit,
    onEditAssignment: (Assignment) -> Unit,
    onQuickStageChange: (Assignment, ProgressStage) -> Unit,
    onToggleChecklist: (Assignment, Int) -> Unit,
    onAddChecklistStep: (Assignment, String) -> Unit,
    onDeleteAssignment: (String) -> Unit
) {
    when (assignmentsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonTeal)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = assignmentsState.message,
                    color = CoralDanger,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is UiState.Success -> {
            val all = assignmentsState.data
            val filtered = remember(all, selectedSubjectId, selectedStageCode) {
                all.filter { item ->
                    val matchSubject = selectedSubjectId == null || item.subjectId == selectedSubjectId
                    val matchStage = selectedStageCode == null || item.stage == selectedStageCode
                    matchSubject && matchStage
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject Filter Row
                if (subjects.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Фильтр по предметам",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedSubjectId == null,
                                    onClick = { onSelectSubject(null) },
                                    label = { Text("Все предметы") }
                                )
                                subjects.forEach { subj ->
                                    val c = parseColorSafe(subj.colorHex)
                                    FilterChip(
                                        selected = selectedSubjectId == subj.id,
                                        onClick = { onSelectSubject(subj.id) },
                                        label = { Text(subj.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = c.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Stage Filter Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedStageCode == null,
                            onClick = { onSelectStage(null) },
                            label = { Text("Все этапы") }
                        )
                        ProgressStage.entries.forEach { st ->
                            FilterChip(
                                selected = selectedStageCode == st.code,
                                onClick = { onSelectStage(st.code) },
                                label = { Text(st.labelRu) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyAssignmentsState(
                            hasSubjects = subjects.isNotEmpty(),
                            onSeedSubjects = onSeedSubjects,
                            onAddAssignment = onAddAssignmentClick
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onEdit = { onEditAssignment(assignment) },
                            onQuickStageChange = { newStage ->
                                onQuickStageChange(assignment, newStage)
                            },
                            onToggleChecklist = { idx ->
                                onToggleChecklist(assignment, idx)
                            },
                            onAddChecklistStep = { stepText ->
                                onAddChecklistStep(assignment, stepText)
                            },
                            onDelete = { onDeleteAssignment(assignment.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignmentCard(
    assignment: Assignment,
    onEdit: () -> Unit,
    onQuickStageChange: (ProgressStage) -> Unit,
    onToggleChecklist: (Int) -> Unit,
    onAddChecklistStep: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by rememberSaveable(assignment.id) { mutableStateOf(true) }
    var quickRevisionInput by remember { mutableStateOf("") }
    val subjectColor = parseColorSafe(assignment.subjectColorHex)
    val stage = assignment.parsedStage
    val checklistEntries = remember(assignment.checklistItems) {
        ChecklistEntry.parseList(assignment.checklistItems)
    }

    val dateFormatter = remember {
        SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
    }
    val daysDiff = remember(assignment.dueDateMillis) {
        val diffMs = assignment.dueDateMillis - System.currentTimeMillis()
        TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    val deadlineColor = when {
        assignment.isSubmitted -> EmeraldReady
        daysDiff < 0 -> CoralDanger
        daysDiff <= 2 -> AmberWarning
        else -> SoftViolet
    }

    val deadlineBadgeText = when {
        assignment.isSubmitted -> "Сдано • ${dateFormatter.format(Date(assignment.dueDateMillis))}"
        daysDiff < 0 -> "Просрочено (${dateFormatter.format(Date(assignment.dueDateMillis))})"
        daysDiff == 0L -> "Сдать сегодня!"
        daysDiff == 1L -> "Сдать завтра (${dateFormatter.format(Date(assignment.dueDateMillis))})"
        else -> "До ${dateFormatter.format(Date(assignment.dueDateMillis))} ($daysDiff дн.)"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (stage == ProgressStage.REVISION) AmberWarning.copy(alpha = 0.55f) else GlassBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("assignment_card_${assignment.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top badges: Subject + WorkType + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = subjectColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, subjectColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = assignment.subjectName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = MidnightSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = assignment.parsedWorkType.labelRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать задание",
                            tint = SoftViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить задание",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Title & Deadline
            Text(
                text = assignment.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = deadlineColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = deadlineBadgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = deadlineColor
                    )
                }

                Surface(
                    color = when (stage) {
                        ProgressStage.REVISION -> AmberWarning.copy(alpha = 0.18f)
                        ProgressStage.SUBMITTED -> EmeraldReady.copy(alpha = 0.18f)
                        else -> NeonTeal.copy(alpha = 0.16f)
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = "${stage.labelRu} • ${assignment.progressPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = when (stage) {
                            ProgressStage.REVISION -> AmberWarning
                            ProgressStage.SUBMITTED -> EmeraldReady
                            else -> NeonTeal
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (assignment.progressPercent.coerceIn(0L, 100L)) / 100f },
                color = when (stage) {
                    ProgressStage.REVISION -> AmberWarning
                    ProgressStage.SUBMITTED -> EmeraldReady
                    else -> NeonTeal
                },
                trackColor = MidnightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            // Current stop-point box ("На каком именно прогрессе я сейчас")
            if (assignment.currentProgressNote.isNotBlank()) {
                Surface(
                    color = MidnightSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonTeal.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "НА КАКОМ ПРОГРЕССЕ СЕЙЧАС:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = assignment.currentProgressNote,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // What needs to be revised ("Что надо доработать")
            if (assignment.revisionNotes.isNotBlank() || checklistEntries.isNotEmpty()) {
                Surface(
                    color = AmberWarning.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberWarning.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ЧТО НАДО ДОРАБОТАТЬ:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning
                            )
                        }

                        if (assignment.revisionNotes.isNotBlank()) {
                            Text(
                                text = assignment.revisionNotes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        checklistEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleChecklist(entry.index) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.isDone) {
                                        Icons.Default.CheckBox
                                    } else {
                                        Icons.Default.CheckBoxOutlineBlank
                                    },
                                    contentDescription = "Отметить пункт",
                                    tint = if (entry.isDone) EmeraldReady else AmberWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (entry.isDone) TextDecoration.LineThrough else null,
                                    color = if (entry.isDone) TextSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Expandable quick stage switcher & inline revision adder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Скрыть быстрое управление этапом" else "Сменить этап / добавить правку",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftViolet
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SoftViolet
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProgressStage.entries.forEach { st ->
                            val isCurrent = stage == st
                            FilterChip(
                                selected = isCurrent,
                                onClick = { onQuickStageChange(st) },
                                label = { Text(st.labelRu, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickRevisionInput,
                            onValueChange = { quickRevisionInput = it },
                            placeholder = { Text("Быстро записать, что ещё доработать...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (quickRevisionInput.isNotBlank()) {
                                    onAddChecklistStep(quickRevisionInput)
                                    quickRevisionInput = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricViolet.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить доработку",
                                tint = NeonTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAssignmentsState(
    hasSubjects: Boolean,
    onSeedSubjects: () -> Unit,
    onAddAssignment: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = SoftViolet,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.empty_assignments_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.empty_assignments_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (!hasSubjects) {
                OutlinedButton(
                    onClick = onSeedSubjects,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_subjects_button")
                ) {
                    Text(stringResource(R.string.btn_seed_subjects))
                }
            }

            Button(
                onClick = onAddAssignment,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.fab_add_assignment))
            }
        }
    }
}

@Composable
private fun SubjectsTabContent(
    subjectsState: UiState<List<Subject>>,
    allAssignments: List<Assignment>,
    onSeedStarterSubjects: () -> Unit,
    onAddSubject: () -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenSubjectAssignments: (String) -> Unit
) {
    when (subjectsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonTeal)
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(subjectsState.message, color = CoralDanger)
            }
        }
        is UiState.Success -> {
            val subjects = subjectsState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (subjects.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = NeonTeal,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_subjects_title),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(R.string.empty_subjects_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = onSeedStarterSubjects,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.btn_seed_subjects))
                                }
                                OutlinedButton(
                                    onClick = onAddSubject,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Создать свой предмет")
                                }
                            }
                        }
                    }
                } else {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectAssignments = allAssignments.filter { it.subjectId == subject.id }
                        val activeCount = subjectAssignments.count { !it.isSubmitted }
                        val revisionCount = subjectAssignments.count { !it.isSubmitted && it.needsRevision }
                        val accent = parseColorSafe(subject.colorHex)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = ElevatedCardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                                .clickable { onOpenSubjectAssignments(subject.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accent.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = accent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (subject.teacher.isNotBlank()) {
                                            Text(
                                                text = subject.teacher,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Активных заданий: $activeCount • На доработке: $revisionCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (revisionCount > 0) AmberWarning else NeonTeal
                                        )
                                    }
                                }

                                IconButton(onClick = { onDeleteSubject(subject.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Удалить предмет",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
