package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCountryPickerBinding
import com.example.fitto.databinding.FragmentRegionBinding
import com.example.fitto.databinding.ItemCountryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.feature_home.presentation.profile.ProfileLanguage
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
class RegionFragment : Fragment(R.layout.fragment_region) {

    private var _binding: FragmentRegionBinding? = null
    private val binding get() = _binding!!

    private val appLanguage: AppLanguage by lazy {
        when (ProfilePreferenceStore.getLanguage(requireContext())) {
            ProfileLanguage.ENGLISH -> AppLanguage.ENGLISH
            ProfileLanguage.TURKISH -> AppLanguage.TURKISH
        }
    }
    private val countries: List<CountryUiModel> by lazy {
        createCountryList()
    }

    private var selectedCountry: CountryUiModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRegionBinding.bind(view)
        binding.root.setOnboardingProgress(2)
        selectedCountry = countries.firstOrNull { it.code == TURKEY_CODE }
            ?: countries.firstOrNull()

        setupClicks()
        updateSelectedCountryUi()
    }

    private fun setupClicks() = with(binding) {
        regionBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        primaryRegionCard.setOnClickListener {
            updateSelectedCountryUi()
        }

        otherRegionCard.setOnClickListener {
            showCountryPickerSheet()
        }

        regionNextButton.setOnClickListener {
            findNavController().navigate(R.id.action_regionFragment_to_userInfoFragment)
        }
    }

    private fun updateSelectedCountryUi() = with(binding) {
        val country = selectedCountry ?: return@with

        primaryRegionFlagText.text = country.flagEmoji
        primaryRegionTitleText.text = country.name

        primaryRegionSubtitleText.isVisible = country.isRecommended
        primaryRegionSubtitleText.text = if (country.isRecommended) {
            getString(R.string.region_recommended)
        } else {
            ""
        }

        primaryRegionCheckContainer.isVisible = true
        regionNextButton.isEnabled = true
        regionNextButton.alpha = 1f
    }

    private fun showCountryPickerSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetCountryPickerBinding.inflate(layoutInflater)

        var temporarySelectedCountry = selectedCountry ?: countries.first()

        val countryAdapter = CountryAdapter(
            appLanguage = appLanguage,
            selectedCountryCode = temporarySelectedCountry.code,
            onCountryClick = { country ->
                temporarySelectedCountry = country
            }
        )

        sheetBinding.countryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = countryAdapter
            setHasFixedSize(false)
        }
        fun submitCountries(query: String = "") {
            val cleanQuery = query.trim()

            val filteredCountries = if (cleanQuery.isBlank()) {
                countries
            } else {
                countries.filter { country ->
                    country.name.contains(cleanQuery, ignoreCase = true) ||
                            country.code.contains(cleanQuery, ignoreCase = true)
                }
            }

            countryAdapter.submitList(filteredCountries)
        }

        sheetBinding.countrySearchEditText.doAfterTextChanged { editable ->
            submitCountries(editable?.toString().orEmpty())
        }

        sheetBinding.countryPickerDoneText.setOnClickListener {
            selectedCountry = temporarySelectedCountry
            updateSelectedCountryUi()
            dialog.dismiss()
        }

        submitCountries()

        dialog.setContentView(sheetBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        dialog.show()
    }

    private fun createCountryList(): List<CountryUiModel> {
        val displayLocale = when (appLanguage) {
            AppLanguage.TURKISH -> Locale("tr", "TR")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }

        return Locale.getISOCountries()
            .mapNotNull { code ->
                val countryLocale = Locale("", code)
                val countryName = countryLocale.getDisplayCountry(displayLocale)

                if (countryName.isBlank()) {
                    null
                } else {
                    CountryUiModel(
                        code = code,
                        name = countryName,
                        flagEmoji = code.toFlagEmoji(),
                        supportedLanguages = supportedLanguagesForCountry(code),
                        isRecommended = code == TURKEY_CODE
                    )
                }
            }
            .sortedWith(
                compareBy<CountryUiModel> {
                    if (it.code == TURKEY_CODE) 0 else 1
                }.thenBy {
                    it.name.lowercase(displayLocale)
                }
            )
    }

    private fun supportedLanguagesForCountry(code: String): Set<AppLanguage> {
        return when (code.uppercase(Locale.US)) {
            TURKEY_CODE -> setOf(AppLanguage.TURKISH)

            "US", "GB", "AU", "CA", "NZ", "IE", "ZA", "SG", "IN" -> {
                setOf(AppLanguage.ENGLISH)
            }

            else -> emptySet()
        }
    }

    private fun String.toFlagEmoji(): String {
        val countryCode = uppercase(Locale.US)

        if (countryCode.length != 2) return "🌍"

        val firstLetter = Character.codePointAt(countryCode, 0) - 'A'.code + REGIONAL_INDICATOR_OFFSET
        val secondLetter = Character.codePointAt(countryCode, 1) - 'A'.code + REGIONAL_INDICATOR_OFFSET

        return String(Character.toChars(firstLetter)) +
                String(Character.toChars(secondLetter))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private enum class AppLanguage {
        TURKISH,
        ENGLISH
    }

    private data class CountryUiModel(
        val code: String,
        val name: String,
        val flagEmoji: String,
        val supportedLanguages: Set<AppLanguage> = emptySet(),
        val isRecommended: Boolean = false
    )

    private class CountryAdapter(
        private val appLanguage: AppLanguage,
        selectedCountryCode: String,
        private val onCountryClick: (CountryUiModel) -> Unit
    ) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

        private var countries: List<CountryUiModel> = emptyList()
        private var selectedCountryCode: String = selectedCountryCode

        fun submitList(newCountries: List<CountryUiModel>) {
            countries = newCountries
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
            val binding = ItemCountryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return CountryViewHolder(binding)
        }

        override fun getItemCount(): Int = countries.size

        override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
            holder.bind(countries[position])
        }

        inner class CountryViewHolder(
            private val binding: ItemCountryBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(country: CountryUiModel) = with(binding) {
                val isSelected = selectedCountryCode == country.code

                countryFlagText.text = country.flagEmoji
                countryNameText.text = country.name

                countryCheckContainer.isVisible = isSelected
                countryChevronIcon.isVisible = !isSelected

                countryCard.setBackgroundResource(
                    if (isSelected) {
                        R.drawable.bg_fitto_card_selected
                    } else {
                        R.drawable.bg_fitto_card_normal
                    }
                )

                val shouldShowLanguageWarning = isSelected &&
                        (
                                country.supportedLanguages.isEmpty() ||
                                        !country.supportedLanguages.contains(appLanguage)
                                )

                languageMismatchContainer.isVisible = shouldShowLanguageWarning

                root.setOnClickListener {
                    selectedCountryCode = country.code
                    onCountryClick(country)
                    notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        private const val TURKEY_CODE = "TR"
        private const val REGIONAL_INDICATOR_OFFSET = 0x1F1E6
    }
}