package com.example.fitto.feature_meal

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.FoodRepository
import com.example.fitto.data.remote.logmeal.LogMealFoodRecognitionClient
import com.example.fitto.databinding.FragmentCameraFoodEntryBinding
import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.fitto.data.remote.logmeal.LogMealFoodCandidate

class CameraFoodEntryFragment : Fragment(R.layout.fragment_camera_food_entry) {

    private var _binding: FragmentCameraFoodEntryBinding? = null
    private val binding get() = _binding!!

    private lateinit var foodRepository: FoodRepository
    private lateinit var logMealClient: LogMealFoodRecognitionClient

    private var imageCapture: ImageCapture? = null
    private var capturedPhotoFile: File? = null

    private val targetMealType: MealType by lazy {
        MealType.fromArgument(arguments?.getString(ARG_MEAL_TYPE))
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            binding.statusText.text = getString(R.string.camera_food_permission_denied)
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_food_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCameraFoodEntryBinding.bind(view)

        foodRepository = FoodRepository(requireContext().applicationContext)

        logMealClient = LogMealFoodRecognitionClient(
            com.example.fitto.BuildConfig.LOGMEAL_API_USER_TOKEN
        )
        setupLogMealCandidateSelectionListener()
        setupClicks()
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        retakeButton.setOnClickListener {
            resetCaptureState()
        }

        usePhotoButton.setOnClickListener {
            analyzeCapturedPhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(
            Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview: Preview = Preview.Builder()
                    .build()
                    .also { previewUseCase: Preview ->
                        previewUseCase.setSurfaceProvider(
                            binding.cameraPreviewView.surfaceProvider
                        )
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                runCatching {
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }.onFailure { throwable ->
                    android.util.Log.e("CameraFoodEntry", "Camera start failed", throwable)
                    binding.statusText.text = getString(R.string.camera_food_start_error)
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.captureButton.isEnabled = false
        binding.statusText.text = getString(R.string.camera_food_capturing)

        val photoFile = File(
            requireContext().cacheDir,
            "fitto_food_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturedPhotoFile = photoFile
                    showCapturedPhoto(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    android.util.Log.e("CameraFoodEntry", "Photo capture failed", exception)

                    binding.captureButton.isEnabled = true
                    binding.statusText.text = getString(R.string.camera_food_capture_error)
                }
            }
        )
    }

    private fun showCapturedPhoto(photoFile: File) = with(binding) {
        capturedImageView.setImageURI(Uri.fromFile(photoFile))
        capturedImageView.isVisible = true

        captureButton.isEnabled = true
        captureButton.isVisible = false

        afterCaptureActions.isVisible = true
        statusText.text = getString(R.string.camera_food_preview_ready)
    }

    private fun resetCaptureState() = with(binding) {
        capturedPhotoFile?.delete()
        capturedPhotoFile = null

        capturedImageView.setImageDrawable(null)
        capturedImageView.isVisible = false

        captureButton.isEnabled = true
        captureButton.isVisible = true

        usePhotoButton.isEnabled = true
        retakeButton.isEnabled = true
        afterCaptureActions.isVisible = false

        statusText.text = getString(R.string.camera_food_subtitle)
    }

    private fun analyzeCapturedPhoto() {
        android.util.Log.d("CameraFoodEntry", "Use photo clicked. Starting analysis.")

        val photoFile = capturedPhotoFile ?: return

        binding.usePhotoButton.isEnabled = false
        binding.retakeButton.isEnabled = false
        binding.statusText.text = getString(R.string.camera_food_analyzing)

        viewLifecycleOwner.lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                findCandidatesWithLogMeal(photoFile)
            }

            if (_binding == null) return@launch

            binding.usePhotoButton.isEnabled = true
            binding.retakeButton.isEnabled = true

            val nutritionCandidates = candidates.filter { it.hasNutrition() }

            if (nutritionCandidates.isEmpty()) {
                binding.statusText.text = getString(R.string.camera_food_no_match)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_food_no_match),
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            LogMealCandidateSelectionBottomSheet
                .newInstance(nutritionCandidates)
                .show(
                    childFragmentManager,
                    LogMealCandidateSelectionBottomSheet.TAG
                )
        }
    }

    private suspend fun findCandidatesWithLogMeal(
        photoFile: File
    ): List<LogMealFoodCandidate> {
        val languageCode = resources.configuration.locales[0].language

        android.util.Log.d(
            "CameraFoodEntry",
            "Starting LogMeal analysis. photo=${photoFile.name}, size=${photoFile.length()}"
        )

        val candidates = logMealClient.recognizeFood(
            imageFile = photoFile,
            languageCode = languageCode
        )

        android.util.Log.d(
            "CameraFoodEntry",
            "LogMeal candidates count=${candidates.size}"
        )

        candidates.forEach { candidate ->
            android.util.Log.d(
                "CameraFoodEntry",
                "Candidate name=${candidate.name}, confidence=${candidate.confidence}, " +
                        "grams=${candidate.servingGrams}, kcal=${candidate.calories}, " +
                        "protein=${candidate.protein}, carbs=${candidate.carbs}, fat=${candidate.fat}"
            )
        }

        return candidates
    }
    private fun setupLogMealCandidateSelectionListener() {
        childFragmentManager.setFragmentResultListener(
            LogMealCandidateSelectionBottomSheet.REQUEST_CANDIDATES_SELECTED,
            viewLifecycleOwner
        ) { _, bundle ->
            @Suppress("DEPRECATION")
            val selectedCandidates = bundle
                .getSerializable(LogMealCandidateSelectionBottomSheet.BUNDLE_SELECTED_CANDIDATES)
                .let { it as? ArrayList<LogMealFoodCandidate> }
                .orEmpty()

            if (selectedCandidates.isEmpty()) return@setFragmentResultListener

            binding.statusText.text = getString(R.string.camera_food_adding_selected)

            viewLifecycleOwner.lifecycleScope.launch {
                val foodIds = withContext(Dispatchers.IO) {
                    selectedCandidates.map { candidate ->
                        foodRepository.createFoodFromLogMealCameraResult(
                            candidate = candidate,
                            mealType = targetMealType.argumentValue
                        ).id
                    }
                }

                if (_binding == null) return@launch

                parentFragmentManager.setFragmentResult(
                    REQUEST_CAMERA_FOODS_RECOGNIZED,
                    Bundle().apply {
                        putStringArrayList(BUNDLE_FOOD_IDS, ArrayList(foodIds))
                    }
                )

                parentFragmentManager.popBackStack()
            }
        }

        childFragmentManager.setFragmentResultListener(
            LogMealCandidateSelectionBottomSheet.REQUEST_MANUAL_SEARCH_SELECTED,
            viewLifecycleOwner
        ) { _, _ ->
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        capturedPhotoFile = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "camera_food_entry"
        const val BACK_STACK_NAME = "camera_food_entry"
        const val REQUEST_CAMERA_FOOD_RECOGNIZED = "request_camera_food_recognized"
        const val BUNDLE_FOOD_ID = "bundle_food_id"
        const val REQUEST_CAMERA_FOODS_RECOGNIZED = "request_camera_foods_recognized"
        const val BUNDLE_FOOD_IDS = "bundle_food_ids"
        private const val ARG_MEAL_TYPE = "arg_meal_type"

        fun newInstance(
            mealType: MealType
        ): CameraFoodEntryFragment {
            return CameraFoodEntryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType.argumentValue)
                }
            }
        }
    }
}