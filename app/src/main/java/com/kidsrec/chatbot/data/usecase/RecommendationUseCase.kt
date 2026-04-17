package com.kidsrec.chatbot.data

class RecommendationUseCase(
    private val contentRepository: ContentRepository = ContentRepository(),
    private val cfService: CollaborativeFilteringService = CollaborativeFilteringService()
) {

    suspend fun getRecommendationsForUser(
        userId: String,
        childAge: Int,
        limit: Int = 10
    ): List<CFRecommendation> {
        val allItems = contentRepository.getAllContentItems()

        val filteredItems = allItems.filter {
            it.isKidSafe && childAge in it.ageMin..it.ageMax
        }

        return cfService.getHybridRecommendations(
            targetUserId = userId,
            allItems = filteredItems,
            limit = limit
        )
    }
}