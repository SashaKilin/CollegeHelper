package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Assignment
import com.example.data.model.ProgressStage
import com.example.data.model.Subject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyRepository(private val db: FirebaseFirestore) {

    // Convenience constructor using the provisioned enterprise database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Необходимо войти через Google для работы с данными.")
    }

    fun observeSubjects(userId: String = auth.currentUser?.uid.orEmpty()): Flow<List<Subject>> = flow {
        val targetUid = userId.ifBlank {
            auth.currentUser?.uid ?: "unauthenticated"
        }
        val path = "users/$targetUid/subjects"
        emitAll(
            db.collection("users").document(targetUid).collection("subjects")
                .whereEqualTo("userId", targetUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Subject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAssignments(userId: String = auth.currentUser?.uid.orEmpty()): Flow<List<Assignment>> = flow {
        val targetUid = userId.ifBlank {
            auth.currentUser?.uid ?: "unauthenticated"
        }
        val path = "users/$targetUid/assignments"
        emitAll(
            db.collection("users").document(targetUid).collection("assignments")
                .whereEqualTo("userId", targetUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Assignment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<Assignment> { it.isSubmitted }
                            .thenBy { it.dueDateMillis }
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSubject(
        name: String,
        teacher: String,
        colorHex: String,
        iconKey: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("subjects")
        val docRef = colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(80),
            "teacher" to teacher.trim().take(100),
            "colorHex" to colorHex.trim().take(9),
            "iconKey" to iconKey.trim().take(30),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("subjects").document(subjectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createAssignment(
        subjectId: String,
        subjectName: String,
        subjectColorHex: String,
        title: String,
        workType: String,
        stage: String,
        progressPercent: Int,
        currentProgressNote: String,
        revisionNotes: String,
        checklistItems: List<String>,
        dueDateMillis: Long,
        priority: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("assignments")
        val docRef = colRef.document()
        val sanitizedChecklist = checklistItems
            .map { it.trim().take(300) }
            .filter { it.isNotEmpty() }
            .take(25)

        val payload = mapOf(
            "userId" to uid,
            "subjectId" to subjectId.trim().take(128),
            "subjectName" to subjectName.trim().take(80),
            "subjectColorHex" to subjectColorHex.trim().take(9),
            "title" to title.trim().take(160),
            "workType" to workType,
            "stage" to stage,
            "progressPercent" to progressPercent.coerceIn(0, 100).toLong(),
            "currentProgressNote" to currentProgressNote.trim().take(1000),
            "revisionNotes" to revisionNotes.trim().take(1500),
            "checklistItems" to sanitizedChecklist,
            "dueDateMillis" to dueDateMillis.coerceAtLeast(1L),
            "priority" to priority,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAssignment(assignment: Assignment): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(assignment.id.isNotBlank()) { "Assignment ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("assignments").document(assignment.id)

        val sanitizedChecklist = assignment.checklistItems
            .map { it.trim().take(300) }
            .filter { it.isNotEmpty() }
            .take(25)

        val updates = mapOf(
            "subjectId" to assignment.subjectId.trim().take(128),
            "subjectName" to assignment.subjectName.trim().take(80),
            "subjectColorHex" to assignment.subjectColorHex.trim().take(9),
            "title" to assignment.title.trim().take(160),
            "workType" to assignment.workType,
            "stage" to assignment.stage,
            "progressPercent" to assignment.progressPercent.coerceIn(0L, 100L),
            "currentProgressNote" to assignment.currentProgressNote.trim().take(1000),
            "revisionNotes" to assignment.revisionNotes.trim().take(1500),
            "checklistItems" to sanitizedChecklist,
            "dueDateMillis" to assignment.dueDateMillis.coerceAtLeast(1L),
            "priority" to assignment.priority,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStageAndProgress(
        assignment: Assignment,
        newStage: ProgressStage,
        newPercent: Int,
        newProgressNote: String? = null
    ): Result<Unit> {
        val updated = assignment.copy(
            stage = newStage.code,
            progressPercent = newPercent.coerceIn(0, 100).toLong(),
            currentProgressNote = newProgressNote ?: assignment.currentProgressNote
        )
        return updateAssignment(updated)
    }

    suspend fun deleteAssignment(assignmentId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("assignments").document(assignmentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getAssignmentForUser(ownerUid: String, assignmentId: String): Result<Assignment?> {
        val docRef = db.collection("users").document(ownerUid)
            .collection("assignments").document(assignmentId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    Assignment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }
}
