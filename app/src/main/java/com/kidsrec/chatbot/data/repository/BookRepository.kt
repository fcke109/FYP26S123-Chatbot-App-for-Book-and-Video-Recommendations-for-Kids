package com.kidsrec.chatbot.data.repository

import com.kidsrec.chatbot.data.remote.GoogleBooksService
import com.kidsrec.chatbot.data.remote.OpenLibraryBook
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import javax.inject.Inject
import javax.inject.Singleton

data class BookPreviewResult(
    val previewUrl: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?
)

@Singleton
class BookRepository @Inject constructor(
    private val openLibraryService: OpenLibraryService,
    private val googleBooksService: GoogleBooksService
) {

    /**
     * Get a direct link to read a book online
     * Uses Open Library which has free readable books
     */
    suspend fun getBookPreviewUrl(title: String): Result<BookPreviewResult> {
        return try {
            // Search Open Library for the book
            val response = openLibraryService.searchBooks(
                query = title,
                limit = 1
            )

            val book = response.docs.firstOrNull()
            if (book != null && book.canReadOnline()) {
                // Book has a readable version - go directly to the reader!
                val readUrl = book.getReadUrl()
                if (readUrl != null) {
                    return Result.success(BookPreviewResult(
                        previewUrl = readUrl,
                        title = book.title,
                        author = book.getAuthorString(),
                        thumbnailUrl = book.getCoverUrl("M")
                    ))
                }
            }

            // If we found a book but can't read online, go to its Open Library page
            if (book != null) {
                return Result.success(BookPreviewResult(
                    previewUrl = book.getOpenLibraryUrl(),
                    title = book.title,
                    author = book.getAuthorString(),
                    thumbnailUrl = book.getCoverUrl("M")
                ))
            }

            // Fallback: Open Library search
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            Result.success(BookPreviewResult(
                previewUrl = "https://openlibrary.org/search?q=$encodedTitle&has_fulltext=true",
                title = title,
                author = "",
                thumbnailUrl = null
            ))
        } catch (e: Exception) {
            // Fallback to Open Library search with readable books filter
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            Result.success(BookPreviewResult(
                previewUrl = "https://openlibrary.org/search?q=$encodedTitle&has_fulltext=true",
                title = title,
                author = "",
                thumbnailUrl = null
            ))
        }
    }

    /**
     * Search for kids books by query
     */
    suspend fun searchBooks(query: String, limit: Int = 5): Result<List<OpenLibraryBook>> {
        return try {
            val response = openLibraryService.searchBooks(
                query = "$query children",
                limit = limit
            )
            Result.success(response.docs)
        } catch (e: Exception) {
            try {
                val response = openLibraryService.searchBooks(
                    query = query,
                    limit = limit
                )
                Result.success(response.docs)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    /**
     * Get the best URL for reading/viewing a book
     */
    fun getBookUrl(book: OpenLibraryBook): String {
        book.getReadUrl()?.let { return it }
        return book.getOpenLibraryUrl()
    }
}
