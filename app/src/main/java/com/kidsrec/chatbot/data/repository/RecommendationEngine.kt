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
        const val WEIGHT_INTEREST_MATCH = 0.22
        const val WEIGHT_AGE_MATCH = 0.22
        const val WEIGHT_READING_LEVEL = 0.12
        const val WEIGHT_FAVORITE_SIMILARITY = 0.18
        const val WEIGHT_CATEGORY_TAG_MATCH = 0.08
        const val WEIGHT_SEARCH_HISTORY = 0.10
        const val WEIGHT_CLICK_HISTORY = 0.08

        private val READING_LEVEL_MAP = mapOf(
            "Beginner" to 1,
            "Early Reader" to 2,
            "Intermediate" to 3,
            "Advanced" to 4
        )
    }

    fun scoreBook(
        book: Book,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList()
    ): Double {
        val interestScore = computeInterestScore(book, user.interests)
        val ageScore = computeAgeScore(book, user.age)
        val readingScore = computeReadingLevelScore(book, user.readingLevel)
        val favoriteScore = computeFavoriteSimilarity(book, favorites)
        val categoryTagScore = computeCategoryTagScore(book, user.interests)
        val searchHistoryScore = computeSearchHistoryScore(book, searchHistory)
        val clickHistoryScore = computeClickHistoryScore(book, clickedItems)

        val finalScore =
            (WEIGHT_INTEREST_MATCH * interestScore) +
                    (WEIGHT_AGE_MATCH * ageScore) +
                    (WEIGHT_READING_LEVEL * readingScore) +
                    (WEIGHT_FAVORITE_SIMILARITY * favoriteScore) +
                    (WEIGHT_CATEGORY_TAG_MATCH * categoryTagScore) +
                    (WEIGHT_SEARCH_HISTORY * searchHistoryScore) +
                    (WEIGHT_CLICK_HISTORY * clickHistoryScore)

        return finalScore.coerceIn(0.0, 1.0)
    }

    fun rankRecommendations(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList()
    ): List<Recommendation> {
        if (recommendations.isEmpty()) return recommendations

        return recommendations
            .map { rec ->
                val matchingBook = curatedBooks.firstOrNull {
                    it.title.equals(rec.title, ignoreCase = true)
                }

                val score = if (matchingBook != null) {
                    scoreBook(
                        book = matchingBook,
                        user = user,
                        favorites = favorites,
                        searchHistory = searchHistory,
                        clickedItems = clickedItems
                    )
                } else {
                    scoreRecommendation(
                        rec = rec,
                        user = user,
                        favorites = favorites,
                        searchHistory = searchHistory,
                        clickedItems = clickedItems
                    )
                }

                rec.copy(
                    relevanceScore = score,
                    reason = if (rec.reason.isNotBlank()) {
                        rec.reason
                    } else {
                        generateReasonFromRecommendation(
                            recommendation = rec,
                            user = user,
                            favorites = favorites,
                            searchHistory = searchHistory,
                            clickedItems = clickedItems,
                            score = score
                        )
                    }
                )
            }
            .sortedByDescending { it.relevanceScore }
    }

    fun getTopRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList(),
        limit: Int = 10
    ): List<Recommendation> {
        if (curatedBooks.isEmpty()) return emptyList()

        val favoriteIds = favorites.mapNotNull { fav ->
            fav.itemId.takeIf { it.isNotBlank() }
        }.toSet()

        val favoriteTitles = favorites.map { it.title.trim().lowercase() }.toSet()

        return curatedBooks
            .filter { book ->
                book.isVisibleToUser &&
                        book.id !in favoriteIds &&
                        book.title.trim().lowercase() !in favoriteTitles &&
                        isAllowedByUserFilters(book, user)
            }
            .map { book ->
                val score = scoreBook(
                    book = book,
                    user = user,
                    favorites = favorites,
                    searchHistory = searchHistory,
                    clickedItems = clickedItems
                )
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
                    reason = generateReason(
                        book = book,
                        user = user,
                        favorites = favorites,
                        searchHistory = searchHistory,
                        clickedItems = clickedItems,
                        score = score
                    ),
                    relevanceScore = score,
                    url = book.displayUrl
                )
            }
    }

    fun getDailyRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList(),
        limit: Int = 5
    ): List<Recommendation> {
        return getTopRecommendations(
            curatedBooks = curatedBooks,
            user = user,
            favorites = favorites,
            searchHistory = searchHistory,
            clickedItems = clickedItems,
            limit = limit
        )
    }

    fun getAllRankedRecommendations(
        curatedBooks: List<Book>,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList()
    ): List<Recommendation> {
        return getTopRecommendations(
            curatedBooks = curatedBooks,
            user = user,
            favorites = favorites,
            searchHistory = searchHistory,
            clickedItems = clickedItems,
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
            if (favWords.isEmpty()) 0.0 else jaccardSimilarity(bookWords, favWords)
        } ?: 0.0

        return maxSimilarity.coerceIn(0.0, 1.0)
    }

    private fun computeSearchHistoryScore(book: Book, searchHistory: List<String>): Double {
        if (searchHistory.isEmpty()) return 0.5

        val bookWords = tokenize(
            "${book.title} ${book.author} ${book.description} ${book.category} ${book.tags.joinToString(" ")}"
        )
        if (bookWords.isEmpty()) return 0.5

        val recentSearches = searchHistory
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeLast(10)

        if (recentSearches.isEmpty()) return 0.5

        val maxSimilarity = recentSearches.maxOfOrNull { query ->
            val queryWords = tokenize(query)
            if (queryWords.isEmpty()) 0.0 else jaccardSimilarity(bookWords, queryWords)
        } ?: 0.0

        return maxSimilarity.coerceIn(0.0, 1.0)
    }

    private fun computeClickHistoryScore(book: Book, clickedItems: List<String>): Double {
        if (clickedItems.isEmpty()) return 0.5

        val bookWords = tokenize(
            "${book.title} ${book.description} ${book.category} ${book.tags.joinToString(" ")}"
        )
        if (bookWords.isEmpty()) return 0.5

        val recentClicks = clickedItems
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeLast(10)

        if (recentClicks.isEmpty()) return 0.5

        val maxSimilarity = recentClicks.maxOfOrNull { clicked ->
            val clickedWords = tokenize(clicked)
            if (clickedWords.isEmpty()) 0.0 else jaccardSimilarity(bookWords, clickedWords)
        } ?: 0.0

        return maxSimilarity.coerceIn(0.0, 1.0)
    }

    fun scoreRecommendation(
        rec: Recommendation,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String> = emptyList(),
        clickedItems: List<String> = emptyList()
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

        val searchScore = if (searchHistory.isEmpty()) {
            0.5
        } else {
            searchHistory.takeLast(10).maxOfOrNull { query ->
                val queryWords = tokenize(query)
                if (queryWords.isEmpty() || recWords.isEmpty()) 0.0
                else jaccardSimilarity(recWords, queryWords)
            } ?: 0.0
        }

        val clickScore = if (clickedItems.isEmpty()) {
            0.5
        } else {
            clickedItems.takeLast(10).maxOfOrNull { clicked ->
                val clickedWords = tokenize(clicked)
                if (clickedWords.isEmpty() || recWords.isEmpty()) 0.0
                else jaccardSimilarity(recWords, clickedWords)
            } ?: 0.0
        }

        return (
                (WEIGHT_INTEREST_MATCH * interestScore) +
                        (WEIGHT_FAVORITE_SIMILARITY * favoriteScore) +
                        (WEIGHT_SEARCH_HISTORY * searchScore) +
                        (WEIGHT_CLICK_HISTORY * clickScore) +
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
            Regex("\\b${Regex.escape(blocked)}\\b").containsMatchIn(combinedText)
        }
    }

    private fun generateReason(
        book: Book,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String>,
        clickedItems: List<String>,
        score: Double
    ): String {
        val percentage = (score * 100).toInt()

        val favoriteMatch = findBestFavoriteMatch(book, favorites)
        if (favoriteMatch != null) {
            return "$percentage% match because you liked ${favoriteMatch.title}."
        }

        val clickedMatch = findBestHistoryMatch(
            text = "${book.title} ${book.description} ${book.category} ${book.tags.joinToString(" ")}",
            history = clickedItems
        )
        if (clickedMatch != null) {
            return "$percentage% match because you explored similar content like \"$clickedMatch\"."
        }

        val searchMatch = findBestHistoryMatch(
            text = "${book.title} ${book.description} ${book.category} ${book.tags.joinToString(" ")}",
            history = searchHistory
        )
        if (searchMatch != null) {
            return "$percentage% match because you searched for \"$searchMatch\"."
        }

        val matchingInterests = user.interests.filter { interest ->
            book.title.contains(interest, ignoreCase = true) ||
                    book.description.contains(interest, ignoreCase = true) ||
                    book.category.contains(interest, ignoreCase = true) ||
                    book.tags.any { tag -> tag.contains(interest, ignoreCase = true) }
        }

        return when {
            matchingInterests.isNotEmpty() ->
                "$percentage% match because you like ${matchingInterests.joinToString(", ")}."
            user.age in book.ageMin..book.ageMax ->
                "$percentage% match and suitable for your age group."
            else ->
                "$percentage% match based on your profile, favorites, and activity."
        }
    }

    private fun generateReasonFromRecommendation(
        recommendation: Recommendation,
        user: User,
        favorites: List<Favorite>,
        searchHistory: List<String>,
        clickedItems: List<String>,
        score: Double
    ): String {
        val percentage = (score * 100).toInt()

        val favoriteMatch = favorites.firstOrNull { fav ->
            recommendation.title.contains(fav.title, ignoreCase = true) ||
                    recommendation.description.contains(fav.title, ignoreCase = true)
        }
        if (favoriteMatch != null) {
            return "$percentage% match because you liked ${favoriteMatch.title}."
        }

        val clickedMatch = findBestHistoryMatch(
            text = "${recommendation.title} ${recommendation.description}",
            history = clickedItems
        )
        if (clickedMatch != null) {
            return "$percentage% match because you explored similar content like \"$clickedMatch\"."
        }

        val searchMatch = findBestHistoryMatch(
            text = "${recommendation.title} ${recommendation.description}",
            history = searchHistory
        )
        if (searchMatch != null) {
            return "$percentage% match because you searched for \"$searchMatch\"."
        }

        val matchingInterests = user.interests.filter { interest ->
            recommendation.title.contains(interest, ignoreCase = true) ||
                    recommendation.description.contains(interest, ignoreCase = true)
        }

        return when {
            matchingInterests.isNotEmpty() ->
                "$percentage% match because you like ${matchingInterests.joinToString(", ")}."
            else ->
                "$percentage% match based on your profile and activity."
        }
    }

    private fun findBestFavoriteMatch(book: Book, favorites: List<Favorite>): Favorite? {
        if (favorites.isEmpty()) return null

        val bookWords = tokenize(
            "${book.title} ${book.description} ${book.category} ${book.tags.joinToString(" ")}"
        )
        if (bookWords.isEmpty()) return null

        return favorites.maxByOrNull { fav ->
            val favWords = tokenize("${fav.title} ${fav.description}")
            if (favWords.isEmpty()) 0.0 else jaccardSimilarity(bookWords, favWords)
        }?.takeIf { favorite ->
            val favWords = tokenize("${favorite.title} ${favorite.description}")
            favWords.isNotEmpty() && jaccardSimilarity(bookWords, favWords) >= 0.20
        }
    }

    private fun findBestHistoryMatch(text: String, history: List<String>): String? {
        if (history.isEmpty()) return null

        val textWords = tokenize(text)
        if (textWords.isEmpty()) return null

        return history
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeLast(10)
            .maxByOrNull { entry ->
                val entryWords = tokenize(entry)
                if (entryWords.isEmpty()) 0.0 else jaccardSimilarity(textWords, entryWords)
            }
            ?.takeIf { entry ->
                val entryWords = tokenize(entry)
                entryWords.isNotEmpty() && jaccardSimilarity(textWords, entryWords) >= 0.20
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