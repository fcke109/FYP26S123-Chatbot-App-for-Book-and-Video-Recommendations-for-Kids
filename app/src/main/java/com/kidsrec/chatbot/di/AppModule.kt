package com.kidsrec.chatbot.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.kidsrec.chatbot.data.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

// Provides app-wide dependencies using Hilt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Firebase Authentication instance
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // Firebase Firestore database instance
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // Firebase Cloud Functions instance
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    // Firebase Storage instance
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // YouTube service using Firebase Functions
    @Provides
    @Singleton
    fun provideYouTubeService(functions: FirebaseFunctions): YouTubeService = YouTubeService(functions)

    // Retrofit setup for Storyweaver API
    @Provides
    @Singleton
    @Named("SWRetrofit")
    fun provideStoryweaverRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://storyweaver.org.in/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    // Storyweaver API service
    @Provides
    @Singleton
    fun provideStoryweaverService(@Named("SWRetrofit") retrofit: Retrofit): StoryweaverService =
        retrofit.create(StoryweaverService::class.java)

    // Retrofit setup for Gutendex API
    @Provides
    @Singleton
    @Named("GutendexRetrofit")
    fun provideGutendexRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://gutendex.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Gutendex API service
    @Provides
    @Singleton
    fun provideGutendexService(@Named("GutendexRetrofit") retrofit: Retrofit): GutendexService =
        retrofit.create(GutendexService::class.java)


    // Retrofit setup for Open Library API
    @Provides
    @Singleton
    @Named("OpenLibraryRetrofit")
    fun provideOpenLibraryRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Open Library API service
    @Provides
    @Singleton
    fun provideOpenLibraryService(@Named("OpenLibraryRetrofit") retrofit: Retrofit): OpenLibraryService =
        retrofit.create(OpenLibraryService::class.java)

    // Retrofit setup for Google Books API
    @Provides
    @Singleton
    @Named("GoogleBooksRetrofit")
    fun provideGoogleBooksRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Google Books API service
    @Provides
    @Singleton
    fun provideGoogleBooksService(@Named("GoogleBooksRetrofit") retrofit: Retrofit): GoogleBooksService =
        retrofit.create(GoogleBooksService::class.java)
}
