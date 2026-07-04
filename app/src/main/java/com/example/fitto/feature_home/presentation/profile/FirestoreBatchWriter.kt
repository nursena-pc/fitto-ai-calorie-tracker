package com.example.fitto.feature_home.presentation.profile

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal class FirestoreBatchWriter(
    private val firestore: FirebaseFirestore
) {
    private var batch = firestore.batch()
    private var operationCount = 0

    suspend fun update(
        reference: DocumentReference,
        updates: Map<String, Any>
    ) {
        batch.update(reference, updates)
        operationCount++
        commitIfNeeded()
    }

    suspend fun setMerge(
        reference: DocumentReference,
        updates: Map<String, Any?>
    ) {
        batch.set(reference, updates, SetOptions.merge())
        operationCount++
        commitIfNeeded()
    }

    suspend fun delete(reference: DocumentReference) {
        batch.delete(reference)
        operationCount++
        commitIfNeeded()
    }

    private suspend fun commitIfNeeded() {
        if (operationCount >= MAX_BATCH_OPERATIONS) {
            commit()
        }
    }

    suspend fun commit() {
        if (operationCount <= 0) return

        batch.commit().await()
        batch = firestore.batch()
        operationCount = 0
    }

    private companion object {
        const val MAX_BATCH_OPERATIONS = 450
    }
}
