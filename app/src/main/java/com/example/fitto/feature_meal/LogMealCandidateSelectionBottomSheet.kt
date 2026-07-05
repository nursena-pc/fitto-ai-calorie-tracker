package com.example.fitto.feature_meal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.data.remote.logmeal.LogMealFoodCandidate
import com.example.fitto.databinding.BottomSheetLogmealCandidatesBinding
import com.example.fitto.databinding.ItemLogmealCandidateBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class LogMealCandidateSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLogmealCandidatesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LogMealCandidateAdapter

    private val rawCandidates: List<LogMealFoodCandidate> by lazy {
        requireArguments().getLogMealCandidatePayload(ARG_CANDIDATES)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLogmealCandidatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPortionUpdateListener()

        adapter = LogMealCandidateAdapter(
            onSelectionChanged = {
                updateAddButton()
            },
            onEditCandidate = { candidate ->
                LogMealCandidatePortionBottomSheet
                    .newInstance(candidate)
                    .show(
                        childFragmentManager,
                        LogMealCandidatePortionBottomSheet.TAG
                    )
            }
        )

        binding.candidatesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LogMealCandidateSelectionBottomSheet.adapter
            itemAnimator = null
        }

        val displayCandidates = prepareDisplayCandidates(rawCandidates)

        Log.d(
            "LogMealCandidateSelection",
            "raw=${rawCandidates.size}, display=${displayCandidates.size}, names=${displayCandidates.joinToString { it.name }}"
        )

        adapter.submitCandidates(displayCandidates)

        binding.addSelectedButton.setOnClickListener {
            val selectedCandidates = adapter.getSelectedCandidates()

            Log.d(
                "LogMealCandidateSelection",
                "Selected candidates=${selectedCandidates.joinToString { it.name }}"
            )

            if (selectedCandidates.isEmpty()) return@setOnClickListener

            parentFragmentManager.setFragmentResult(
                REQUEST_CANDIDATES_SELECTED,
                Bundle().apply {
                    putLogMealCandidatePayload(
                        BUNDLE_SELECTED_CANDIDATES,
                        selectedCandidates
                    )
                }
            )

            dismiss()
        }

        binding.manualSearchButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_MANUAL_SEARCH_SELECTED,
                Bundle.EMPTY
            )
            dismiss()
        }

        updateAddButton()
    }

    private fun setupPortionUpdateListener() {
        childFragmentManager.setFragmentResultListener(
            LogMealCandidatePortionBottomSheet.REQUEST_PORTION_UPDATED,
            viewLifecycleOwner
        ) { _, bundle ->
            val updatedCandidate = BundleCompat.getSerializable(
                bundle,
                LogMealCandidatePortionBottomSheet.BUNDLE_UPDATED_CANDIDATE,
                LogMealFoodCandidate::class.java
            ) ?: return@setFragmentResultListener

            adapter.updateCandidate(updatedCandidate)
            updateAddButton()
        }
    }

    private fun prepareDisplayCandidates(
        candidates: List<LogMealFoodCandidate>
    ): List<LogMealFoodCandidate> {
        val genericNames = setOf(
            "gıda",
            "malzemeler",
            "kombinasyon yemek",
            "süt ürünleri",
            "sebzeler",
            "meyve",
            "içecek",
            "içecekler",
            "tatlılar",
            "baklagiller",
            "et",
            "fındık",
            "yumurta",
            "deniz ürünleri",
            "balık"
        )

        return candidates
            .filter { candidate ->
                candidate.hasNutrition() && candidate.name.isNotBlank()
            }
            .filterNot { candidate ->
                candidate.name.trim().lowercase() in genericNames
            }
            .groupBy { candidate ->
                candidate.foodItemPosition?.let { "position_$it" }
                    ?: "${candidate.servingGrams}_${candidate.calories}_${candidate.protein}_${candidate.carbs}_${candidate.fat}"
            }
            .map { (_, group) ->
                chooseBestCandidateName(group)
            }
            .sortedWith(
                compareBy<LogMealFoodCandidate> { candidate ->
                    candidate.foodItemPosition ?: Int.MAX_VALUE
                }.thenByDescending { candidate ->
                    candidate.confidence
                }
            )
            .take(8)
    }

    private fun chooseBestCandidateName(
        group: List<LogMealFoodCandidate>
    ): LogMealFoodCandidate {
        return group
            .sortedWith(
                compareByDescending<LogMealFoodCandidate> { candidate ->
                    isCleanFoodName(candidate.name)
                }.thenByDescending { candidate ->
                    candidate.confidence
                }.thenBy { candidate ->
                    candidate.name.length
                }
            )
            .first()
    }

    private fun isCleanFoodName(name: String): Boolean {
        val normalized = name.trim().lowercase()

        val noisyWords = listOf(
            "ile",
            "chorizo",
            "jambon",
            "pineaple",
            "yangzhou",
            "nasi goreng",
            "küba tarzı",
            "karides",
            "morina"
        )

        return noisyWords.none { noisyWord ->
            normalized.contains(noisyWord)
        }
    }

    private fun updateAddButton() {
        val count = if (::adapter.isInitialized) adapter.getSelectedCandidates().size else 0

        binding.addSelectedButton.isEnabled = count > 0
        binding.addSelectedButton.alpha = if (count > 0) 1f else 0.55f
        binding.addSelectedButton.text = if (count > 0) {
            getString(R.string.logmeal_candidates_add_selected_count, count)
        } else {
            getString(R.string.logmeal_candidates_add_selected)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "logmeal_candidate_selection"

        const val REQUEST_CANDIDATES_SELECTED = "request_logmeal_candidates_selected"
        const val REQUEST_MANUAL_SEARCH_SELECTED = "request_logmeal_manual_search_selected"
        const val BUNDLE_SELECTED_CANDIDATES = "bundle_selected_candidates"

        private const val ARG_CANDIDATES = "arg_candidates"

        fun newInstance(
            candidates: List<LogMealFoodCandidate>
        ): LogMealCandidateSelectionBottomSheet {
            return LogMealCandidateSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putLogMealCandidatePayload(ARG_CANDIDATES, candidates)
                }
            }
        }
    }
}

