package com.nutrition.core

import com.nutrition.models.*

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
    
    fun calculateRecipeNutritionPerServing(recipe: Recipe): RecipeNutrition {
        val totalNutrition = calculateRecipeNutrition(recipe)
        return RecipeNutrition(
            totalCalories = totalNutrition.totalCalories / recipe.servings,
            totalProtein = totalNutrition.totalProtein / recipe.servings,
            totalFat = totalNutrition.totalFat / recipe.servings,
            totalCarbs = totalNutrition.totalCarbs / recipe.servings,
            proteinPercentage = totalNutrition.proteinPercentage,
            fatPercentage = totalNutrition.fatPercentage,
            carbsPercentage = totalNutrition.carbsPercentage,
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

    fun calculateDayNutrition(journal: FoodJournal): DayNutrition {
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0

        journal.entries.forEach { entry ->
            totalCalories += entry.calories
            totalProtein += entry.protein
            totalFat += entry.fat
            totalCarbs += entry.carbs
        }

        val proteinCalories = totalProtein * CALORIES_PER_GRAM_PROTEIN
        val fatCalories = totalFat * CALORIES_PER_GRAM_FAT
        val carbCalories = totalCarbs * CALORIES_PER_GRAM_CARBS

        val proteinPercentage = if (totalCalories > 0) (proteinCalories / totalCalories) * 100 else 0.0
        val fatPercentage = if (totalCalories > 0) (fatCalories / totalCalories) * 100 else 0.0
        val carbsPercentage = if (totalCalories > 0) (carbCalories / totalCalories) * 100 else 0.0

        return DayNutrition(
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalFat = totalFat,
            totalCarbs = totalCarbs,
            proteinPercentage = proteinPercentage,
            fatPercentage = fatPercentage,
            carbsPercentage = carbsPercentage,
        )
    }

    fun calculateIngredientNutrition(ingredient: Ingredient, servings: Double): JournalEntry {
        return JournalEntry(
            type = EntryType.INGREDIENT,
            name = ingredient.name,
            servings = servings,
            calories = ingredient.calories * servings,
            protein = ingredient.protein * servings,
            fat = ingredient.fat * servings,
            carbs = ingredient.carbs * servings,
        )
    }

    fun calculateRecipeNutritionForJournal(recipe: Recipe, servings: Double): JournalEntry {
        val recipeNutrition = calculateRecipeNutrition(recipe)
        // Calculate per-serving nutrition by dividing by recipe servings
        val perServingCalories = recipeNutrition.totalCalories / recipe.servings
        val perServingProtein = recipeNutrition.totalProtein / recipe.servings
        val perServingFat = recipeNutrition.totalFat / recipe.servings
        val perServingCarbs = recipeNutrition.totalCarbs / recipe.servings
        
        return JournalEntry(
            type = EntryType.RECIPE,
            name = recipe.name,
            servings = servings,
            calories = perServingCalories * servings,
            protein = perServingProtein * servings,
            fat = perServingFat * servings,
            carbs = perServingCarbs * servings,
        )
    }

    data class ServingCalculation(
        val servings: Double,
        val displayText: String
    )

    fun parseServingInput(input: String, ingredient: Ingredient): ServingCalculation? {
        val trimmed = input.trim()
        
        // Try to parse weight-based inputs (e.g., "150g", "150 g", "5oz", "5 oz")
        val weightRegex = Regex("""^(\d+(?:\.\d+)?)\s*(g|oz)$""", RegexOption.IGNORE_CASE)
        val weightMatch = weightRegex.find(trimmed)
        
        if (weightMatch != null) {
            val amount = weightMatch.groupValues[1].toDoubleOrNull() ?: return null
            val unit = weightMatch.groupValues[2].lowercase()
            
            // Convert to grams if needed
            val grams = when (unit) {
                "g" -> amount
                "oz" -> amount * 28.3495 // 1 oz = 28.3495 g
                else -> return null
            }
            
            // Calculate servings based on weight
            if (ingredient.servingWeightGrams != null && ingredient.servingWeightGrams > 0) {
                val servings = grams / ingredient.servingWeightGrams
                val displayText = "%.1f servings (${grams.toInt()}g)".format(servings)
                return ServingCalculation(servings, displayText)
            } else {
                // No weight information available for this ingredient
                return null
            }
        }
        
        // Try to parse as regular serving number
        val servings = trimmed.toDoubleOrNull()
        if (servings != null && servings > 0) {
            val displayText = if (servings == servings.toInt().toDouble()) {
                "${servings.toInt()} servings"
            } else {
                "%.1f servings".format(servings)
            }
            return ServingCalculation(servings, displayText)
        }
        
        return null
    }
}
