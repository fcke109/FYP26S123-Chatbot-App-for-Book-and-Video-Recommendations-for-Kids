package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Library API Service
 * Free API with millions of books and free ebooks
 * Docs: https://openlibrary.org/developers/api
 */

// Search response
data class OpenLibrarySearchResponse(
    val numFound: Int = 0,
    val docs: List<OpenLibraryBook> = emptyList()
)

data class OpenLibraryBook(
    val key: String = "",                    // e.g., "/works/OL45883W"
    val title: String = "",
    val author_name: List<String>? = null,
    val first_publish_year: Int? = null,
    val cover_i: Int? = null,                // Cover ID for image URL
    val isbn: List<String>? = null,
    val subject: List<String>? = null,
    val language: List<String>? = null,
    val edition_count: Int? = null,
    val ebook_access: String? = null,        // "public", "borrowable", "no_ebook"
    val has_fulltext: Boolean? = null,
    val ia: List<String>? = null             // Internet Archive IDs for reading
) {
    // Get cover image URL (multiple sizes: S, M, L)
    fun getCoverUrl(size: String = "M"): String? {
        return cover_i?.let {
            "https://covers.openlibrary.org/b/id/$it-$size.jpg"
        }
    }

    // Get the work ID (without /works/ prefix)
    fun getWorkId(): String {
        return key.removePrefix("/works/")
    }

    // Get Open Library page URL
    fun getOpenLibraryUrl(): String {
        return "https://openlibrary.org$key"
    }

    // Get read online URL if available - goes directly to the book reader!
    fun getReadUrl(): String? {
        return ia?.firstOrNull()?.let {
            // Use /stream/ to go directly to the embedded book reader
            "https://archive.org/stream/$it?ui=embed"
        }
    }

    // Check if book can be read online
    fun canReadOnline(): Boolean {
        return ebook_access == "public" || ebook_access == "borrowable" || has_fulltext == true
    }

    // Get author string
    fun getAuthorString(): String {
        return author_name?.joinToString(", ") ?: "Unknown Author"
    }
}

// Work details response (for more info about a specific book)
data class OpenLibraryWork(
    val title: String = "",
    val description: Any? = null,  // Can be String or {type, value} object
    val covers: List<Int>? = null,
    val subjects: List<String>? = null,
    val subject_people: List<String>? = null,
    val subject_places: List<String>? = null
) {
    fun getDescriptionText(): String {
        return when (description) {
            is String -> description
            is Map<*, *> -> (description["value"] as? String) ?: ""
            else -> ""
        }
    }
}

interface OpenLibraryService {

    // Search for books
    // Example: /search.json?q=harry+potter&limit=10
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("language") language: String = "eng",
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,isbn,subject,ebook_access,has_fulltext,ia,edition_count"
    ): OpenLibrarySearchResponse

    // Search with subject filter (good for kids books)
    // Example: /search.json?q=dinosaurs&subject=juvenile+fiction
    @GET("search.json")
    suspend fun searchKidsBooks(
        @Query("q") query: String,
        @Query("subject") subject: String = "juvenile",
        @Query("limit") limit: Int = 10,
        @Query("language") language: String = "eng",
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,isbn,subject,ebook_access,has_fulltext,ia,edition_count"
    ): OpenLibrarySearchResponse

    // Get work details
    // Example: /works/OL45883W.json
    @GET("works/{workId}.json")
    suspend fun getWorkDetails(
        @Path("workId") workId: String
    ): OpenLibraryWork
}
