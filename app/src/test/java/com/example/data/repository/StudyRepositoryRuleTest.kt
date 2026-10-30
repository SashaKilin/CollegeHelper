package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AssignmentPriority
import com.example.data.model.ProgressStage
import com.example.data.model.WorkType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveSubjectAndAssignment_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudyRepository(firestore)

        val subjectResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSubject(
                name = "Программирование на Kotlin",
                teacher = "Петров П.П., ауд. 212",
                colorHex = "#7C5CFF",
                iconKey = "CODE"
            )
        }
        assertTrue(subjectResult.isSuccess)
        val subjectId = subjectResult.getOrThrow()

        val assignmentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAssignment(
                subjectId = subjectId,
                subjectName = "Программирование на Kotlin",
                subjectColorHex = "#7C5CFF",
                title = "Курсовая работа: Мобильное приложение",
                workType = WorkType.COURSEWORK.code,
                stage = ProgressStage.REVISION.code,
                progressPercent = 75,
                currentProgressNote = "Сделан экран авторизации и база данных, пишу отчёт",
                revisionNotes = "Добавить диаграмму классов в 2 главу и тесты",
                checklistItems = listOf("[x] Сделать UI", "[ ] Добавить UML диаграмму"),
                dueDateMillis = System.currentTimeMillis() + 86_400_000L * 5,
                priority = AssignmentPriority.HIGH.code
            )
        }
        assertTrue(assignmentResult.isSuccess)
        val assignmentId = assignmentResult.getOrThrow()

        val emittedAssignments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAssignments(aliceUid).first { list -> list.any { it.id == assignmentId } }
        }
        val created = emittedAssignments.first { it.id == assignmentId }
        assertEquals("Курсовая работа: Мобильное приложение", created.title)
        assertEquals(75L, created.progressPercent)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStageAndProgress(
                assignment = created,
                newStage = ProgressStage.READY,
                newPercent = 95,
                newProgressNote = "Диаграмма добавлена, отчёт распечатан"
            )
        }
        assertTrue(updateResult.isSuccess)
    }

    @Test
    fun getAssignment_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudyRepository(firestore)

        val assignmentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAssignment(
                subjectId = "subj_1",
                subjectName = "Физика",
                subjectColorHex = "#2DD4BF",
                title = "Лабораторная работа №2",
                workType = WorkType.LAB.code,
                stage = ProgressStage.DRAFTING.code,
                progressPercent = 50,
                currentProgressNote = "Сняты замеры",
                revisionNotes = "Посчитать погрешность",
                checklistItems = listOf("[ ] Таблица погрешностей"),
                dueDateMillis = System.currentTimeMillis() + 86_400_000L,
                priority = AssignmentPriority.MEDIUM.code
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudyRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getAssignmentForUser(aliceUid, assignmentId)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeAssignments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StudyRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAssignments("alice_unauth_check").first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Expected exception for unauthenticated query", caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@studpulse.test"
        const val BOB_EMAIL = "bob@studpulse.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
