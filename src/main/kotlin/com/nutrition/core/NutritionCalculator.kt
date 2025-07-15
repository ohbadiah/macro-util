package com.nutrition.core

import com.nutrition.models.Recipe
import com.nutrition.models.RecipeIngredient
import com.nutrition.models.RecipeNutrition

object NutritionCalculator {
    private const val CALORIES_PER_GRAM_PROTEIN = 4.0
    private const val CALORIES_PER_GRAM_CARBS = 4.0
    private const val CALORIES_PER_GRAM_FAT = 9.0

    fun calculateRecipeNutrition(recipe: Recipe): RecipeNutrition {
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0

        recipe.ingredients.forEach { recipeIngredient ->
            val ingredient = recipeIngredient.ingredient
            val multiplier = calculateMultiplier(recipeIngredient)

            totalCalories += ingredient.calories * multiplier
            totalProtein += ingredient.protein * multiplier
            totalFat += ingredient.fat * multiplier
            totalCarbs += ingredient.carbs * multiplier
        }

        val proteinCalories = totalProtein * CALORIES_PER_GRAM_PROTEIN
        val fatCalories = totalFat * CALORIES_PER_GRAM_FAT
        val carbCalories = totalCarbs * CALORIES_PER_GRAM_CARBS

        val proteinPercentage = if (totalCalories > 0) (proteinCalories / totalCalories) * 100 else 0.0
        val fatPercentage = if (totalCalories > 0) (fatCalories / totalCalories) * 100 else 0.0
        val carbsPercentage = if (totalCalories > 0) (carbCalories / totalCalories) * 100 else 0.0

        return RecipeNutrition(
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalFat = totalFat,
            totalCarbs = totalCarbs,
            proteinPercentage = proteinPercentage,
            fatPercentage = fatPercentage,
            carbsPercentage = carbsPercentage,
        )
    }

    private fun calculateMultiplier(recipeIngredient: RecipeIngredient): Double {
        val ingredient = recipeIngredient.ingredient
        
        return when {
            recipeIngredient.weightGrams != null && ingredient.servingWeightGrams != null -> {
                recipeIngredient.weightGrams / ingredient.servingWeightGrams
            }
            else -> recipeIngredient.servings
        }
    }
}
