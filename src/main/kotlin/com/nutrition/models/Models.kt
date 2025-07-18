package com.nutrition.models

import java.time.LocalDate

data class Ingredient(
    val id: Int = 0,
    val name: String,
    val servingSize: Double,
    val servingUnit: String,
    val servingWeightGrams: Double?,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

data class Recipe(
    val id: Int = 0,
    val name: String,
    val servings: Double,
    val ingredients: List<RecipeIngredient> = emptyList(),
)

data class RecipeIngredient(
    val ingredient: Ingredient,
    val servings: Double,
    val weightGrams: Double? = null,
)

data class NutritionixResponse(
    val foods: List<NutritionixFood>,
)

data class NutritionixFood(
    val food_name: String,
    val serving_qty: Double,
    val serving_unit: String,
    val serving_weight_grams: Double?,
    val nf_calories: Double,
    val nf_protein: Double,
    val nf_total_fat: Double,
    val nf_total_carbohydrate: Double,
)

data class NutritionixInstantResponse(
    val common: List<NutritionixCommonFood>,
    val branded: List<NutritionixBrandedFood>,
)

data class NutritionixCommonFood(
    val food_name: String,
    val serving_unit: String?,
    val serving_qty: Double?,
    val nf_calories: Double,
    val photo: NutritionixPhoto?,
)

data class NutritionixBrandedFood(
    val food_name: String,
    val serving_unit: String?,
    val serving_qty: Double?,
    val nf_calories: Double,
    val brand_name: String?,
    val nix_item_id: String,
    val photo: NutritionixPhoto?,
)

data class NutritionixPhoto(
    val thumb: String?,
)

data class RecipeNutrition(
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val proteinPercentage: Double,
    val fatPercentage: Double,
    val carbsPercentage: Double,
)

enum class EntryType {
    RECIPE, INGREDIENT
}

data class JournalEntry(
    val id: Int = 0,
    val type: EntryType,
    val name: String,
    val servings: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

data class FoodJournal(
    val id: Int = 0,
    val date: LocalDate,
    val entries: List<JournalEntry> = emptyList(),
)

data class DayNutrition(
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val proteinPercentage: Double,
    val fatPercentage: Double,
    val carbsPercentage: Double,
)
