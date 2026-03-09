package com.kidsrec.chatbot.data.repository

import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.model.User
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RecommendationEngine: A simplified ANN-inspired scoring engine that ranks
 * content based on user profile features and learned preferences from favorites.
 *
 * Architecture (simplified neural network concept):
 * - Input layer: user features (age, interests, reading level, favorite history)
 * - Weights: static weights that determine feature importance
 * - Scoring function: weighted sum of feature match scores
 * - Output: ranked list of recommendations
 */
@Singleton
class RecommendationEngine @Inject constructor() {

    // --- Weights (analogous to trained ANN weights) ---
    companion object {
        const val WEIGHT_INTEREST_MATCH = 0.35
        const val WEIGHT_AGE_MATCH = 0.25
        const val WEIGHT_READING_LEVEL = 0.15
        const val WEIGHT_FAVORITE_SIMILARITY = 0.25

        private val READING_LEVEL_MAP = mapOf(
            "Beginner" to 1,
            "Early Reader" to 2,
            "Intermediate" to 3,
            "Advanced" to 4
        )

        private val AGE_RANGE_MAP = mapOf(
            "3-5 years" to 4,
            "4-6 years" to 5,
            "5-7 years" to 6,
            "6-8 years" to 7,
            "7-9 years" to 8,
            "8-10 years" to 9,
            "9-12 years" to 10,
            "10-12 years" to 11,
            "12+ years" to 13
        )
    }

    /**
     * Score a single book against a user profile and their favorites.
     * Returns a value between 0.0 and 1.0.
     */
    fun scoreBook(book: Book, user: User, favorites: List<Favorite>): Double {
        val interestScore = computeInterestScore(book, user.interests)
        val ageScore = computeAgeScore(book, user.age)
        val readingScore = computeReadingLevelScore(book, user.readingLevel)
        val favoriteScore = computeFavoriteSimilarity(book, favorites)

        return (WEIGHT_INTEREST_MATCH * interestScore +
                WEIGHT_AGE_MATCH * ageScore +
                WEIGHT_READING_LEVEL * readingScore +
                WEIGHT_FAVORITE_SIMILARITY * favoriteScore)
            .coerceIn(0.0, 1.0)
    }

    /**
     * Rank a list of AI-generated recommendations using user profile data.
     * Recommendations with higher relevance scores appear first.
     */
    fun rankRecommendations(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>
    ): List<Recommendation> {
        if (recommendations.isEmpty()) return recommendations

        return recommendations.map { rec ->
            val matchingBook = curatedBooks.firstOrNull {
                it.title.equals(rec.title, ignoreCase = true)
            }
            val score = if (matchingBook != null) {
                scoreBook(matchingBook, user, favorites)
            } else {
                scoreRecommendation(rec, user, favorites)
            }
            rec.copy(relevanceScore = score)
        }.sortedByDescending { it.relevanceScore }
    }