private class LogMealCandidateAdapter(
    private val onSelectionChanged: () -> Unit,
    private val onEditCandidate: (LogMealFoodCandidate) -> Unit
) : RecyclerView.Adapter<LogMealCandidateAdapter.ViewHolder>() {

    private val candidates = mutableListOf<LogMealFoodCandidate>()
    private val selectedKeys = mutableSetOf<String>()

    fun submitCandidates(newCandidates: List<LogMealFoodCandidate>) {
        candidates.clear()
        candidates.addAll(newCandidates)

        selectedKeys.clear()
        selectedKeys.addAll(newCandidates.map { it.stableSelectionKey() })

        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun updateCandidate(updatedCandidate: LogMealFoodCandidate) {
        val key = updatedCandidate.stableSelectionKey()

        val index = candidates.indexOfFirst { candidate ->
            candidate.stableSelectionKey() == key
        }

        if (index == -1) return

        candidates[index] = updatedCandidate
        selectedKeys.add(key)

        notifyItemChanged(index)
        onSelectionChanged()
    }

    fun getSelectedCandidates(): List<LogMealFoodCandidate> {
        return candidates.filter { candidate ->
            candidate.stableSelectionKey() in selectedKeys
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogmealCandidateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = candidates.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(candidates[position])
    }

    inner class ViewHolder(
        private val binding: ItemLogmealCandidateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(candidate: LogMealFoodCandidate) = with(binding) {
            val key = candidate.stableSelectionKey()

            nameText.text = candidate.name.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }

            nameText.setOnClickListener {
                onEditCandidate(candidate)
            }

            nameRow.setOnClickListener {
                onEditCandidate(candidate)
            }

            editText.setOnClickListener {
                onEditCandidate(candidate)
            }

            calorieText.text = itemView.context.getString(
                R.string.logmeal_candidate_kcal,
                candidate.calories?.roundToInt() ?: 0
            )

            detailText.text = itemView.context.getString(
                R.string.logmeal_candidate_detail,
                formatAmount(candidate.servingGrams ?: 100.0),
                formatAmount(candidate.protein ?: 0.0),
                formatAmount(candidate.carbs ?: 0.0),
                formatAmount(candidate.fat ?: 0.0)
            )

            candidateCheckBox.setOnCheckedChangeListener(null)
            candidateCheckBox.isChecked = key in selectedKeys

            rootCard.setOnClickListener {
                toggleCandidate(key)
            }

            candidateCheckBox.setOnClickListener {
                toggleCandidate(key)
            }
        }

        private fun toggleCandidate(key: String) {
            if (bindingAdapterPosition == RecyclerView.NO_POSITION) return

            if (key in selectedKeys) {
                selectedKeys.remove(key)
            } else {
                selectedKeys.add(key)
            }

            notifyItemChanged(bindingAdapterPosition)
            onSelectionChanged()
        }
    }

    private fun LogMealFoodCandidate.stableSelectionKey(): String {
        return foodItemPosition?.let { "position_$it" }
            ?: name.trim().lowercase()
    }

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}