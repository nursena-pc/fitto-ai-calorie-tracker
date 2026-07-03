package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class FirebaseSocialRealtimeObservationSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val realtimeDatabase: FirebaseDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : SocialRealtimeObservationSource {

    override fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel> = callbackFlow {
        if (SocialRealtimePresenceResolver.shouldSkipUserPresence(userId)) {
            trySend(SocialRealtimePresenceResolver.offlineStatus(userId))
            close()
            return@callbackFlow
        }

        val statusRef = realtimeDatabase.reference
            .child(REALTIME_STATUS_NODE)
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.child("state").getValue(String::class.java).orEmpty()
                val lastChanged = snapshot.child("lastChanged").getValue(Long::class.java) ?: 0L

                trySend(
                    SocialRealtimePresenceResolver.resolvePresenceStatus(
                        userId = userId,
                        state = state,
                        lastChanged = lastChanged,
                        onlineState = PRESENCE_ONLINE
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(SocialRealtimePresenceResolver.offlineStatus(userId))
            }
        }

        statusRef.addValueEventListener(listener)
        awaitClose { statusRef.removeEventListener(listener) }
    }

    override fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (SocialRealtimePresenceResolver.shouldSkipTypingUsers(
                threadId = threadId,
                currentUserId = currentUserId
            )
        ) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val typingRef = realtimeDatabase.reference
            .child(REALTIME_TYPING_NODE)
            .child(threadId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val records = snapshot.children.map { child ->
                    SocialTypingUserResolver.TypingRecord(
                        key = child.key,
                        uid = child.child("uid").getValue(String::class.java),
                        isTyping = child.child("isTyping").getValue(Boolean::class.java) == true,
                        updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
                        userName = child.child("userName").getValue(String::class.java),
                        photoUrl = child.child("photoUrl").getValue(String::class.java)
                    )
                }

                trySend(
                    SocialTypingUserResolver.resolve(
                        records = records,
                        currentUserId = currentUserId,
                        nowMillis = nowMillis(),
                        staleMillis = TYPING_STALE_MILLIS
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        typingRef.addValueEventListener(listener)
        awaitClose { typingRef.removeEventListener(listener) }
    }

    override fun setTyping(threadId: String, isTyping: Boolean) {
        val user = auth.currentUser ?: return
        if (SocialRealtimePresenceResolver.shouldSkipTypingUpdate(
                threadId = threadId,
                userId = user.uid
            )
        ) return

        val typingRef = realtimeDatabase.reference
            .child(REALTIME_TYPING_NODE)
            .child(threadId)
            .child(user.uid)

        if (SocialRealtimePresenceResolver.shouldRemoveTyping(isTyping)) {
            typingRef.removeValue()
            return
        }

        typingRef.onDisconnect().removeValue()

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                typingRef.setValue(
                    SocialTypingPayloadResolver.createPayload(
                        uid = user.uid,
                        profileUserName = document.getString("userName"),
                        profileUsername = document.getString("username"),
                        profilePhotoUrl = document.getString("photoUrl"),
                        email = user.email,
                        authPhotoUrl = user.photoUrl?.toString(),
                        updatedAt = ServerValue.TIMESTAMP
                    )
                )
            }
            .addOnFailureListener {
                typingRef.setValue(
                    SocialTypingPayloadResolver.createPayload(
                        uid = user.uid,
                        profileUserName = null,
                        profileUsername = null,
                        profilePhotoUrl = null,
                        email = user.email,
                        authPhotoUrl = user.photoUrl?.toString(),
                        updatedAt = ServerValue.TIMESTAMP
                    )
                )
            }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val REALTIME_STATUS_NODE = "status"
        const val REALTIME_TYPING_NODE = "typing"
        const val PRESENCE_ONLINE = "online"
        const val TYPING_STALE_MILLIS = 8_000L
    }
}
