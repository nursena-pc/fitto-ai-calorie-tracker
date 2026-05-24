package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

object RealtimePresenceManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var connectedRef: DatabaseReference? = null
    private var statusRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null
    private var trackedUserId: String? = null
    private var trackedPublicUserName: String? = null
    private var trackedPublicPhotoUrl: String? = null
    private var isStarted = false

    fun start() {
        isStarted = true

        if (authListener == null) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (isStarted && user != null) {
                    startForUser(user)
                } else {
                    detachConnectionListener(writeOffline = true)
                }
            }
            authListener = listener
            auth.addAuthStateListener(listener)
        }

        auth.currentUser?.let { user -> startForUser(user) }
    }

    fun stop(removeStatus: Boolean = true) {
        isStarted = false
        detachConnectionListener(writeOffline = removeStatus)

        authListener?.let { listener ->
            auth.removeAuthStateListener(listener)
        }
        authListener = null
    }

    private fun startForUser(user: FirebaseUser) {
        if (trackedUserId == user.uid && connectedListener != null) return

        detachConnectionListener(writeOffline = true)

        resolvePublicProfile(user) { publicUserName, publicPhotoUrl ->
            if (!isStarted || auth.currentUser?.uid != user.uid) return@resolvePublicProfile
            attachConnectionListener(
                user = user,
                publicUserName = publicUserName,
                publicPhotoUrl = publicPhotoUrl
            )
        }
    }

    private fun attachConnectionListener(
        user: FirebaseUser,
        publicUserName: String,
        publicPhotoUrl: String?
    ) {
        val currentStatusRef = database.reference
            .child(STATUS_NODE)
            .child(user.uid)
        val infoConnectedRef = database.reference.child(INFO_CONNECTED_NODE)
        val photoUrl = publicPhotoUrl ?: user.photoUrl?.toString()

        val offlineValue = mapOf(
            "uid" to user.uid,
            "state" to STATE_OFFLINE,
            "userName" to publicUserName,
            "photoUrl" to photoUrl,
            "lastChanged" to System.currentTimeMillis()
        )

        val onlineValue = mapOf(
            "uid" to user.uid,
            "state" to STATE_ONLINE,
            "userName" to publicUserName,
            "photoUrl" to photoUrl,
            "lastChanged" to System.currentTimeMillis()
        )

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (!connected) return

                currentStatusRef.onDisconnect().setValue(offlineValue)
                currentStatusRef.setValue(onlineValue)
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        trackedUserId = user.uid
        trackedPublicUserName = publicUserName
        trackedPublicPhotoUrl = photoUrl
        connectedRef = infoConnectedRef
        statusRef = currentStatusRef
        connectedListener = listener
        infoConnectedRef.addValueEventListener(listener)
    }

    private fun resolvePublicProfile(
        user: FirebaseUser,
        onResolved: (String, String?) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val publicUserName = document.getString("userName")
                    ?: document.getString("username")
                    ?: user.email?.substringBefore("@")
                    ?: "fitto_user"
                val publicPhotoUrl = document.getString("photoUrl")
                    ?: user.photoUrl?.toString()

                onResolved(publicUserName, publicPhotoUrl)
            }
            .addOnFailureListener {
                onResolved(
                    user.email?.substringBefore("@") ?: "fitto_user",
                    user.photoUrl?.toString()
                )
            }
    }

    private fun detachConnectionListener(writeOffline: Boolean) {
        val ref = connectedRef
        val listener = connectedListener
        if (ref != null && listener != null) {
            ref.removeEventListener(listener)
        }

        if (writeOffline) {
            val user = auth.currentUser
            val currentStatusRef = statusRef
            if (user != null && currentStatusRef != null) {
                val publicUserName = trackedPublicUserName
                    ?: user.email?.substringBefore("@")
                    ?: "fitto_user"

                currentStatusRef.setValue(
                    mapOf(
                        "uid" to user.uid,
                        "state" to STATE_OFFLINE,
                        "userName" to publicUserName,
                        "photoUrl" to (trackedPublicPhotoUrl ?: user.photoUrl?.toString()),
                        "lastChanged" to System.currentTimeMillis()
                    )
                )
            }
        }

        connectedRef = null
        statusRef = null
        connectedListener = null
        trackedUserId = null
        trackedPublicUserName = null
        trackedPublicPhotoUrl = null
    }

    private const val USERS_COLLECTION = "users"
    private const val STATUS_NODE = "status"
    private const val INFO_CONNECTED_NODE = ".info/connected"
    private const val STATE_ONLINE = "online"
    private const val STATE_OFFLINE = "offline"
}
