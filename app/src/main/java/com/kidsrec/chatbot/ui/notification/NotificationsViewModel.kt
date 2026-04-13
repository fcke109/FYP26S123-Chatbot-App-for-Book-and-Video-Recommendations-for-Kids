package com.kidsrec.chatbot.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.UserNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<UserNotification>>(emptyList())
    val uiState: StateFlow<List<UserNotification>> = _uiState.asStateFlow()

    private val _popupNotification = MutableStateFlow<UserNotification?>(null)
    val popupNotification: StateFlow<UserNotification?> = _popupNotification.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var hasHandledFirstSnapshotForSession = false
    private var currentUserId: String? = null

    fun startListening(userId: String) {
        if (currentUserId != userId) {
            hasHandledFirstSnapshotForSession = false
            _popupNotification.value = null
            currentUserId = userId
        }

        listenerRegistration?.remove()
        listenerRegistration = firestore.collection("userNotifications")
            .document(userId)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationsVM", "Error listening for notifications", error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.map { doc ->
                    UserNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        type = doc.getString("type") ?: "",
                        read = doc.getBoolean("read") ?: false,
                        category = doc.getString("category") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        popupOnLogin = doc.getBoolean("popupOnLogin") ?: true,
                        popupShown = doc.getBoolean("popupShown") ?: false
                    )
                } ?: emptyList()

                _uiState.value = notifications

                // Show only once per login session, on first snapshot
                if (!hasHandledFirstSnapshotForSession) {
                    hasHandledFirstSnapshotForSession = true

                    val firstUnreadPopup = notifications.firstOrNull {
                        !it.read && it.popupOnLogin && !it.popupShown
                    }

                    if (firstUnreadPopup != null) {
                        _popupNotification.value = firstUnreadPopup
                    }
                }
            }
    }

    fun dismissPopup(userId: String, notificationId: String, markAsRead: Boolean = true) {
        viewModelScope.launch {
            try {
                val updateMap = mutableMapOf<String, Any>(
                    "popupShown" to true
                )

                if (markAsRead) {
                    updateMap["read"] = true
                }

                firestore.collection("userNotifications")
                    .document(userId)
                    .collection("items")
                    .document(notificationId)
                    .update(updateMap)
                    .await()

                _popupNotification.value = null
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to dismiss popup notification", e)
            }
        }
    }

    fun markRead(userId: String, notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("userNotifications")
                    .document(userId)
                    .collection("items")
                    .document(notificationId)
                    .update(
                        mapOf(
                            "read" to true,
                            "popupShown" to true
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to mark notification as read", e)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}