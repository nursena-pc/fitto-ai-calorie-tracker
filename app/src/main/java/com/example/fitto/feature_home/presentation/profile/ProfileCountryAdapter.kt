package com.example.fitto.feature_home.presentation.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.databinding.ItemCountryBinding

internal class ProfileCountryAdapter(
    selectedCountryCode: String,
    private val onCountryClick: (ProfileCountryUiModel) -> Unit
) : RecyclerView.Adapter<ProfileCountryAdapter.CountryViewHolder>() {

    private var countries: List<ProfileCountryUiModel> = emptyList()
    private var selectedCountryCode: String = selectedCountryCode

    fun submitList(newCountries: List<ProfileCountryUiModel>) {
        countries = newCountries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CountryViewHolder {
        return CountryViewHolder(
            ItemCountryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = countries.size

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(countries[position])
    }

    inner class CountryViewHolder(
        private val binding: ItemCountryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(country: ProfileCountryUiModel) = with(binding) {
            val isSelected = selectedCountryCode == country.code

            countryFlagText.text = country.flagEmoji
            countryNameText.text = country.name
            countryCheckContainer.isVisible = isSelected
            countryChevronIcon.isVisible = !isSelected

            root.setOnClickListener {
                selectedCountryCode = country.code
                onCountryClick(country)
                notifyDataSetChanged()
            }
        }
    }
}
