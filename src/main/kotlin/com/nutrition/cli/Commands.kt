package com.nutrition.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.prompt
import com.nutrition.api.NutritionixClient
import com.nutrition.core.NutritionCalculator
import com.nutrition.database.DatabaseManager
import com.nutrition.models.Recipe
import com.nutrition.models.RecipeIngredient

class ShowRecipeCommand(
    private val db: DatabaseManager
) : CliktCommand(name = "show", help = "Display information for a recipe") {
    private val recipeName by argument(help = "Name of the recipe")

    override fun run() {
        val recipe = db.getRecipe(recipeName)
        if (recipe == null) {
            echo("Recipe '$recipeName' not found.")
            return
        }

        displayRecipe(recipe)
    }

    private fun displayRecipe(recipe: Recipe) {
        echo("\n=== Recipe: ${recipe.name} ===")
        echo("\nIngredients:")

        recipe.ingredients.forEach { ri ->
            val ingredient = ri.ingredient
            val servings = ri.servings
            val calories = ingredient.calories * servings
            val protein = ingredient.protein * servings
            val fat = ingredient.fat * servings
            val carbs = ingredient.carbs * servings

            echo(String.format(
                "  - %s: %.1f %s (%.1f servings) = %.1f cal, %.1fg protein, %.1fg fat, %.1fg carbs",
                ingredient.name,
                ingredient.servingSize * servings,
                ingredient.servingUnit,
                servings,
                calories,
                protein,
                fat,
                carbs
            ))
        }

        val nutrition = NutritionCalculator.calculateRecipeNutrition(recipe)

        echo("\n=== Total Nutrition ===")
        echo(String.format("Calories: %.1f", nutrition.totalCalories))
        echo(String.format("Protein: %.1fg (%.1f%% of calories)",
            nutrition.totalProtein, nutrition.proteinPercentage))
        echo(String.format("Fat: %.1fg (%.1f%% of calories)",
            nutrition.totalFat, nutrition.fatPercentage))
        echo(String.format("Carbs: %.1fg (%.1f%% of calories)",
            nutrition.totalCarbs, nutrition.carbsPercentage))
    }
}

class CreateRecipeCommand(
    private val db: DatabaseManager,
    private val nutritionix: NutritionixClient
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
                ingredient = nutritionix.searchFood(ingredientName)

                if (ingredient == null) {
                    echo("Ingredient not found in Nutritionix. Please try a different name.")
                    continue
                }

                echo("Found: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit})")
                echo("  Calories: ${ingredient.calories}, Protein: ${ingredient.protein}g, Fat: ${ingredient.fat}g, Carbs: ${ingredient.carbs}g")

                val save = prompt("Save this ingredient to database? (y/n)", default = "y")
                if (save?.lowercase() == "y") {
                    ingredient = db.saveIngredient(ingredient)
                    echo("Saved to database.")
                }
            } else {
                echo("Found in database: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit})")
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

        ShowRecipeCommand(db).displayRecipe(savedRecipe)
    }
}
