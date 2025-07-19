package com.nutrition.cli

import com.nutrition.models.Ingredient

object CustomIngredientUtil {
    fun createCustomIngredient(echo: (String) -> Unit, prompt: (String, String?) -> String?): Ingredient? {
        echo("\n=== Create Custom Ingredient ===")
        
        val name = prompt("Ingredient name", null) ?: return null
        if (name.isBlank()) {
            echo("Name cannot be empty.")
            return null
        }
        
        val servingSize = prompt("Serving size (e.g., 1, 0.5, 2)", "1")?.toDoubleOrNull()
        if (servingSize == null || servingSize <= 0) {
            echo("Invalid serving size. Must be a positive number.")
            return null
        }
        
        val servingUnit = prompt("Serving unit (e.g., cup, gram, piece, slice)", "serving") ?: "serving"
        if (servingUnit.isBlank()) {
            echo("Serving unit cannot be empty.")
            return null
        }
        
        val servingWeightStr = prompt("Serving weight in grams (optional, press enter to skip)", null)
        val servingWeightGrams = if (servingWeightStr.isNullOrBlank()) {
            null
        } else {
            servingWeightStr.toDoubleOrNull()?.takeIf { it > 0 }
        }
        
        val calories = prompt("Calories per serving", "0")?.toDoubleOrNull()
        if (calories == null || calories < 0) {
            echo("Invalid calories. Must be a non-negative number.")
            return null
        }
        
        val protein = prompt("Protein per serving (grams)", "0")?.toDoubleOrNull()
        if (protein == null || protein < 0) {
            echo("Invalid protein. Must be a non-negative number.")
            return null
        }
        
        val fat = prompt("Fat per serving (grams)", "0")?.toDoubleOrNull()
        if (fat == null || fat < 0) {
            echo("Invalid fat. Must be a non-negative number.")
            return null
        }
        
        val carbs = prompt("Carbs per serving (grams)", "0")?.toDoubleOrNull()
        if (carbs == null || carbs < 0) {
            echo("Invalid carbs. Must be a non-negative number.")
            return null
        }
        
        val ingredient = Ingredient(
            name = name,
            servingSize = servingSize,
            servingUnit = servingUnit,
            servingWeightGrams = servingWeightGrams,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs
        )
        
        // Show summary
        val weightInfo = if (servingWeightGrams != null) " (~${servingWeightGrams.toInt()}g)" else ""
        echo("\nCustom ingredient created:")
        echo("  ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
        echo("  Calories: ${ingredient.calories}, Protein: ${ingredient.protein}g, Fat: ${ingredient.fat}g, Carbs: ${ingredient.carbs}g")
        
        return ingredient
    }
}