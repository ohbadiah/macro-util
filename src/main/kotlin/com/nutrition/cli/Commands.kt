package com.nutrition.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.prompt
import com.nutrition.api.NutritionixClient
import com.nutrition.core.NutritionCalculator
import com.nutrition.database.DatabaseManager
import com.nutrition.models.Recipe
import com.nutrition.models.RecipeIngredient

object RecipeDisplayUtil {
    fun displayRecipe(recipe: Recipe, echo: (String) -> Unit) {
        echo("\n=== Recipe: ${recipe.name} ===")
        echo("\nIngredients:")

        recipe.ingredients.forEach { ri ->
            val ingredient = ri.ingredient
            val servings = ri.servings
            val calories = ingredient.calories * servings
            val protein = ingredient.protein * servings
            val fat = ingredient.fat * servings
            val carbs = ingredient.carbs * servings

            val weightInfo = if (ingredient.servingWeightGrams != null) " (~${(ingredient.servingWeightGrams * servings).toInt()}g)" else ""
            echo(
                String.format(
                    "  - %s: %.1f %s (%.1f servings)%s = %.1f cal, %.1fg protein, %.1fg fat, %.1fg carbs",
                    ingredient.name,
                    ingredient.servingSize * servings,
                    ingredient.servingUnit,
                    servings,
                    weightInfo,
                    calories,
                    protein,
                    fat,
                    carbs,
                ),
            )
        }

        val nutrition = NutritionCalculator.calculateRecipeNutrition(recipe)

        echo("\n=== Total Nutrition ===")
        echo(String.format("Calories: %.1f", nutrition.totalCalories))
        echo(
            String.format(
                "Protein: %.1fg (%.1f%% of calories)",
                nutrition.totalProtein,
                nutrition.proteinPercentage,
            ),
        )
        echo(
            String.format(
                "Fat: %.1fg (%.1f%% of calories)",
                nutrition.totalFat,
                nutrition.fatPercentage,
            ),
        )
        echo(
            String.format(
                "Carbs: %.1fg (%.1f%% of calories)",
                nutrition.totalCarbs,
                nutrition.carbsPercentage,
            ),
        )
    }
}

class ShowRecipeCommand(
    private val db: DatabaseManager,
) : CliktCommand(name = "show", help = "Display information for a recipe") {
    private val recipeName by argument(help = "Name of the recipe")

    override fun run() {
        val recipe = db.getRecipe(recipeName)
        if (recipe == null) {
            echo("Recipe '$recipeName' not found.")
            return
        }

        RecipeDisplayUtil.displayRecipe(recipe, this::echo)
    }
}

class CreateRecipeCommand(
    private val db: DatabaseManager,
    private val nutritionix: NutritionixClient,
) : CliktCommand(name = "create", help = "Create a new recipe") {
    override fun run() {
        val recipeName = prompt("Recipe name")!!

        if (db.getRecipe(recipeName) != null) {
            echo("Recipe '$recipeName' already exists.")
            return
        }

        val ingredients = mutableListOf<RecipeIngredient>()

        while (true) {
            val ingredientName = prompt("\nIngredient name (or 'done' to finish)") ?: break
            if (ingredientName.lowercase() == "done") break

            var ingredient = db.getIngredient(ingredientName)

            if (ingredient == null) {
                echo("Ingredient not found in database. Searching Nutritionix...")
                val searchResults = nutritionix.searchFoodOptions(ingredientName)

                if (searchResults.isEmpty()) {
                    echo("No results found in Nutritionix. Please try a different name.")
                    continue
                }

                ingredient = if (searchResults.size == 1) {
                    // Only one result, get full nutrition info if needed
                    val result = searchResults.first()
                    if (result.protein == 0.0 && result.fat == 0.0 && result.carbs == 0.0) {
                        // This is from instant search, get full nutrition
                        val detailedNutrition = nutritionix.searchFood(result.name)
                        if (detailedNutrition != null) {
                            // Preserve the branded name from selection, use detailed nutrition data
                            detailedNutrition.copy(name = result.name)
                        } else {
                            result
                        }
                    } else {
                        result
                    }
                } else {
                    // Multiple results, let user choose
                    echo("\nFound multiple options:")
                    searchResults.forEachIndexed { index, result ->
                        val weightInfo = if (result.servingWeightGrams != null) " (~${result.servingWeightGrams.toInt()}g)" else ""
                        val calorieInfo = if (result.calories > 0) " ${result.calories.toInt()} cal" else ""
                        echo("  ${index + 1}. ${result.name} (${result.servingSize} ${result.servingUnit}$weightInfo)$calorieInfo")
                    }

                    val choice = prompt("Select option (1-${searchResults.size})")?.toIntOrNull()
                    if (choice == null || choice < 1 || choice > searchResults.size) {
                        echo("Invalid selection.")
                        continue
                    }

                    val selectedResult = searchResults[choice - 1]
                    echo("Getting detailed nutrition info for: ${selectedResult.name}")
                    
                    // Get full nutrition info
                    if (selectedResult.protein == 0.0 && selectedResult.fat == 0.0 && selectedResult.carbs == 0.0) {
                        val detailedNutrition = nutritionix.searchFood(selectedResult.name)
                        if (detailedNutrition != null) {
                            // Preserve the branded name from selection, use detailed nutrition data
                            detailedNutrition.copy(name = selectedResult.name)
                        } else {
                            selectedResult
                        }
                    } else {
                        selectedResult
                    }
                }

                val weightInfo = if (ingredient.servingWeightGrams != null) " (~${ingredient.servingWeightGrams.toInt()}g)" else ""
                echo("Selected: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
                echo(
                    "  Calories: ${ingredient.calories}, Protein: ${ingredient.protein}g, Fat: ${ingredient.fat}g, Carbs: ${ingredient.carbs}g",
                )

                val save = prompt("Save this ingredient to database? (y/n)", default = "y")
                if (save?.lowercase() == "y") {
                    ingredient = db.saveIngredient(ingredient)
                    echo("Saved to database.")
                }
            } else {
                val weightInfo = if (ingredient.servingWeightGrams != null) " (~${ingredient.servingWeightGrams.toInt()}g)" else ""
                echo("Found in database: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
            }

            val servingsStr = prompt("How many servings of ${ingredient.name}?") ?: continue
            val servings = servingsStr.toDoubleOrNull()

            if (servings == null || servings <= 0) {
                echo("Invalid serving amount.")
                continue
            }

            ingredients.add(RecipeIngredient(ingredient, servings))
        }

        if (ingredients.isEmpty()) {
            echo("No ingredients added. Recipe not created.")
            return
        }

        val recipe = Recipe(name = recipeName, ingredients = ingredients)
        val savedRecipe = db.saveRecipe(recipe)

        RecipeDisplayUtil.displayRecipe(savedRecipe, this::echo)
    }
}
