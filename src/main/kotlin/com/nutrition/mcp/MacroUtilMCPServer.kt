package com.nutrition.mcp

import com.google.gson.Gson
import com.nutrition.api.NutritionixClient
import com.nutrition.core.NutritionCalculator
import com.nutrition.database.DatabaseManager
import com.nutrition.models.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MacroUtilMCPServer(
    private val db: DatabaseManager,
    private val nutritionix: NutritionixClient,
) {
    private val gson = Gson()
    private val server: Server
    private val logger = LoggerFactory.getLogger(MacroUtilMCPServer::class.java)

    init {
        server = Server(
            Implementation(
                name = "macro-util",
                version = "1.0.0"
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        // Register all MCP tools
        registerTools()
    }

    private fun registerTools() {
        server.addTool(
            name = "list_recipes",
            description = "List all available recipes",
            inputSchema = Tool.Input()
        ) { _ ->
            handleListRecipes()
        }

        server.addTool(
            name = "show_recipe",
            description = "Display nutrition information for a recipe",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("recipe_name") {
                        put("type", "string")
                        put("description", "Name of the recipe")
                    }
                },
                required = listOf("recipe_name")
            )
        ) { request ->
            handleShowRecipe(request.arguments)
        }

        server.addTool(
            name = "create_recipe",
            description = "Create a new recipe with ingredients and serving count",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Name of the recipe")
                    }
                    putJsonObject("servings") {
                        put("type", "number")
                        put("description", "Number of servings this recipe makes")
                    }
                    putJsonObject("ingredients") {
                        put("type", "array")
                        put("description", "List of ingredients with servings")
                        putJsonObject("items") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("name") {
                                    put("type", "string")
                                    put("description", "Ingredient name")
                                }
                                putJsonObject("servings") {
                                    put("type", "number")
                                    put("description", "Number of servings")
                                }
                            }
                        }
                    }
                },
                required = listOf("name", "servings", "ingredients")
            )
        ) { request ->
            handleCreateRecipe(request.arguments)
        }

        server.addTool(
            name = "search_ingredient",
            description = "Search for ingredient options from Nutritionix API",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Search query for ingredient")
                    }
                },
                required = listOf("query")
            )
        ) { request ->
            handleSearchIngredient(request.arguments)
        }

        server.addTool(
            name = "add_ingredient_to_journal",
            description = "Add an ingredient to the food journal",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("food_name") {
                        put("type", "string")
                        put("description", "Name of the food ingredient")
                    }
                    putJsonObject("servings") {
                        put("type", "number")
                        put("description", "Number of servings")
                    }
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "Date in YYYY-MM-DD format (optional, defaults to today)")
                    }
                },
                required = listOf("food_name", "servings")
            )
        ) { request ->
            handleAddIngredientToJournal(request.arguments)
        }

        server.addTool(
            name = "add_recipe_to_journal",
            description = "Add a recipe to the food journal",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("recipe_name") {
                        put("type", "string")
                        put("description", "Name of the recipe")
                    }
                    putJsonObject("servings") {
                        put("type", "number")
                        put("description", "Number of servings")
                    }
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "Date in YYYY-MM-DD format (optional, defaults to today)")
                    }
                },
                required = listOf("recipe_name", "servings")
            )
        ) { request ->
            handleAddRecipeToJournal(request.arguments)
        }

        server.addTool(
            name = "get_journal_summary",
            description = "Get nutrition summary and entries for a date",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "Date in YYYY-MM-DD format (optional, defaults to today)")
                    }
                }
            )
        ) { request ->
            handleGetJournalSummary(request.arguments)
        }

        server.addTool(
            name = "reset_journal",
            description = "Clear all entries from a journal date",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "Date in YYYY-MM-DD format (optional, defaults to today)")
                    }
                }
            )
        ) { request ->
            handleResetJournal(request.arguments)
        }
        
        server.addTool(
            name = "create_custom_ingredient",
            description = "Create a custom ingredient with manual nutrition data",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Name of the ingredient")
                    }
                    putJsonObject("serving_size") {
                        put("type", "number")
                        put("description", "Serving size (e.g., 1, 0.5)")
                    }
                    putJsonObject("serving_unit") {
                        put("type", "string")
                        put("description", "Serving unit (e.g., cup, gram, piece)")
                    }
                    putJsonObject("serving_weight_grams") {
                        put("type", "number")
                        put("description", "Weight of one serving in grams (optional)")
                    }
                    putJsonObject("calories") {
                        put("type", "number")
                        put("description", "Calories per serving")
                    }
                    putJsonObject("protein") {
                        put("type", "number")
                        put("description", "Protein per serving in grams")
                    }
                    putJsonObject("fat") {
                        put("type", "number")
                        put("description", "Fat per serving in grams")
                    }
                    putJsonObject("carbs") {
                        put("type", "number")
                        put("description", "Carbs per serving in grams")
                    }
                },
                required = listOf("name", "serving_size", "serving_unit", "calories", "protein", "fat", "carbs")
            )
        ) { request ->
            handleCreateCustomIngredient(request.arguments)
        }
    }

    private fun handleListRecipes(): CallToolResult {
        return try {
            val recipes = db.getAllRecipes()
            val recipeList = recipes.map { recipe ->
                val nutrition = NutritionCalculator.calculateRecipeNutrition(recipe)
                mapOf(
                    "name" to recipe.name,
                    "servings" to recipe.servings,
                    "ingredients" to recipe.ingredients.size,
                    "calories" to nutrition.totalCalories.toInt()
                )
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(gson.toJson(recipeList))
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error listing recipes: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleShowRecipe(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val recipeName = arguments["recipe_name"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing recipe_name parameter")
                    ),
                    isError = true
                )

            val recipe = db.getRecipe(recipeName)
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Recipe '$recipeName' not found")
                    ),
                    isError = true
                )

            val nutrition = NutritionCalculator.calculateRecipeNutrition(recipe)
            val result = mapOf(
                "name" to recipe.name,
                "servings" to recipe.servings,
                "ingredients" to recipe.ingredients.map { ri ->
                    mapOf(
                        "name" to ri.ingredient.name,
                        "servings" to ri.servings,
                        "calories" to (ri.ingredient.calories * ri.servings).toInt()
                    )
                },
                "nutrition" to mapOf(
                    "calories" to nutrition.totalCalories.toInt(),
                    "protein" to String.format("%.1f", nutrition.totalProtein),
                    "fat" to String.format("%.1f", nutrition.totalFat),
                    "carbs" to String.format("%.1f", nutrition.totalCarbs),
                    "protein_percentage" to String.format("%.1f", nutrition.proteinPercentage),
                    "fat_percentage" to String.format("%.1f", nutrition.fatPercentage),
                    "carbs_percentage" to String.format("%.1f", nutrition.carbsPercentage)
                )
            )

            CallToolResult(
                content = listOf(
                    TextContent(gson.toJson(result))
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error showing recipe: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleSearchIngredient(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val query = arguments["query"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing query parameter")
                    ),
                    isError = true
                )

            val results = nutritionix.searchFoodOptions(query)
            val searchResults = results.map { ingredient ->
                mapOf(
                    "name" to ingredient.name,
                    "serving_size" to ingredient.servingSize,
                    "serving_unit" to ingredient.servingUnit,
                    "calories" to ingredient.calories.toInt(),
                    "serving_weight_grams" to ingredient.servingWeightGrams
                )
            }

            CallToolResult(
                content = listOf(
                    TextContent(gson.toJson(searchResults))
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error searching ingredients: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleAddIngredientToJournal(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val foodName = arguments["food_name"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing food_name parameter")
                    ),
                    isError = true
                )

            val servings = arguments["servings"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid servings parameter")
                    ),
                    isError = true
                )

            val date = parseDate(arguments["date"]?.jsonPrimitive?.content)

            // Try to find existing ingredient first
            var ingredient = db.getIngredient(foodName)
            if (ingredient == null) {
                // Get detailed nutrition from API
                ingredient = nutritionix.searchFood(foodName)
                    ?: return CallToolResult(
                        content = listOf(
                            TextContent("Could not find nutrition information for '$foodName'")
                        ),
                        isError = true
                    )
            }

            val journalEntry = JournalEntry(
                type = EntryType.INGREDIENT,
                name = ingredient.name,
                servings = servings,
                calories = ingredient.calories * servings,
                protein = ingredient.protein * servings,
                fat = ingredient.fat * servings,
                carbs = ingredient.carbs * servings
            )

            db.addJournalEntry(date, journalEntry)

            CallToolResult(
                content = listOf(
                    TextContent("Added ${ingredient.name} (${servings} servings, ${(ingredient.calories * servings).toInt()} calories) to journal for $date")
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error adding ingredient to journal: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleAddRecipeToJournal(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val recipeName = arguments["recipe_name"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing recipe_name parameter")
                    ),
                    isError = true
                )

            val servings = arguments["servings"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid servings parameter")
                    ),
                    isError = true
                )

            val date = parseDate(arguments["date"]?.jsonPrimitive?.content)

            val recipe = db.getRecipe(recipeName)
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Recipe '$recipeName' not found")
                    ),
                    isError = true
                )

            val journalEntry = NutritionCalculator.calculateRecipeNutritionForJournal(recipe, servings)
            db.addJournalEntry(date, journalEntry)

            CallToolResult(
                content = listOf(
                    TextContent("Added ${recipe.name} (${servings} servings, ${journalEntry.calories.toInt()} calories) to journal for $date")
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error adding recipe to journal: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleGetJournalSummary(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val date = parseDate(arguments["date"]?.jsonPrimitive?.content)
            val journal = db.getJournalByDate(date)

            if (journal == null || journal.entries.isEmpty()) {
                return CallToolResult(
                    content = listOf(
                        TextContent(gson.toJson(mapOf(
                            "date" to date.toString(),
                            "entries" to emptyList<Any>(),
                            "nutrition" to mapOf(
                                "calories" to 0,
                                "protein" to "0.0",
                                "fat" to "0.0",
                                "carbs" to "0.0"
                            )
                        )))
                    )
                )
            }

            val nutrition = NutritionCalculator.calculateDayNutrition(journal)
            val result = mapOf(
                "date" to date.toString(),
                "entries" to journal.entries.map { entry ->
                    mapOf(
                        "name" to entry.name,
                        "servings" to entry.servings,
                        "calories" to entry.calories.toInt()
                    )
                },
                "nutrition" to mapOf(
                    "calories" to nutrition.totalCalories.toInt(),
                    "protein" to String.format("%.1f", nutrition.totalProtein),
                    "fat" to String.format("%.1f", nutrition.totalFat),
                    "carbs" to String.format("%.1f", nutrition.totalCarbs),
                    "protein_percentage" to String.format("%.1f", nutrition.proteinPercentage),
                    "fat_percentage" to String.format("%.1f", nutrition.fatPercentage),
                    "carbs_percentage" to String.format("%.1f", nutrition.carbsPercentage)
                )
            )

            CallToolResult(
                content = listOf(
                    TextContent(gson.toJson(result))
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error getting journal summary: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleResetJournal(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val date = parseDate(arguments["date"]?.jsonPrimitive?.content)
            val success = db.resetJournal(date)

            if (success) {
                CallToolResult(
                    content = listOf(
                        TextContent("Journal for $date has been reset to empty")
                    )
                )
            } else {
                CallToolResult(
                    content = listOf(
                        TextContent("Failed to reset journal for $date")
                    ),
                    isError = true
                )
            }
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error resetting journal: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun handleCreateRecipe(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val recipeName = arguments["name"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing name parameter")
                    ),
                    isError = true
                )
                
            val servings = arguments["servings"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid servings parameter")
                    ),
                    isError = true
                )

            val ingredientsList = arguments["ingredients"]?.let { element ->
                if (element is kotlinx.serialization.json.JsonArray) {
                    element.map { it as kotlinx.serialization.json.JsonObject }
                } else null
            } ?: return CallToolResult(
                content = listOf(
                    TextContent("Missing or invalid ingredients parameter")
                ),
                isError = true
            )

            // Check if recipe already exists
            if (db.getRecipe(recipeName) != null) {
                return CallToolResult(
                    content = listOf(
                        TextContent("Recipe '$recipeName' already exists")
                    ),
                    isError = true
                )
            }

            // Create recipe ingredients list
            val recipeIngredients = mutableListOf<RecipeIngredient>()

            for (item in ingredientsList) {
                val ingredientName = item["name"]?.jsonPrimitive?.content ?: continue
                val servings = item["servings"]?.jsonPrimitive?.doubleOrNull ?: continue

                // Find or create ingredient
                var ingredient = db.getIngredient(ingredientName)
                if (ingredient == null) {
                    ingredient = nutritionix.searchFood(ingredientName) ?: continue
                    ingredient = db.saveIngredient(ingredient)
                }

                recipeIngredients.add(RecipeIngredient(ingredient, servings))
            }

            // Save the complete recipe
            val recipe = Recipe(name = recipeName, servings = servings, ingredients = recipeIngredients)
            db.saveRecipe(recipe)

            CallToolResult(
                content = listOf(
                    TextContent("Created recipe '$recipeName' with ${recipeIngredients.size} ingredients (${servings} servings)")
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error creating recipe: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun parseDate(dateString: String?): LocalDate {
        return when {
            dateString == null -> LocalDate.now()
            dateString.lowercase() == "today" -> LocalDate.now()
            dateString.lowercase() == "yesterday" -> LocalDate.now().minusDays(1)
            else -> try {
                LocalDate.parse(dateString)
            } catch (e: DateTimeParseException) {
                LocalDate.now()
            }
        }
    }

    fun start() {
        val pid = ProcessHandle.current().pid()
        logger.info("MCP server starting with PID: $pid")
        
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        
        runBlocking {
            server.connect(transport)
            val done = Job()
            server.onClose {
                done.complete()
            }
            done.join()
        }
    }
    
    private fun handleCreateCustomIngredient(arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult {
        return try {
            val name = arguments["name"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing name parameter")
                    ),
                    isError = true
                )
            
            val servingSize = arguments["serving_size"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid serving_size parameter")
                    ),
                    isError = true
                )
            
            val servingUnit = arguments["serving_unit"]?.jsonPrimitive?.content
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing serving_unit parameter")
                    ),
                    isError = true
                )
            
            val servingWeightGrams = arguments["serving_weight_grams"]?.jsonPrimitive?.doubleOrNull
            
            val calories = arguments["calories"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid calories parameter")
                    ),
                    isError = true
                )
            
            val protein = arguments["protein"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid protein parameter")
                    ),
                    isError = true
                )
            
            val fat = arguments["fat"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid fat parameter")
                    ),
                    isError = true
                )
            
            val carbs = arguments["carbs"]?.jsonPrimitive?.doubleOrNull
                ?: return CallToolResult(
                    content = listOf(
                        TextContent("Missing or invalid carbs parameter")
                    ),
                    isError = true
                )
            
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
            
            val savedIngredient = db.saveIngredient(ingredient)
            
            CallToolResult(
                content = listOf(
                    TextContent("Created custom ingredient '${savedIngredient.name}' with ${savedIngredient.calories} calories per ${savedIngredient.servingSize} ${savedIngredient.servingUnit}")
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent("Error creating custom ingredient: ${e.message}")
                ),
                isError = true
            )
        }
    }
}