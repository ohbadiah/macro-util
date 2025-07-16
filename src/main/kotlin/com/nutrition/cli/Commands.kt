package com.nutrition.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.prompt
import com.nutrition.api.NutritionixClient
import com.nutrition.core.NutritionCalculator
import com.nutrition.database.DatabaseManager
import com.nutrition.models.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

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

class ListRecipesCommand(
    private val db: DatabaseManager,
) : CliktCommand(name = "list", help = "List all available recipes") {
    override fun run() {
        val recipes = db.getAllRecipes()
        if (recipes.isEmpty()) {
            echo("No recipes found.")
            return
        }

        echo("Available recipes:")
        recipes.forEach { recipe ->
            val ingredientCount = recipe.ingredients.size
            val totalCalories = NutritionCalculator.calculateRecipeNutrition(recipe).totalCalories
            echo("  - ${recipe.name} ($ingredientCount ingredients, ${totalCalories.toInt()} calories)")
        }
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

class RenameRecipeCommand(
    private val db: DatabaseManager,
) : CliktCommand(name = "rename", help = "Rename an existing recipe") {
    private val oldName by argument(help = "Current name of the recipe")
    private val newName by argument(help = "New name for the recipe")

    override fun run() {
        if (db.getRecipe(oldName) == null) {
            echo("Recipe '$oldName' not found.")
            return
        }

        if (db.getRecipe(newName) != null) {
            echo("Recipe '$newName' already exists.")
            return
        }

        if (db.renameRecipe(oldName, newName)) {
            echo("Recipe renamed from '$oldName' to '$newName'.")
        } else {
            echo("Failed to rename recipe.")
        }
    }
}

class DeleteRecipeCommand(
    private val db: DatabaseManager,
) : CliktCommand(name = "delete", help = "Delete an existing recipe") {
    private val recipeName by argument(help = "Name of the recipe to delete")

    override fun run() {
        if (db.getRecipe(recipeName) == null) {
            echo("Recipe '$recipeName' not found.")
            return
        }

        if (db.deleteRecipe(recipeName)) {
            echo("Recipe '$recipeName' deleted.")
        } else {
            echo("Failed to delete recipe.")
        }
    }
}

class JournalCommand(
    private val db: DatabaseManager,
    private val nutritionix: NutritionixClient,
) : CliktCommand(name = "journal", help = "Record daily food intake") {
    override fun run() {
        // Step 1: Get the date for journaling
        val date = getJournalDate()
        echo("Journaling for: $date")
        
        // Step 2: Show current day's nutrition if any entries exist
        showCurrentDayNutrition(date)
        
        // Step 3: Interactive entry loop
        addFoodEntries(date)
        
        // Step 4: Show final summary
        showFinalSummary(date)
    }
    
    private fun getJournalDate(): LocalDate {
        while (true) {
            val dateInput = prompt("Enter date (today/yesterday/YYYY-MM-DD)", default = "today")?.lowercase()
            
            try {
                return when (dateInput) {
                    "today" -> LocalDate.now()
                    "yesterday" -> LocalDate.now().minusDays(1)
                    else -> LocalDate.parse(dateInput)
                }
            } catch (e: DateTimeParseException) {
                echo("Invalid date format. Please use 'today', 'yesterday', or YYYY-MM-DD format.")
            }
        }
    }
    
    private fun showCurrentDayNutrition(date: LocalDate) {
        val journal = db.getJournalByDate(date)
        if (journal != null && journal.entries.isNotEmpty()) {
            val nutrition = NutritionCalculator.calculateDayNutrition(journal)
            echo("\n=== Current Day's Nutrition ===")
            displayDayNutrition(nutrition)
            
            echo("\nCurrent entries:")
            journal.entries.forEach { entry ->
                echo("  - ${entry.name} (${entry.servings} servings, ${entry.calories.toInt()} cal)")
            }
        } else {
            echo("\nNo entries for this date yet.")
        }
    }
    
    private fun addFoodEntries(date: LocalDate) {
        echo("\n=== Add Food Entries ===")
        
        while (true) {
            val action = prompt("\nAdd (r)ecipe, (i)ngredient, or (d)one?")?.lowercase()
            
            when (action) {
                "r", "recipe" -> addRecipeEntry(date)
                "i", "ingredient" -> addIngredientEntry(date)
                "d", "done" -> break
                else -> echo("Please enter 'r' for recipe, 'i' for ingredient, or 'd' when done.")
            }
            
            // Show running totals after each entry
            showCurrentDayNutrition(date)
        }
    }
    
    private fun addRecipeEntry(date: LocalDate) {
        val recipeName = prompt("Recipe name") ?: return
        
        var recipe = db.getRecipe(recipeName)
        if (recipe == null) {
            echo("Recipe '$recipeName' not found. Let's create it.")
            // Trigger recipe creation flow - reuse CreateRecipeCommand logic
            val createCommand = CreateRecipeCommand(db, nutritionix)
            createCommand.main(arrayOf()) // This won't work directly, need to refactor
            // For now, let's just return and let user try again
            echo("Please create the recipe first using the 'create' command, then try again.")
            return
        }
        
        val servingsStr = prompt("How many servings of ${recipe.name}? (e.g., 1, 0.5, 150g, 5oz)") ?: return
        
        // For recipes, we don't have ingredient weight data, so only accept numeric servings
        val servings = servingsStr.toDoubleOrNull()
        if (servings == null || servings <= 0) {
            echo("Invalid serving amount. For recipes, please use numeric servings (e.g., 1, 0.5, 2).")
            return
        }
        
        val journalEntry = NutritionCalculator.calculateRecipeNutritionForJournal(recipe, servings)
        db.addJournalEntry(date, journalEntry)
        echo("Added ${recipe.name} (${servings} servings) to journal.")
    }
    
    private fun addIngredientEntry(date: LocalDate) {
        val ingredientName = prompt("Ingredient name") ?: return
        
        val ingredient = db.getIngredient(ingredientName) ?: run {
            echo("Ingredient not found in database. Searching Nutritionix...")
            val searchResults = nutritionix.searchFoodOptions(ingredientName)
            
            if (searchResults.isEmpty()) {
                echo("No results found in Nutritionix. Please try a different name.")
                return
            }
            
            val selectedIngredient = if (searchResults.size == 1) {
                val result = searchResults.first()
                if (result.protein == 0.0 && result.fat == 0.0 && result.carbs == 0.0) {
                    val detailedNutrition = nutritionix.searchFood(result.name)
                    detailedNutrition?.copy(name = result.name) ?: result
                } else {
                    result
                }
            } else {
                // Multiple results, let user choose (reuse existing logic)
                var selectedIngredient: Ingredient? = null
                while (selectedIngredient == null) {
                    echo("\nFound multiple options:")
                    searchResults.forEachIndexed { index, result ->
                        val weightInfo = if (result.servingWeightGrams != null) " (~${result.servingWeightGrams.toInt()}g)" else ""
                        val calorieInfo = if (result.calories > 0) " ${result.calories.toInt()} cal" else ""
                        echo("  ${index + 1}. ${result.name} (${result.servingSize} ${result.servingUnit}$weightInfo)$calorieInfo")
                    }
                    echo("  ${searchResults.size + 1}. None of these - try different search")

                    val choice = prompt("Select option (1-${searchResults.size + 1})")?.toIntOrNull()
                    if (choice == null || choice < 1 || choice > searchResults.size + 1) {
                        echo("Invalid selection.")
                        continue
                    }

                    if (choice == searchResults.size + 1) {
                        echo("Try entering a different ingredient name.")
                        return
                    }

                    val selectedResult = searchResults[choice - 1]
                    echo("Getting detailed nutrition info for: ${selectedResult.name}")
                    
                    selectedIngredient = if (selectedResult.protein == 0.0 && selectedResult.fat == 0.0 && selectedResult.carbs == 0.0) {
                        val detailedNutrition = nutritionix.searchFood(selectedResult.name)
                        detailedNutrition?.copy(name = selectedResult.name) ?: selectedResult
                    } else {
                        selectedResult
                    }
                }
                selectedIngredient
            }
            
            // Ask if user wants to save to database
            val save = prompt("Save this ingredient to database? (y/n)", default = "y")
            if (save?.lowercase() == "y") {
                db.saveIngredient(selectedIngredient)
                echo("Saved to database.")
            }
            
            selectedIngredient
        }
        
        val weightInfo = if (ingredient.servingWeightGrams != null) " (1 serving = ${ingredient.servingWeightGrams.toInt()}g)" else ""
        val servingsStr = prompt("How many servings of ${ingredient.name}?$weightInfo (e.g., 1, 0.5, 150g, 5oz)") ?: return
        
        val servingCalc = NutritionCalculator.parseServingInput(servingsStr, ingredient)
        if (servingCalc == null) {
            echo("Invalid serving amount. Use numeric servings (e.g., 1, 0.5) or weights (e.g., 150g, 5oz).")
            return
        }
        
        val servings = servingCalc.servings
        
        val journalEntry = NutritionCalculator.calculateIngredientNutrition(ingredient, servings)
        db.addJournalEntry(date, journalEntry)
        echo("Added ${ingredient.name} (${servingCalc.displayText}) to journal.")
    }
    
    private fun showFinalSummary(date: LocalDate) {
        val journal = db.getJournalByDate(date)
        if (journal != null && journal.entries.isNotEmpty()) {
            val nutrition = NutritionCalculator.calculateDayNutrition(journal)
            echo("\n=== Final Day Summary for $date ===")
            displayDayNutrition(nutrition)
        }
    }
    
    private fun displayDayNutrition(nutrition: DayNutrition) {
        echo(String.format("Calories: %.1f", nutrition.totalCalories))
        echo(String.format("Protein: %.1fg (%.1f%% of calories)", nutrition.totalProtein, nutrition.proteinPercentage))
        echo(String.format("Fat: %.1fg (%.1f%% of calories)", nutrition.totalFat, nutrition.fatPercentage))
        echo(String.format("Carbs: %.1fg (%.1f%% of calories)", nutrition.totalCarbs, nutrition.carbsPercentage))
    }
}

class ResetJournalCommand(
    private val db: DatabaseManager,
) : CliktCommand(name = "reset-journal", help = "Reset a day's journal to empty") {
    private val dateArg by argument(name = "date", help = "Date to reset (today/yesterday/YYYY-MM-DD)")

    override fun run() {
        val date = try {
            when (dateArg.lowercase()) {
                "today" -> LocalDate.now()
                "yesterday" -> LocalDate.now().minusDays(1)
                else -> LocalDate.parse(dateArg)
            }
        } catch (e: DateTimeParseException) {
            echo("Invalid date format. Please use 'today', 'yesterday', or YYYY-MM-DD format.")
            return
        }

        // Check if journal exists for this date
        val journal = db.getJournalByDate(date)
        if (journal == null || journal.entries.isEmpty()) {
            echo("No journal entries found for $date.")
            return
        }

        // Show what will be deleted
        echo("Journal entries for $date:")
        journal.entries.forEach { entry ->
            echo("  - ${entry.name} (${entry.servings} servings, ${entry.calories.toInt()} cal)")
        }
        
        val nutrition = NutritionCalculator.calculateDayNutrition(journal)
        echo("\nTotal nutrition to be cleared:")
        echo(String.format("  Calories: %.1f", nutrition.totalCalories))
        echo(String.format("  Protein: %.1fg, Fat: %.1fg, Carbs: %.1fg", nutrition.totalProtein, nutrition.totalFat, nutrition.totalCarbs))

        // Confirm deletion
        val confirm = prompt("\nAre you sure you want to reset this journal? (yes/no)", default = "no")
        if (confirm?.lowercase() != "yes") {
            echo("Reset cancelled.")
            return
        }

        // Perform the reset
        if (db.resetJournal(date)) {
            echo("Journal for $date has been reset to empty.")
        } else {
            echo("Failed to reset journal.")
        }
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

            val ingredient = db.getIngredient(ingredientName) ?: run {
                echo("Ingredient not found in database. Searching Nutritionix...")
                val searchResults = nutritionix.searchFoodOptions(ingredientName)

                if (searchResults.isEmpty()) {
                    echo("No results found in Nutritionix. Please try a different name.")
                    return@run null
                }

                if (searchResults.size == 1) {
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
                    var selectedIngredient: Ingredient? = null
                    while (selectedIngredient == null) {
                        echo("\nFound multiple options:")
                        searchResults.forEachIndexed { index, result ->
                            val weightInfo = if (result.servingWeightGrams != null) " (~${result.servingWeightGrams.toInt()}g)" else ""
                            val calorieInfo = if (result.calories > 0) " ${result.calories.toInt()} cal" else ""
                            echo("  ${index + 1}. ${result.name} (${result.servingSize} ${result.servingUnit}$weightInfo)$calorieInfo")
                        }
                        echo("  ${searchResults.size + 1}. None of these - try different search")

                        val choice = prompt("Select option (1-${searchResults.size + 1})")?.toIntOrNull()
                        if (choice == null || choice < 1 || choice > searchResults.size + 1) {
                            echo("Invalid selection.")
                            continue
                        }

                        if (choice == searchResults.size + 1) {
                            // User wants to try a different search
                            echo("Try entering a different ingredient name.")
                            return@run null
                        }

                        val selectedResult = searchResults[choice - 1]
                        echo("Getting detailed nutrition info for: ${selectedResult.name}")
                        
                        // Get full nutrition info
                        selectedIngredient = if (selectedResult.protein == 0.0 && selectedResult.fat == 0.0 && selectedResult.carbs == 0.0) {
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
                    selectedIngredient
                }
            }

            if (ingredient == null) {
                continue // User chose "none of these" or search failed, restart ingredient prompt
            }

            // At this point, ingredient is guaranteed to be non-null
            val finalIngredient = if (ingredient.id == 0) {
                // This ingredient came from search, show details and ask to save
                val weightInfo = if (ingredient.servingWeightGrams != null) " (~${ingredient.servingWeightGrams.toInt()}g)" else ""
                echo("Selected: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
                echo(
                    "  Calories: ${ingredient.calories}, Protein: ${ingredient.protein}g, Fat: ${ingredient.fat}g, Carbs: ${ingredient.carbs}g",
                )

                val save = prompt("Save this ingredient to database? (y/n)", default = "y")
                if (save?.lowercase() == "y") {
                    val savedIngredient = db.saveIngredient(ingredient)
                    echo("Saved to database.")
                    savedIngredient
                } else {
                    ingredient
                }
            } else {
                // This ingredient was found in database
                val weightInfo = if (ingredient.servingWeightGrams != null) " (~${ingredient.servingWeightGrams.toInt()}g)" else ""
                echo("Found in database: ${ingredient.name} (${ingredient.servingSize} ${ingredient.servingUnit}$weightInfo)")
                ingredient
            }

            val weightInfo = if (finalIngredient.servingWeightGrams != null) " (1 serving = ${finalIngredient.servingWeightGrams.toInt()}g)" else ""
            val servingsStr = prompt("How many servings of ${finalIngredient.name}?$weightInfo (e.g., 1, 0.5, 150g, 5oz)") ?: continue
            
            val servingCalc = NutritionCalculator.parseServingInput(servingsStr, finalIngredient)
            if (servingCalc == null) {
                echo("Invalid serving amount. Use numeric servings (e.g., 1, 0.5) or weights (e.g., 150g, 5oz).")
                continue
            }

            val servings = servingCalc.servings
            echo("Using ${servingCalc.displayText} of ${finalIngredient.name}.")

            ingredients.add(RecipeIngredient(finalIngredient, servings))
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
