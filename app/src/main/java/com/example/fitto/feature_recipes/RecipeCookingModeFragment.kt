package com.example.fitto.feature_recipes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitto.R
import com.example.fitto.databinding.FragmentRecipeCookingModeBinding
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.fittoAppContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RecipeCookingModeFragment : Fragment(R.layout.fragment_recipe_cooking_mode) {

    private var _binding: FragmentRecipeCookingModeBinding? = null
    private val binding: FragmentRecipeCookingModeBinding
        get() = _binding!!

    private lateinit var dependencies: RecipeFeatureDependencies
    private lateinit var repository: RecipeRepository
    private var recipe: RecipeUiModel? = null
    private var stepIndex: Int = 0
    private var elapsedSeconds: Int = 0
    private var running: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!running || _binding == null) return
            elapsedSeconds++
            renderTimer()
            handler.postDelayed(this, ONE_SECOND_MS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecipeCookingModeBinding.bind(view)
        dependencies = requireContext().fittoAppContainer.createRecipeFeatureDependencies()
        repository = dependencies.repository
        binding.root.keepScreenOn = true

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.previousButton.setOnClickListener { moveStep(-1) }
        binding.nextButton.setOnClickListener { moveStep(1) }
        binding.finishButton.setOnClickListener { finishCooking() }
        binding.timerButton.setOnClickListener { toggleTimer() }

        val recipeId = arguments?.getString(ARG_RECIPE_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            recipe = withContext(dependencies.ioDispatcher) { recipeId?.let { repository.findById(it) } }
            if (_binding == null) return@launch
            if (recipe == null) {
                Toast.makeText(requireContext(), R.string.recipes_detail_not_found, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            renderStep()
            renderTimer()
        }
    }

    private fun moveStep(delta: Int) {
        val steps = recipe?.steps.orEmpty()
        if (steps.isEmpty()) return
        stepIndex = (stepIndex + delta).coerceIn(0, steps.size)
        renderStep()
    }

    private fun toggleTimer() {
        running = !running
        if (running) handler.postDelayed(timerRunnable, ONE_SECOND_MS)
        renderTimer()
    }

    private fun renderStep() = with(binding) {
        val currentRecipe = recipe ?: return@with
        val steps = currentRecipe.steps.ifEmpty { listOf(getString(R.string.recipes_cooking_no_steps)) }
        stepIndex = stepIndex.coerceIn(0, steps.size)
        titleText.text = currentRecipe.title

        val isDoneScreen = stepIndex == steps.size
        doneContainer.isVisible = isDoneScreen
        stepNumberText.isVisible = !isDoneScreen
        stepText.isVisible = !isDoneScreen

        if (isDoneScreen) {
            progressText.text = getString(R.string.recipes_cooking_done_title)
            Glide.with(doneMascotImage)
                .asGif()
                .load(R.drawable.fitto_mascot_anim_hi)
                .into(doneMascotImage)
        } else {
            progressText.text = getString(R.string.recipes_cooking_step_progress, stepIndex + 1, steps.size)
            stepNumberText.text = (stepIndex + 1).toString()
            stepText.text = steps[stepIndex]
        }

        previousButton.isEnabled = stepIndex > 0
        nextButton.isEnabled = !isDoneScreen
        previousButton.alpha = if (previousButton.isEnabled) 1f else 0.42f
        nextButton.alpha = if (nextButton.isEnabled) 1f else 0.42f
        nextButton.text = ""
        nextButton.setIconResource(R.drawable.ic_chevron_right)
        nextButton.contentDescription = getString(R.string.recipes_cooking_next)
    }

    private fun finishCooking() {
        running = false
        handler.removeCallbacks(timerRunnable)
        Toast.makeText(requireContext(), R.string.recipes_cooking_finished, Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun renderTimer() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val suffix = if (running) getString(R.string.recipes_cooking_timer_pause) else getString(R.string.recipes_cooking_timer_start)
        binding.timerButton.text = String.format(Locale.getDefault(), "%02d:%02d  %s", minutes, seconds, suffix)
    }

    override fun onDestroyView() {
        running = false
        handler.removeCallbacksAndMessages(null)
        binding.root.keepScreenOn = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_RECIPE_ID = "arg_recipe_id"
        private const val ONE_SECOND_MS = 1000L

        fun newInstance(recipeId: String): RecipeCookingModeFragment {
            return RecipeCookingModeFragment().apply {
                arguments = bundleOf(ARG_RECIPE_ID to recipeId)
            }
        }
    }
}
