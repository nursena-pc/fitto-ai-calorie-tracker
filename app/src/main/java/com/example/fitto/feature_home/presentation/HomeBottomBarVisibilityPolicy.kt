package com.example.fitto.feature_home.presentation

import androidx.fragment.app.Fragment
import com.example.fitto.feature_dietplans.CreateDietPlanFragment
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_dietplans.DietPlanFilterFragment
import com.example.fitto.feature_dietplans.DietPlanSearchFragment
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_dietplans.IntermittentFastingDetailFragment
import com.example.fitto.feature_exercise.AddExerciseFragment
import com.example.fitto.feature_home.presentation.profile.ProfileBodyMeasurementsFragment
import com.example.fitto.feature_home.presentation.profile.ProfileDetailFragment
import com.example.fitto.feature_home.presentation.profile.ProfileWeightFragment
import com.example.fitto.feature_meal.AddMealFragment
import com.example.fitto.feature_recipes.FridgeInventoryFragment
import com.example.fitto.feature_recipes.MyRecipesFragment
import com.example.fitto.feature_recipes.RecipeAddFragment
import com.example.fitto.feature_recipes.RecipeCookingModeFragment
import com.example.fitto.feature_recipes.RecipeDetailFragment
import com.example.fitto.feature_recipes.SavedRecipesFragment
import com.example.fitto.feature_recipes.ShoppingListFragment

object HomeBottomBarVisibilityPolicy {

    fun shouldHideFor(fragment: Fragment?): Boolean {
        return fragment is ProfileDetailFragment ||
                fragment is ProfileWeightFragment ||
                fragment is ProfileBodyMeasurementsFragment ||
                fragment is CreateDietPlanFragment ||
                fragment is DietPlanDetailFragment ||
                fragment is DietPlansFragment ||
                fragment is DietPlanSearchFragment ||
                fragment is DietPlanFilterFragment ||
                fragment is IntermittentFastingDetailFragment ||
                fragment is AddMealFragment ||
                fragment is AddExerciseFragment ||
                fragment is RecipeDetailFragment ||
                fragment is RecipeAddFragment ||
                fragment is RecipeCookingModeFragment ||
                fragment is FridgeInventoryFragment ||
                fragment is SavedRecipesFragment ||
                fragment is ShoppingListFragment ||
                fragment is MyRecipesFragment
    }
}