    /**
     * Generate top recommendations directly from the curated books collection.
     * Used as a fallback when the AI service is unavailable.
     */
    fun getTopRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        limit: Int = 3
    ): List<Recommendation> {
        if (curatedBooks.isEmpty()) return emptyList()

        val favoriteTitles = favorites.map { it.title.lowercase() }.toSet()

        return curatedBooks
            .filter { it.title.lowercase() !in favoriteTitles }
            .map { book -> book to scoreBook(book, user, favorites) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (book, score) ->
                Recommendation(
                    id = UUID.randomUUID().toString(),
                    type = RecommendationType.BOOK,
                    title = book.title,
                    description = book.description ?: "A great book for you!",
                    imageUrl = book.coverUrl ?: "",
                    reason = generateReason(book, user),
                    relevanceScore = score
                )
            }
    }

    // --- Scoring functions (analogous to activation functions) ---

    private fun computeInterestScore(book: Book, interests: List<String>): Double {
        if (interests.isEmpty()) return 0.5
        val bookText = "${book.title} ${book.description ?: ""} ${book.author}".lowercase()
        val matchCount = interests.count { interest ->
            bookText.contains(interest.lowercase())
        }
        return (matchCount.toDouble() / interests.size).coerceIn(0.0, 1.0)
    }

    private fun computeAgeScore(book: Book, userAge: Int): Double {
        val bookAge = AGE_RANGE_MAP.entries
            .firstOrNull { book.ageRating.contains(it.key, ignoreCase = true) }
            ?.value ?: 7
        val ageDiff = kotlin.math.abs(userAge - bookAge)
        return when {
            ageDiff <= 1 -> 1.0
            ageDiff <= 2 -> 0.8
            ageDiff <= 3 -> 0.5
            ageDiff <= 5 -> 0.3
            else -> 0.1
        }
    }

    private fun computeReadingLevelScore(book: Book, readingLevel: String): Double {
        val userLevel = READING_LEVEL_MAP[readingLevel] ?: 2
        val bookLevel = when {
            book.readingAvailability.contains("Beginner", ignoreCase = true) -> 1
            book.readingAvailability.contains("Early", ignoreCase = true) -> 2
            book.readingAvailability.contains("Intermediate", ignoreCase = true) -> 3
            book.readingAvailability.contains("Advanced", ignoreCase = true) -> 4
            else -> 2
        }
        val diff = kotlin.math.abs(userLevel - bookLevel)
        return when (diff) {
            0 -> 1.0
            1 -> 0.7
            2 -> 0.3
            else -> 0.1
        }
    }

    /**
     * Learning element: computes similarity between a book and the user's
     * favorite history. Books similar to past favorites score higher.
     */
    private fun computeFavoriteSimilarity(book: Book, favorites: List<Favorite>): Double {
        if (favorites.isEmpty()) return 0.5

        val bookWords = "${book.title} ${book.description ?: ""}".lowercase()
            .split(Regex("\\W+")).filter { it.length > 2 }.toSet()

        if (bookWords.isEmpty()) return 0.5

        val maxSimilarity = favorites.maxOfOrNull { fav ->
            val favWords = "${fav.title} ${fav.description}".lowercase()
                .split(Regex("\\W+")).filter { it.length > 2 }.toSet()
            if (favWords.isEmpty()) return@maxOfOrNull 0.0
            val intersection = bookWords.intersect(favWords).size
            val union = bookWords.union(favWords).size
            if (union == 0) 0.0 else intersection.toDouble() / union
        } ?: 0.0

        return maxSimilarity.coerceIn(0.0, 1.0)
    }

    /**
     * Score a recommendation that doesn't have a matching curated book.
     * Uses text-based matching against user profile.
     */
    private fun scoreRecommendation(
        rec: Recommendation,
        user: User,
        favorites: List<Favorite>
    ): Double {
        val recText = "${rec.title} ${rec.description}".lowercase()
        val interestScore = if (user.interests.isEmpty()) 0.5 else {
            val matchCount = user.interests.count { recText.contains(it.lowercase()) }
            (matchCount.toDouble() / user.interests.size).coerceIn(0.0, 1.0)
        }

        val favScore = if (favorites.isEmpty()) 0.5 else {
            val recWords = recText.split(Regex("\\W+")).filter { it.length > 2 }.toSet()
            favorites.maxOfOrNull { fav ->
                val favWords = "${fav.title} ${fav.description}".lowercase()
                    .split(Regex("\\W+")).filter { it.length > 2 }.toSet()
                if (favWords.isEmpty() || recWords.isEmpty()) 0.0
                else recWords.intersect(favWords).size.toDouble() / recWords.union(favWords).size
            } ?: 0.0
        }

        return (WEIGHT_INTEREST_MATCH * interestScore +
                WEIGHT_FAVORITE_SIMILARITY * favScore +
                (WEIGHT_AGE_MATCH + WEIGHT_READING_LEVEL) * 0.5)
            .coerceIn(0.0, 1.0)
    }

    private fun generateReason(book: Book, user: User): String {
        val matchingInterests = user.interests.filter { interest ->
            "${book.title} ${book.description ?: ""}".contains(interest, ignoreCase = true)
        }
        return if (matchingInterests.isNotEmpty()) {
            "Picked for you because you love ${matchingInterests.joinToString(" and ")}!"
        } else {
            "A great pick for ${user.age}-year-olds!"
        }
    }
}
