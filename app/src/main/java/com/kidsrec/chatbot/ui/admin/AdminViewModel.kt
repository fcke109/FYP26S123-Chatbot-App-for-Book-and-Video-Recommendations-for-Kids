package com.kidsrec.chatbot.ui.admin

import android.annotation.SuppressLint
import com.kidsrec.chatbot.data.model.BookCategory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.Date
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.LoginAttempt
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.SuspiciousActivity
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.model.UserStatus
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.AnalyticsRepository
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val ADMIN_NOTIFICATION_EMAIL = "admin@littledino.com"

private fun isEligibleNotificationRecipient(
    doc: com.google.firebase.firestore.DocumentSnapshot
): Boolean {
    val email = doc.getString("email")
    if (email.equals(ADMIN_NOTIFICATION_EMAIL, ignoreCase = true)) return false

    val status = doc.getString("status")?.trim()?.uppercase()
    return status.isNullOrBlank() || status == "ACTIVE"
}

private fun extractNormalizedInterests(
    doc: com.google.firebase.firestore.DocumentSnapshot
): Set<String> {
    return (doc.get("interests") as? List<*>)
        ?.mapNotNull { (it as? String)?.trim()?.lowercase() }
        ?.filter { it.isNotBlank() }
        ?.toSet()
        ?: emptySet()
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val openLibraryService: OpenLibraryService,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val adminUpgradeRepository: AdminUpgradeRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _categories = MutableStateFlow<List<BookCategory>>(emptyList())
    val categories: StateFlow<List<BookCategory>> = _categories.asStateFlow()

    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    private val _isLoadingAdminStats = MutableStateFlow(false)
    val isLoadingAdminStats: StateFlow<Boolean> = _isLoadingAdminStats.asStateFlow()

    private val _userReadingHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val userReadingHistory: StateFlow<List<ReadingHistory>> = _userReadingHistory.asStateFlow()

    private val _userChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val userChatHistory: StateFlow<List<ChatMessage>> = _userChatHistory.asStateFlow()

    private val _isLoadingUserActivity = MutableStateFlow(false)
    val isLoadingUserActivity: StateFlow<Boolean> = _isLoadingUserActivity.asStateFlow()

    private val _loginAttempts = MutableStateFlow<List<LoginAttempt>>(emptyList())
    val loginAttempts: StateFlow<List<LoginAttempt>> = _loginAttempts.asStateFlow()

    private val _suspiciousActivities = MutableStateFlow<List<SuspiciousActivity>>(emptyList())
    val suspiciousActivities: StateFlow<List<SuspiciousActivity>> = _suspiciousActivities.asStateFlow()

    private val _isLoadingSecurityData = MutableStateFlow(false)
    val isLoadingSecurityData: StateFlow<Boolean> = _isLoadingSecurityData.asStateFlow()

    private val _deleteResult = MutableStateFlow<String?>(null)
    val deleteResult: StateFlow<String?> = _deleteResult.asStateFlow()

    private val _topSearchedTopics = MutableStateFlow<List<com.kidsrec.chatbot.data.model.TopSearchedTopic>>(emptyList())
    val topSearchedTopics: StateFlow<List<com.kidsrec.chatbot.data.model.TopSearchedTopic>> = _topSearchedTopics.asStateFlow()

    private val _topViewedBooks = MutableStateFlow<List<com.kidsrec.chatbot.data.model.TopViewedBook>>(emptyList())
    val topViewedBooks: StateFlow<List<com.kidsrec.chatbot.data.model.TopViewedBook>> = _topViewedBooks.asStateFlow()

    private val _topDropOffs = MutableStateFlow<List<com.kidsrec.chatbot.data.model.TopDropOff>>(emptyList())
    val topDropOffs: StateFlow<List<com.kidsrec.chatbot.data.model.TopDropOff>> = _topDropOffs.asStateFlow()

    private var usersJob: Job? = null
    private var booksJob: Job? = null
    private var topSearchJob: Job? = null
    private var topViewedJob: Job? = null
    private var topDropOffJob: Job? = null

    init {
        refreshAllAdminData()
    }

    fun refreshAllAdminData() {
        startManagingUsers()
        loadCuratedBooks()
        loadCategories()
        refreshAdminStats()
        loadAnalytics()
    }

    fun getCurrentUserId(): String? = accountManager.getCurrentUserId()

    fun startManagingUsers() {
        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            accountManager.getAllUsersFlow()
                .catch { e -> Log.e("AdminVM", "Permission denied for users list", e) }
                .collect { userList -> _users.value = userList }
        }
    }

    private fun loadCuratedBooks() {
        booksJob?.cancel()
        booksJob = viewModelScope.launch {
            bookDataManager.getCuratedBooksFlow()
                .catch { e -> Log.e("AdminVM", "Load failed", e) }
                .collect { books -> _curatedBooks.value = books }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("categories").get().await()
                val list = snapshot.documents.mapNotNull {
                    it.toObject(BookCategory::class.java)?.copy(id = it.id)
                }.sortedBy { it.name.lowercase() }

                _categories.value = list
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to load categories", e)
                _categories.value = emptyList()
            }
        }
    }

    fun addCategory(name: String, description: String) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                val trimmedDescription = description.trim()

                if (trimmedName.isBlank()) {
                    Log.w("AdminVM", "Category name cannot be blank")
                    return@launch
                }

                val id = trimmedName
                    .lowercase()
                    .replace(Regex("\\s+"), "-")
                    .replace(Regex("[^a-z0-9-]"), "")

                val category = BookCategory(
                    id = id,
                    name = trimmedName,
                    description = trimmedDescription
                )

                firestore.collection("categories")
                    .document(id)
                    .set(category)
                    .await()

                loadCategories()
                Log.d("AdminVM", "Category added: $id")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to add category", e)
            }
        }
    }

    fun updateCategory(id: String, name: String, description: String) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                val trimmedDescription = description.trim()

                if (trimmedName.isBlank()) {
                    Log.w("AdminVM", "Category name cannot be blank")
                    return@launch
                }

                firestore.collection("categories")
                    .document(id)
                    .update(
                        mapOf(
                            "name" to trimmedName,
                            "description" to trimmedDescription
                        )
                    )
                    .await()

                loadCategories()
                Log.d("AdminVM", "Category updated: $id")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to update category", e)
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            try {
                firestore.collection("categories")
                    .document(id)
                    .delete()
                    .await()

                loadCategories()
                Log.d("AdminVM", "Category deleted: $id")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to delete category", e)
            }
        }
    }

    fun refreshAdminStats() {
        viewModelScope.launch {
            _isLoadingAdminStats.value = true
            try {
                val nowMillis = System.currentTimeMillis()
                val twentyFourHoursAgo = Timestamp(Date(nowMillis - 24 * 60 * 60 * 1000))
                val thirtyDaysAgo = Timestamp(Date(nowMillis - 30L * 24 * 60 * 60 * 1000))

                val totalUsers = firestore.collection("users")
                    .get()
                    .await()
                    .size()
                    .toLong()

                val dailyActiveUsers = try {
                    firestore.collection("users")
                        .whereGreaterThan("lastLoggedInAt", twentyFourHoursAgo)
                        .get()
                        .await()
                        .size()
                        .toLong()
                } catch (e: Exception) {
                    Log.w("AdminVM", "Failed to query users.lastLoggedInAt for DAU, falling back to loginAttempts", e)
                    firestore.collection("loginAttempts")
                        .whereEqualTo("success", true)
                        .whereGreaterThan("timestamp", twentyFourHoursAgo)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.getString("userId") }
                        .distinct()
                        .size
                        .toLong()
                }

                val monthlyActiveUsers = try {
                    firestore.collection("users")
                        .whereGreaterThan("lastLoggedInAt", thirtyDaysAgo)
                        .get()
                        .await()
                        .size()
                        .toLong()
                } catch (e: Exception) {
                    Log.w("AdminVM", "Failed to query users.lastLoggedInAt for MAU, falling back to loginAttempts", e)
                    firestore.collection("loginAttempts")
                        .whereEqualTo("success", true)
                        .whereGreaterThan("timestamp", thirtyDaysAgo)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.getString("userId") }
                        .distinct()
                        .size
                        .toLong()
                }

                // Count all stored chatbot messages so existing Firestore history appears in the dashboard.
                val chatbotUsageCount = try {
                    firestore.collectionGroup("messages")
                        .get()
                        .await()
                        .documents
                        .size
                        .toLong()
                } catch (e: Exception) {
                    Log.w("AdminVM", "Failed to query collectionGroup(messages) for chatbot usage, falling back to nested traversal", e)
                    try {
                        firestore.collection("chatHistory")
                            .get()
                            .await()
                            .documents
                            .sumOf { userDoc ->
                                try {
                                    userDoc.reference
                                        .collection("conversations")
                                        .get()
                                        .await()
                                        .documents
                                        .sumOf { convoDoc ->
                                            try {
                                                convoDoc.reference
                                                    .collection("messages")
                                                    .get()
                                                    .await()
                                                    .size()
                                                    .toLong()
                                            } catch (inner: Exception) {
                                                0L
                                            }
                                        }
                                } catch (inner: Exception) {
                                    0L
                                }
                            }
                    } catch (inner: Exception) {
                        Log.w("AdminVM", "Fallback nested traversal also failed for chatbot usage", inner)
                        0L
                    }
                }

                _adminStats.value = AdminStats(
                    totalUsers = totalUsers,
                    dailyActiveUsers = dailyActiveUsers,
                    monthlyActiveUsers = monthlyActiveUsers,
                    chatbotUsageCount = chatbotUsageCount
                )
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to load admin stats", e)
                _adminStats.value = AdminStats()
            } finally {
                _isLoadingAdminStats.value = false
            }
        }
    }

    private fun loadAnalytics() {
        topSearchJob?.cancel()
        topViewedJob?.cancel()
        topDropOffJob?.cancel()

        topSearchJob = viewModelScope.launch {
            analyticsRepository.getTopSearchedTopicsFlow()
                .catch { e -> Log.e("AdminVM", "Failed to load top searched topics", e) }
                .collect { topics -> _topSearchedTopics.value = topics }
        }
        topViewedJob = viewModelScope.launch {
            analyticsRepository.getTopViewedBooksFlow()
                .catch { e -> Log.e("AdminVM", "Failed to load top viewed books", e) }
                .collect { books -> _topViewedBooks.value = books }
        }
        topDropOffJob = viewModelScope.launch {
            analyticsRepository.getTopDropOffsFlow()
                .catch { e -> Log.e("AdminVM", "Failed to load top drop-off points", e) }
                .collect { dropOffs -> _topDropOffs.value = dropOffs }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        val lowerQuery = query.lowercase().trim()
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Track search analytics
                val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
                analyticsRepository.trackSearch(lowerQuery, currentUserId)

                val response = openLibraryService.searchBooks("$query subject:\"Children's fiction\" language:eng")
                val results: List<Book> = response.docs.mapNotNull { doc ->
                    val iaId = doc.ia?.firstOrNull() ?: return@mapNotNull null
                    if (doc.cover_i == null) return@mapNotNull null

                    val title = doc.title
                    val author = doc.author_name?.firstOrNull() ?: "Unknown"

                    // Basic Scoring
                    var score = 0
                    if (title.lowercase().contains(lowerQuery)) score += 60
                    if (author.lowercase().contains(lowerQuery)) score += 20
                    if (title.lowercase() == lowerQuery) score += 40

                    // Cap score at 100
                    val finalScore = score.coerceAtMost(100)

                    if (finalScore > 0) {
                        Book(
                            id = iaId,
                            title = title,
                            author = author,
                            coverUrl = "https://covers.openlibrary.org/b/id/${doc.cover_i}-L.jpg",
                            bookUrl = "https://archive.org/embed/$iaId",
                            readerUrl = "https://archive.org/embed/$iaId",
                            source = "ICDL/Archive",
                            ageMin = 3,
                            ageMax = 12,
                            isKidSafe = true
                        ).apply { searchScore = finalScore }
                    } else null
                }.sortedByDescending { it.searchScore }

                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("AdminVM", "Search failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun trackBookView(bookTitle: String, bookId: String = "") {
        viewModelScope.launch {
            val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
            analyticsRepository.trackBookView(bookId, bookTitle, currentUserId)
        }
    }

    fun addBookToLibrary(book: Book) {
        viewModelScope.launch {
            // The DataManager will now automatically pick the next sequential ID
            bookDataManager.addBook(book.copy(id = ""))
        }
    }

    /**
     * Re-assigns numeric IDs to all books in the library to clean them up.
     */
    @SuppressLint("DefaultLocale")
    fun cleanLibraryIds() {
        viewModelScope.launch {
            val currentBooks = _curatedBooks.value.toList()
            currentBooks.forEachIndexed { index, book ->
                val newId = String.format("%03d", index + 1)
                if (book.id != newId) {
                    // Delete the old record
                    bookDataManager.deleteBook(book.id)
                    // Save it with the new numeric ID
                    bookDataManager.addBook(book.copy(id = newId))
                }
            }
        }
    }

    fun deleteBookFromLibrary(bookId: String) {
        viewModelScope.launch { bookDataManager.deleteBook(bookId) }
    }

    fun removeUnsafeContent(bookId: String, reason: String) {
        viewModelScope.launch {
            try {
                adminUpgradeRepository.removeUnsafeContent(bookId, reason)
                Log.d("AdminVM", "Content $bookId removed as unsafe: $reason")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to remove unsafe content $bookId", e)
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                // Delete user's Firestore document
                firestore.collection("users").document(userId).delete().await()

                // Delete user's favorites
                val favItems = firestore.collection("favorites")
                    .document(userId).collection("items").get().await()
                for (doc in favItems.documents) {
                    doc.reference.delete().await()
                }

                // Delete user's reading history
                val sessions = firestore.collection("readingHistory")
                    .document(userId).collection("sessions").get().await()
                for (doc in sessions.documents) {
                    doc.reference.delete().await()
                }

                // Delete user's chat history conversations and messages
                val convos = firestore.collection("chatHistory")
                    .document(userId).collection("conversations").get().await()
                for (convo in convos.documents) {
                    val messages = convo.reference.collection("messages").get().await()
                    for (msg in messages.documents) {
                        msg.reference.delete().await()
                    }
                    convo.reference.delete().await()
                }

                // Also delete the Firebase Auth account via Cloud Function
                try {
                    functions.getHttpsCallable("deleteAuthUser")
                        .call(hashMapOf("uid" to userId))
                        .await()
                    Log.d("AdminVM", "Auth account $userId also deleted")
                } catch (authErr: Exception) {
                    Log.w("AdminVM", "Firestore deleted but Auth cleanup failed (deploy functions first): ${authErr.message}")
                }

                Log.d("AdminVM", "User $userId deleted from Firestore")
                _deleteResult.value = "success"
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to delete user $userId", e)
                _deleteResult.value = "error: ${e.message}"
            }
        }
    }

    fun suspendUser(userId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    val updatedUser = user.copy(status = UserStatus.SUSPENDED)
                    accountManager.updateUser(updatedUser)
                    Log.d("AdminVM", "User $userId suspended")
                }
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to suspend user $userId", e)
            }
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    val updatedUser = user.copy(status = UserStatus.BANNED)
                    accountManager.updateUser(updatedUser)
                    Log.d("AdminVM", "User $userId banned")
                }
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to ban user $userId", e)
            }
        }
    }

    fun activateUser(userId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    val updatedUser = user.copy(status = UserStatus.ACTIVE)
                    accountManager.updateUser(updatedUser)
                    Log.d("AdminVM", "User $userId activated")
                }
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to activate user $userId", e)
            }
        }
    }

    fun loadUserActivity(userId: String) {
        viewModelScope.launch {
            _isLoadingUserActivity.value = true
            try {
                // Load reading history
                val readingHistory = firestore.collection("readingHistory")
                    .document(userId)
                    .collection("sessions")
                    .orderBy("openedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()
                    .toObjects(ReadingHistory::class.java)

                _userReadingHistory.value = readingHistory

                // Load chat history (recent messages from all conversations)
                val chatMessages = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .get()
                    .await()
                    .documents
                    .flatMap { convoDoc ->
                        convoDoc.reference.collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(10)
                            .get()
                            .await()
                            .toObjects(ChatMessage::class.java)
                    }
                    .sortedByDescending { it.timestamp }

                _userChatHistory.value = chatMessages.take(20) // Take most recent 20 messages

            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to load user activity for $userId", e)
                _userReadingHistory.value = emptyList()
                _userChatHistory.value = emptyList()
            } finally {
                _isLoadingUserActivity.value = false
            }
        }
    }

    fun loadSecurityData() {
        viewModelScope.launch {
            _isLoadingSecurityData.value = true
            try {
                // Load recent login attempts (last 7 days)
                val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
                val loginAttempts = firestore.collection("loginAttempts")
                    .whereGreaterThan("timestamp", sevenDaysAgo)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()
                    .toObjects(LoginAttempt::class.java)

                _loginAttempts.value = loginAttempts

                // Load suspicious activities (last 30 days)
                val thirtyDaysAgo = Timestamp(Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000))
                val suspiciousActivities = firestore.collection("suspiciousActivities")
                    .whereGreaterThan("timestamp", thirtyDaysAgo)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                    .toObjects(SuspiciousActivity::class.java)

                _suspiciousActivities.value = suspiciousActivities

            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to load security data", e)
                _loginAttempts.value = emptyList()
                _suspiciousActivities.value = emptyList()
            } finally {
                _isLoadingSecurityData.value = false
            }
        }
    }

    fun markSuspiciousActivityResolved(activityId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("suspiciousActivities")
                    .document(activityId)
                    .update("resolved", true)
                    .await()

                // Update local state
                _suspiciousActivities.value = _suspiciousActivities.value.map {
                    if (it.id == activityId) it.copy(resolved = true) else it
                }

                Log.d("AdminVM", "Marked suspicious activity $activityId as resolved")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to mark activity as resolved", e)
            }
        }
    }

    fun sendNotification(
        title: String,
        body: String,
        type: NotificationType,
        targetValue: String = ""
    ) {
        viewModelScope.launch {
            try {
                when (type) {
                    NotificationType.ANNOUNCEMENT -> {
                        // Send announcement to all users
                        sendAnnouncementToAllUsers(title, body)
                    }
                    NotificationType.PERSONALIZED -> {
                        // Send personalized alert based on interest category
                        sendPersonalizedAlert(title, body, targetValue)
                    }
                }
                Log.d("AdminVM", "Notification sent successfully: $title")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to send notification", e)
                throw e
            }
        }
    }

    private suspend fun sendAnnouncementToAllUsers(title: String, body: String) {
        val recipientIds = firestore.collection("users")
            .get()
            .await()
            .documents
            .filter { doc -> isEligibleNotificationRecipient(doc) }
            .map { it.id }

        if (recipientIds.isEmpty()) {
            Log.w("AdminVM", "Announcement not sent because no eligible recipients were found")
            return
        }

        val batch = firestore.batch()
        val notificationData = mapOf(
            "title" to title,
            "body" to body,
            "type" to "announcement",
            "read" to false,
            "createdAt" to System.currentTimeMillis()
        )

        recipientIds.forEach { userId ->
            val notificationRef = firestore.collection("userNotifications")
                .document(userId)
                .collection("items")
                .document()
            batch.set(notificationRef, notificationData)
        }

        batch.commit().await()
        Log.d("AdminVM", "Announcement sent to ${recipientIds.size} users")
    }

    private suspend fun sendPersonalizedAlert(title: String, body: String, interestCategory: String) {
        val normalizedCategory = interestCategory.trim().lowercase()

        val interestedUsers = firestore.collection("users")
            .get()
            .await()
            .documents
            .filter { doc ->
                isEligibleNotificationRecipient(doc) &&
                        extractNormalizedInterests(doc).contains(normalizedCategory)
            }
            .map { it.id }

        if (interestedUsers.isEmpty()) {
            Log.w("AdminVM", "No eligible users found with interest: $interestCategory")
            return
        }

        val batch = firestore.batch()
        val notificationData = mapOf(
            "title" to title,
            "body" to body,
            "type" to "personalized",
            "category" to interestCategory,
            "read" to false,
            "createdAt" to System.currentTimeMillis()
        )

        interestedUsers.forEach { userId ->
            val notificationRef = firestore.collection("userNotifications")
                .document(userId)
                .collection("items")
                .document()
            batch.set(notificationRef, notificationData)
        }

        batch.commit().await()
        Log.d("AdminVM", "Personalized alert sent to ${interestedUsers.size} users interested in $interestCategory")
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            accountManager.signOut()
            onSuccess()
        }
    }
}
