package com.kidsrec.chatbot.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.math.sqrt

// Represents a recommendable item used in collaborative filtering
data class CFItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val url: String = "",

    // BOOK or VIDEO
    val type: String = "BOOK", // BOOK or VIDEO
    val category: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 18,

    // Safety filtering for children
    val isKidSafe: Boolean = true,
    val tags: List<String> = emptyList()
)

// Final recommendation result with scores
data class CFRecommendation(
    val item: CFItem,
    val userBasedScore: Double,
    val itemBasedScore: Double,
    val finalScore: Double,
    val reason: String
)

// Stores user-item interaction data
data class UserInteraction(
    val userId: String,
    val itemId: String,
    val weight: Double
)

// Hybrid collaborative filtering recommendation engine
class CollaborativeFilteringService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Main recommendation generation function
    suspend fun getHybridRecommendations(
        targetUserId: String,
        allItems: List<CFItem>,
        limit: Int = 10
    ): List<CFRecommendation> {

        val interactions = loadAllInteractions()
        if (interactions.isEmpty()) return emptyList()

        // Only recommend kid-safe items
        val safeItems = allItems
            .filter { it.isKidSafe }
            .associateBy { it.id }

        // Build user-item interaction matrix
        val userItemMatrix = buildUserItemMatrix(interactions)
        val targetVector = userItemMatrix[targetUserId] ?: emptyMap()
        if (targetVector.isEmpty()) return emptyList()

        // Prevent recommending already seen items
        val seenItemIds = targetVector.keys.toSet()

        // Calculate recommendation scores
        val userBasedScores = computeUserBasedScores(
            targetUserId = targetUserId,
            userItemMatrix = userItemMatrix,
            seenItemIds = seenItemIds
        )

        val itemBasedScores = computeItemBasedScores(
            targetUserId = targetUserId,
            userItemMatrix = userItemMatrix,
            seenItemIds = seenItemIds
        )

        // Merge recommendation candidates
        val candidateIds = (userBasedScores.keys + itemBasedScores.keys)
            .filter { it !in seenItemIds && safeItems.containsKey(it) }

        return candidateIds.mapNotNull { itemId ->
            val item = safeItems[itemId] ?: return@mapNotNull null
            val uScore = userBasedScores[itemId] ?: 0.0
            val iScore = itemBasedScores[itemId] ?: 0.0
            // Final hybrid score
            val finalScore = (0.5 * uScore) + (0.5 * iScore)

            if (finalScore <= 0.0) return@mapNotNull null

            CFRecommendation(
                item = item,
                userBasedScore = uScore,
                itemBasedScore = iScore,
                finalScore = finalScore,
                reason = buildReason(uScore, iScore, item.type)
            )
        }
            .sortedByDescending { it.finalScore }
            .take(limit)
    }

    // Loads favorites + reading history interactions from Firestore
    private suspend fun loadAllInteractions(): List<UserInteraction> = coroutineScope {

        // Load favorites and reading history in parallel
        val favoritesRootDeferred = async {
            db.collection("favorites").get().await()
        }
        val historyRootDeferred = async {
            db.collection("readingHistory").get().await()
        }

        val favoritesRoot = favoritesRootDeferred.await()
        val historyRoot = historyRootDeferred.await()

        // Load favorite items per user
        val favoritesPerUser = favoritesRoot.documents.map { userDoc ->
            async {
                val userId = userDoc.id
                val itemsSnap = db.collection("favorites")
                    .document(userId)
                    .collection("items")
                    .get()
                    .await()
                itemsSnap.documents.map { doc ->
                    UserInteraction(
                        userId = userId,
                        itemId = doc.id,
                        // Favorites have higher importance
                        weight = 3.0
                    )
                }
            }
        }

        // Load reading history per user
        val historyPerUser = historyRoot.documents.map { userDoc ->
            async {
                val userId = userDoc.id
                val itemsSnap = db.collection("readingHistory")
                    .document(userId)
                    .collection("items")
                    .get()
                    .await()
                itemsSnap.documents.map { doc ->
                    val completed = doc.getBoolean("completed") ?: false
                    val isVideo = doc.getBoolean("isVideo") ?: false
                    val weight = when {
                        completed && isVideo -> 2.5
                        completed -> 2.5
                        else -> 1.5
                    }
                    val itemId = doc.getString("itemId") ?: doc.id
                    UserInteraction(
                        userId = userId,
                        itemId = itemId,
                        weight = weight
                    )
                }
            }
        }

        // Merge all interactions together
        val all = (favoritesPerUser.awaitAll() + historyPerUser.awaitAll()).flatten()
        mergeDuplicateInteractions(all)
    }

    // Merges duplicate interactions into one weighted interaction
    private fun mergeDuplicateInteractions(
        interactions: List<UserInteraction>
    ): List<UserInteraction> {
        return interactions
            .groupBy { "${it.userId}::${it.itemId}" }
            .map { (_, grouped) ->
                UserInteraction(
                    userId = grouped.first().userId,
                    itemId = grouped.first().itemId,
                    // Sum interaction weights
                    weight = grouped.sumOf { it.weight }
                )
            }
    }

    private fun buildUserItemMatrix(
        interactions: List<UserInteraction>
    ): Map<String, Map<String, Double>> {
        return interactions
            .groupBy { it.userId }
            .mapValues { (_, userInteractions) ->
                userInteractions.associate { it.itemId to it.weight }
            }
    }

    // User-based collaborative filtering
    private fun computeUserBasedScores(
        targetUserId: String,
        userItemMatrix: Map<String, Map<String, Double>>,
        seenItemIds: Set<String>
    ): Map<String, Double> {
        val targetVector = userItemMatrix[targetUserId] ?: return emptyMap()

        val scores = mutableMapOf<String, Double>()
        val similaritySums = mutableMapOf<String, Double>()

        for ((otherUserId, otherVector) in userItemMatrix) {
            if (otherUserId == targetUserId) continue

            val similarity = cosineSimilarity(targetVector, otherVector)
            if (similarity <= 0.0) continue

            for ((itemId, rating) in otherVector) {
                if (itemId in seenItemIds) continue

                scores[itemId] = (scores[itemId] ?: 0.0) + (similarity * rating)
                similaritySums[itemId] = (similaritySums[itemId] ?: 0.0) + similarity
            }
        }

        return scores.mapValues { (itemId, score) ->
            val denom = similaritySums[itemId] ?: 1.0
            score / denom
        }
    }

    // Item-based collaborative filtering
    private fun computeItemBasedScores(
        targetUserId: String,
        userItemMatrix: Map<String, Map<String, Double>>,
        seenItemIds: Set<String>
    ): Map<String, Double> {
        val targetVector = userItemMatrix[targetUserId] ?: return emptyMap()
        val itemUserMatrix = buildItemUserMatrix(userItemMatrix)

        val scores = mutableMapOf<String, Double>()
        val similaritySums = mutableMapOf<String, Double>()

        for ((seenItemId, seenWeight) in targetVector) {
            val seenItemVector = itemUserMatrix[seenItemId] ?: continue

            for ((candidateItemId, candidateVector) in itemUserMatrix) {
                if (candidateItemId == seenItemId) continue
                if (candidateItemId in seenItemIds) continue

                val similarity = cosineSimilarity(seenItemVector, candidateVector)
                if (similarity <= 0.0) continue

                scores[candidateItemId] =
                    (scores[candidateItemId] ?: 0.0) + (similarity * seenWeight)

                similaritySums[candidateItemId] =
                    (similaritySums[candidateItemId] ?: 0.0) + similarity
            }
        }

        return scores.mapValues { (itemId, score) ->
            val denom = similaritySums[itemId] ?: 1.0
            score / denom
        }
    }

    private fun buildItemUserMatrix(
        userItemMatrix: Map<String, Map<String, Double>>
    ): Map<String, Map<String, Double>> {
        val itemUserMap = mutableMapOf<String, MutableMap<String, Double>>()

        for ((userId, itemMap) in userItemMatrix) {
            for ((itemId, weight) in itemMap) {
                val userWeights = itemUserMap.getOrPut(itemId) { mutableMapOf() }
                userWeights[userId] = weight
            }
        }

        return itemUserMap
    }

    // Calculates cosine similarity between two vectors
    private fun cosineSimilarity(
        a: Map<String, Double>,
        b: Map<String, Double>
    ): Double {
        val commonKeys = a.keys.intersect(b.keys)
        if (commonKeys.isEmpty()) return 0.0

        val dot = commonKeys.sumOf { key -> (a[key] ?: 0.0) * (b[key] ?: 0.0) }
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })

        if (normA == 0.0 || normB == 0.0) return 0.0
        return dot / (normA * normB)
    }

    // Generates explanation text for recommendations
    private fun buildReason(
        userBasedScore: Double,
        itemBasedScore: Double,
        type: String
    ): String {
        return when {
            userBasedScore > itemBasedScore ->
                "Recommended because similar users liked this $type."
            itemBasedScore > userBasedScore ->
                "Recommended because it is similar to items the child already liked."
            else ->
                "Recommended using both similar users and similar items."
        }
    }
}