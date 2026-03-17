package com.kidsrec.chatbot.data.repository

import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class RecommendationEngine @Inject constructor() {

    companion object {
        const val WEIGHT_INTEREST_MATCH = 0.30
        const val WEIGHT_AGE_MATCH = 0.25
        const val WEIGHT_READING_LEVEL = 0.15
        const val WEIGHT_FAVORITE_SIMILARITY = 0.20
        const val WEIGHT_CATEGORY_TAG_MATCH = 0.10

        private val READING_LEVEL_MAP = mapOf(
            "Beginner" to 1,
            "Early Reader" to 2,
            "Intermediate" to 3,
            "Advanced" to 4
        )
    }

    fun scoreBook(book: Book, user: User, favorites: List<Favorite>): Double {
        val interestScore = computeInterestScore(book, user.interests)
        val ageScore = computeAgeScore(book, user.age)
        val readingScore = computeReadingLevelScore(book, user.readingLevel)
        val favoriteScore = computeFavoriteSimilarity(book, favorites)
        val categoryTagScore = computeCategoryTagScore(book, user.interests)

        val finalScore =
            (WEIGHT_INTEREST_MATCH * interestScore) +
                    (WEIGHT_AGE_MATCH * ageScore) +
                    (WEIGHT_READING_LEVEL * readingScore) +
                    (WEIGHT_FAVORITE_SIMILARITY * favoriteScore) +
                    (WEIGHT_CATEGORY_TAG_MATCH * categoryTagScore)

        return finalScore.coerceIn(0.0, 1.0)
    }

    fun rankRecommendations(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>
    ): List<Recommendation> {
        if (recommendations.isEmpty()) return recommendations

        return recommendations
            .map { rec ->
                val matchingBook = curatedBooks.firstOrNull {
                    it.title.equals(rec.title, ignoreCase = true)
                }

                val score = if (matchingBook != null) {
                    scoreBook(matchingBook, user, favorites)
                } else {
                    scoreRecommendation(rec, user, favorites)
                }

                rec.copy(relevanceScore = score)
            }
            .sortedByDescending { it.relevanceScore }
    }

    fun getTopRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        limit: Int = 10
    ): List<Recommendation> {
        if (curatedBooks.isEmpty()) return emptyList()

        val favoriteIds = favorites.mapNotNull { fav ->
            fav.itemId.takeIf { it.isNotBlank() }
        }.toSet()

        val favoriteTitles = favorites.map { it.title.trim().lowercase() }.toSet()

        return curatedBooks
            .filter { book ->
                book.isVisibleToUser && // CRITICAL: Only show books/videos allowed by canPlayInApp logic
                        book.id !in favoriteIds &&
                        book.title.trim().lowercase() !in favoriteTitles &&
                        isAllowedByUserFilters(book, user)
            }
            .map { book ->
                val score = scoreBook(book, user, favorites)
                book to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (book, score) ->
                Recommendation(
                    id = book.id,
                    type = if (book.contentType == "video") RecommendationType.VIDEO else RecommendationType.BOOK,
                    title = book.title,
                    description = book.description,
                    imageUrl = book.displayImageUrl,
                    reason = generateReason(book, user, score),
                    relevanceScore = score,
                    url = book.displayUrl
                )
            }
    }

    fun getAllRankedRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>
    ): List<Recommendation> {
        return getTopRecommendations(
            curatedBooks = curatedBooks,
            user = user,
            favorites = favorites,
            limit = curatedBooks.size.coerceAtLeast(1)
        )
    }

    private fun computeInterestScore(book: Book, interests: List<String>): Double {
        if (interests.isEmpty()) return 0.5

        val searchableText = buildString {
            append(book.title).append(" ")
            append(book.author).append(" ")
            append(book.description).append(" ")
            append(book.category).append(" ")
            append(book.tags.joinToString(" ")).append(" ")
            append(book.source).append(" ")
            append(book.language)
        }.lowercase()

        val matchCount = interests.count { interest ->
            searchableText.contains(interest.lowercase())
        }

        return (matchCount.toDouble() / interests.size).coerceIn(0.0, 1.0)
    }

    private fun computeCategoryTagScore(book: Book, interests: List<String>): Double {
        if (interests.isEmpty()) return 0.5

        val category = book.category.lowercase()
        val tags = book.tags.map { it.lowercase() }

        val matches = interests.count { interest ->
            val target = interest.lowercase()
            category.contains(target) || tags.any { it.contains(target) }
        }

        return (matches.toDouble() / interests.size).coerceIn(0.0, 1.0)
    }

    private fun computeAgeScore(book: Book, userAge: Int): Double {
        if (userAge in book.ageMin..book.ageMax) return 1.0

        val midpoint = (book.ageMin + book.ageMax) / 2.0
        val ageDiff = abs(userAge - midpoint)

        return when {
            ageDiff <= 1 -> 0.9
            ageDiff <= 2 -> 0.75
            ageDiff <= 3 -> 0.55
            ageDiff <= 5 -> 0.30
            else -> 0.10
        }
    }

    private fun computeReadingLevelScore(book: Book, readingLevel: String): Double {
        val userLevel = READING_LEVEL_MAP[readingLevel] ?: 2

        val bookLevel = when {
            book.effectiveDifficulty.contains("beginner", ignoreCase = true) -> 1
            book.effectiveDifficulty.contains("easy", ignoreCase = true) -> 1
            book.effectiveDifficulty.contains("early", ignoreCase = true) -> 2
            book.effectiveDifficulty.contains("medium", ignoreCase = true) -> 2
            book.effectiveDifficulty.contains("intermediate", ignoreCase = true) -> 3
            book.effectiveDifficulty.contains("advanced", ignoreCase = true) -> 4
            book.effectiveDifficulty.contains("hard", ignoreCase = true) -> 4
            else -> 2
        }

        val diff = abs(userLevel - bookLevel)

        return when (diff) {
            0 -> 1.0
            1 -> 0.7
            2 -> 0.35
            else -> 0.1
        }
    }

    private fun computeFavoriteSimilarity(book: Book, favorites: List<Favorite>): Double {
        if (favorites.isEmpty()) return 0.5

        val bookWords = tokenize(
            "${book.title} ${book.description} ${book.category} ${book.tags.joinToString(" ")}"
        )
        if (bookWords.isEmpty()) return 0.5

        val maxSimilarity = favorites.maxOfOrNull { fav ->
            val favWords = tokenize("${fav.title} ${fav.description}")
            if (favWords.isEmpty()) 0.0
            else jaccardSimilarity(bookWords, favWords)
        } ?: 0.0

        return maxSimilarity.coerceIn(0.0, 1.0)
    }

    fun scoreRecommendation(
        rec: Recommendation,
        user: User,
        favorites: List<Favorite>
    ): Double {
        val recWords = tokenize("${rec.title} ${rec.description}")

        val interestScore = if (user.interests.isEmpty()) {
            0.5
        } else {
            val matchCount = user.interests.count { interest ->
                rec.title.contains(interest, ignoreCase = true) ||
                        rec.description.contains(interest, ignoreCase = true)
            }
            (matchCount.toDouble() / user.interests.size).coerceIn(0.0, 1.0)
        }

        val favoriteScore = if (favorites.isEmpty()) {
            0.5
        } else {
            favorites.maxOfOrNull { fav ->
                val favWords = tokenize("${fav.title} ${fav.description}")
                if (favWords.isEmpty() || recWords.isEmpty()) 0.0
                else jaccardSimilarity(recWords, favWords)
            } ?: 0.0
        }

        return (
                (WEIGHT_INTEREST_MATCH * interestScore) +
                        (WEIGHT_FAVORITE_SIMILARITY * favoriteScore) +
                        ((WEIGHT_AGE_MATCH + WEIGHT_READING_LEVEL + WEIGHT_CATEGORY_TAG_MATCH) * 0.5)
                ).coerceIn(0.0, 1.0)
    }

    private fun isAllowedByUserFilters(book: Book, user: User): Boolean {
        if (!book.isKidSafe) return false
        if (book.contentType == "video" && !user.contentFilters.allowVideos) return false
        if (book.ageMax > user.contentFilters.maxAgeRating) return false

        val blockedTopics = user.contentFilters.blockedTopics.map { it.lowercase() }
        val combinedText = buildString {
            append(book.title).append(" ")
            append(book.description).append(" ")
            append(book.category).append(" ")
            append(book.tags.joinToString(" "))
        }.lowercase()

        return blockedTopics.none { blocked ->
            combinedText.contains(blocked)
        }
    }

    private fun generateReason(book: Book, user: User, score: Double): String {
        val matchingInterests = user.interests.filter { interest ->
            book.title.contains(interest, ignoreCase = true) ||
                    book.description.contains(interest, ignoreCase = true) ||
                    book.category.contains(interest, ignoreCase = true) ||
                    book.tags.any { tag -> tag.contains(interest, ignoreCase = true) }
        }

        val percentage = (score * 100).toInt()

        return when {
            matchingInterests.isNotEmpty() ->
                "$percentage% match because you like ${matchingInterests.joinToString(", ")}."
            user.age in book.ageMin..book.ageMax ->
                "$percentage% match and suitable for your age group."
            else ->
                "$percentage% match based on your profile and favorites."
        }
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 }
            .toSet()
    }

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        val union = a.union(b)
        if (union.isEmpty()) return 0.0
        val intersection = a.intersect(b)
        return intersection.size.toDouble() / union.size.toDouble()
    }
}