package com.example.fitto.feature_home.presentation.social

import com.example.fitto.feature_recipes.data.RecipeRepository
import kotlinx.coroutines.CoroutineDispatcher

/** Shared social and messaging dependency graph owned by the application composition root. */
internal class SocialFeatureDependencies(
    repositoryFactory: () -> SocialRepository,
    recipeRepositoryFactory: () -> RecipeRepository,
    val ioDispatcher: CoroutineDispatcher,
    private val startPresence: () -> Unit
) {
    val repository: SocialRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        repositoryFactory()
    }

    val recipeRepository: RecipeRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        recipeRepositoryFactory()
    }

    fun createViewModelFactory(): SocialViewModelFactory {
        return SocialViewModelFactory(repository)
    }

    fun startRealtimePresence() {
        startPresence.invoke()
    }
}
