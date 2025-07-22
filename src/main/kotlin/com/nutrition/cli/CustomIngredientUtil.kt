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

    fun customizeExistingIngredientStreamlined(
        baseIngredient: Ingredient,
        echo: (String) -> Unit,
        prompt: (String, String?) -> String?
    ): Ingredient? {
        echo("\n=== Customize Ingredient ===")
        echo("Base ingredient: ${baseIngredient.name}")
        echo("Current values: ${baseIngredient.calories.toInt()} cal, ${baseIngredient.protein}g protein, ${baseIngredient.fat}g fat, ${baseIngredient.carbs}g carbs")
        echo("Current serving: ${baseIngredient.servingSize} ${baseIngredient.servingUnit}${if (baseIngredient.servingWeightGrams != null) " (~${baseIngredient.servingWeightGrams.toInt()}g)" else ""}")
        echo("")
        
        val fieldsToChange = prompt("What do you want to change? \u001b[1m(n)\u001b[0mame, serving \u001b[1m(s)\u001b[0mize, serving \u001b[1m(u)\u001b[0mnit, serving \u001b[1m(w)\u001b[0meight in grams, \u001b[1m(p)\u001b[0mrotein, \u001b[1m(f)\u001b[0mat, or \u001b[1m(c)\u001b[0marbs (separate multiple with spaces)", "")
        if (fieldsToChange.isNullOrBlank()) {
            echo("No changes specified.")
            return null
        }
        
        val changeList = fieldsToChange.lowercase().split("\\s+".toRegex())
        var ingredient = baseIngredient
        var macrosChanged = false
        var servingWeightChanged = false
        val originalWeight = baseIngredient.servingWeightGrams
        
        for (change in changeList) {
            when {
                change.startsWith("n") -> {
                    val newName = prompt("New ingredient name", baseIngredient.name) ?: return null
                    if (newName.isNotBlank()) {
                        ingredient = ingredient.copy(name = newName)
                    }
                }
                change.startsWith("s") -> {
                    val newSize = prompt("New serving size", baseIngredient.servingSize.toString())?.toDoubleOrNull()
                    if (newSize != null && newSize > 0) {
                        ingredient = ingredient.copy(servingSize = newSize)
                    } else {
                        echo("Invalid serving size, keeping current value.")
                    }
                }
                change.startsWith("u") -> {
                    val newUnit = prompt("New serving unit", baseIngredient.servingUnit) ?: return null
                    if (newUnit.isNotBlank()) {
                        ingredient = ingredient.copy(servingUnit = newUnit)
                    }
                }
                change.startsWith("w") -> {
                    val currentWeight = baseIngredient.servingWeightGrams?.toString() ?: ""
                    val newWeightStr = prompt("New serving weight in grams", currentWeight)
                    val newWeight = if (newWeightStr.isNullOrBlank()) null else newWeightStr.toDoubleOrNull()
                    if (newWeightStr != null) {
                        ingredient = ingredient.copy(servingWeightGrams = newWeight)
                        servingWeightChanged = true
                    }
                }
                change.startsWith("p") -> {
                    val newProtein = prompt("New protein per serving (grams)", baseIngredient.protein.toString())?.toDoubleOrNull()
                    if (newProtein != null && newProtein >= 0) {
                        ingredient = ingredient.copy(protein = newProtein)
                        macrosChanged = true
                    } else {
                        echo("Invalid protein value, keeping current value.")
                    }
                }
                change.startsWith("f") -> {
                    val newFat = prompt("New fat per serving (grams)", baseIngredient.fat.toString())?.toDoubleOrNull()
                    if (newFat != null && newFat >= 0) {
                        ingredient = ingredient.copy(fat = newFat)
                        macrosChanged = true
                    } else {
                        echo("Invalid fat value, keeping current value.")
                    }
                }
                change.startsWith("c") -> {
                    val newCarbs = prompt("New carbs per serving (grams)", baseIngredient.carbs.toString())?.toDoubleOrNull()
                    if (newCarbs != null && newCarbs >= 0) {
                        ingredient = ingredient.copy(carbs = newCarbs)
                        macrosChanged = true
                    } else {
                        echo("Invalid carbs value, keeping current value.")
                    }
                }
                else -> {
                    echo("Unknown option: $change (ignoring)")
                }
            }
        }
        
        // Recalculate calories
        val finalIngredient = if (macrosChanged) {
            // Calculate calories from macros: 4 cal/g for protein and carbs, 9 cal/g for fat
            val calculatedCalories = (ingredient.protein * 4) + (ingredient.fat * 9) + (ingredient.carbs * 4)
            echo("Recalculated calories from macros: ${calculatedCalories.toInt()}")
            ingredient.copy(calories = calculatedCalories)
        } else if (servingWeightChanged && originalWeight != null && ingredient.servingWeightGrams != null) {
            // Scale calories based on weight change
            val weightRatio = ingredient.servingWeightGrams / originalWeight
            val scaledCalories = baseIngredient.calories * weightRatio
            echo("Scaled calories based on weight change: ${scaledCalories.toInt()}")
            ingredient.copy(calories = scaledCalories)
        } else {
            ingredient
        }
        
        // Show summary
        val weightInfo = if (finalIngredient.servingWeightGrams != null) " (~${finalIngredient.servingWeightGrams.toInt()}g)" else ""
        echo("\nCustomized ingredient:")
        echo("  ${finalIngredient.name} (${finalIngredient.servingSize} ${finalIngredient.servingUnit}$weightInfo)")
        echo("  Calories: ${finalIngredient.calories.toInt()}, Protein: ${finalIngredient.protein}g, Fat: ${finalIngredient.fat}g, Carbs: ${finalIngredient.carbs}g")
        
        return finalIngredient
    }

    fun customizeExistingIngredient(
        baseIngredient: Ingredient,
        echo: (String) -> Unit,
        prompt: (String, String?) -> String?
    ): Ingredient? {
        echo("\n=== Customize Ingredient ===")
        echo("Base ingredient: ${baseIngredient.name}")
        echo("Current values: ${baseIngredient.calories.toInt()} cal, ${baseIngredient.protein}g protein, ${baseIngredient.fat}g fat, ${baseIngredient.carbs}g carbs")
        echo("Enter new values (press Enter to keep current value):")
        
        val name = prompt("Ingredient name", baseIngredient.name) ?: return null
        if (name.isBlank()) {
            echo("Name cannot be empty.")
            return null
        }
        
        val servingSize = prompt("Serving size", baseIngredient.servingSize.toString())?.toDoubleOrNull()
        if (servingSize == null || servingSize <= 0) {
            echo("Invalid serving size. Must be a positive number.")
            return null
        }
        
        val servingUnit = prompt("Serving unit", baseIngredient.servingUnit) ?: baseIngredient.servingUnit
        if (servingUnit.isBlank()) {
            echo("Serving unit cannot be empty.")
            return null
        }
        
        val servingWeightStr = prompt(
            "Serving weight in grams (current: ${baseIngredient.servingWeightGrams?.toInt() ?: "none"})",
            baseIngredient.servingWeightGrams?.toString()
        )
        val servingWeightGrams = if (servingWeightStr.isNullOrBlank()) {
            baseIngredient.servingWeightGrams
        } else {
            servingWeightStr.toDoubleOrNull()?.takeIf { it > 0 }
        }
        
        val calories = prompt("Calories per serving", baseIngredient.calories.toString())?.toDoubleOrNull()
        if (calories == null || calories < 0) {
            echo("Invalid calories. Must be a non-negative number.")
            return null
        }
        
        val protein = prompt("Protein per serving (grams)", baseIngredient.protein.toString())?.toDoubleOrNull()
        if (protein == null || protein < 0) {
            echo("Invalid protein. Must be a non-negative number.")
            return null
        }
        
        val fat = prompt("Fat per serving (grams)", baseIngredient.fat.toString())?.toDoubleOrNull()
        if (fat == null || fat < 0) {
            echo("Invalid fat. Must be a non-negative number.")
            return null
        }
        
        val carbs = prompt("Carbs per serving (grams)", baseIngredient.carbs.toString())?.toDoubleOrNull()
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
        echo("\nCustomized ingredient:")
        echo("  ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
        echo("  Calories: ${ingredient.calories}, Protein: ${ingredient.protein}g, Fat: ${ingredient.fat}g, Carbs: ${ingredient.carbs}g")
        
        return ingredient
    }
}