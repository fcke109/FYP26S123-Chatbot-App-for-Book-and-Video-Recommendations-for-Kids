package com.kidsrec.chatbot.di

import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.ContentRepository
import com.kidsrec.chatbot.data.CollaborativeFilteringService
import com.kidsrec.chatbot.data.repository.GamificationManager
import com.kidsrec.chatbot.data.repository.LearningProgressManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt module for providing repository-related classes
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // Provides LearningProgressManager instance
    @Provides
    @Singleton
    fun provideLearningProgressManager(
        firestore: FirebaseFirestore
    ): LearningProgressManager {
        return LearningProgressManager(firestore)
    }

    // Provides GamificationManager instance
    @Provides
    @Singleton
    fun provideGamificationManager(
        firestore: FirebaseFirestore
    ): GamificationManager {
        return GamificationManager(firestore)
    }

    // Provides ContentRepository instance
    @Provides
    @Singleton
    fun provideContentRepository(
        firestore: FirebaseFirestore
    ): ContentRepository = ContentRepository(firestore)

    // Provides collaborative filtering recommendation service
    @Provides
    @Singleton
    fun provideCollaborativeFilteringService(
        firestore: FirebaseFirestore
    ): CollaborativeFilteringService = CollaborativeFilteringService(firestore)
}